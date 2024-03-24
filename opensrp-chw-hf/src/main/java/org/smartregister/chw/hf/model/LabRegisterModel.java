package org.smartregister.chw.hf.model;

import static org.smartregister.chw.hf.utils.Constants.JsonFormConstants.NAME_OF_HF;
import static org.smartregister.chw.lab.util.Constants.EVENT_TYPE.LAB_SET_MANIFEST_SETTINGS;
import static org.smartregister.chw.lab.util.LabUtil.getHfrCode;
import static org.smartregister.client.utils.constants.JsonFormConstants.ENCOUNTER_TYPE;
import static org.smartregister.client.utils.constants.JsonFormConstants.GLOBAL;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.HeiDao;
import org.smartregister.chw.hf.repository.HfLocationRepository;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hiv.domain.HivMemberObject;
import org.smartregister.chw.lab.model.BaseLabRegisterModel;
import org.smartregister.chw.lab.util.LabJsonFormUtils;
import org.smartregister.chw.pmtct.dao.PmtctDao;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;

import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class LabRegisterModel extends BaseLabRegisterModel {
    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) throws Exception {
        JSONObject form = LabJsonFormUtils.getFormAsJson(formName);
        LabJsonFormUtils.getRegistrationForm(form, entityId, currentLocationId);

        String patientId;
        if (PmtctDao.isRegisteredForPmtct(entityId)) {
            HivMemberObject hivMemberObject = HivDao.getMember(entityId);
            if (hivMemberObject != null && hivMemberObject.getCtcNumber() != null)
                patientId = hivMemberObject.getCtcNumber();
            else return null;
        } else {
            patientId = HeiDao.getHeiNumber(entityId);
        }

        form.getJSONObject(GLOBAL).put("PatientId", patientId);
        form.getJSONObject(GLOBAL).put("HfrCode", getHfrCode());

        if (form.getString(ENCOUNTER_TYPE).equals(LAB_SET_MANIFEST_SETTINGS)) {
            initializeHealthFacilitiesList(form);
        }

        return form;
    }


    public JSONObject initializeHealthFacilitiesList(JSONObject form) {
        HfLocationRepository locationRepository = new HfLocationRepository();
        List<Location> locations = locationRepository.getAllLocationsWithTags();
        if (locations != null && form != null) {

            try {

                JSONArray fields = form.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1)
                        .getJSONArray(JsonFormConstants.FIELDS);

                JSONObject referralHealthFacilities = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, NAME_OF_HF);

                JSONArray options = referralHealthFacilities.getJSONArray("options");
                String healthFacilityTagName = "Facility";
                for (Location location : locations) {
                    Set<LocationTag> locationTags = location.getLocationTags();
                    if (locationTags.iterator().next().getName().equalsIgnoreCase(healthFacilityTagName)) {
                        JSONObject optionNode = new JSONObject();
                        optionNode.put("text", StringUtils.capitalize(location.getProperties().getName()));
                        optionNode.put("key", StringUtils.capitalize(location.getProperties().getName()));
                        JSONObject propertyObject = new JSONObject();
                        propertyObject.put("presumed-id", location.getProperties().getUid());
                        propertyObject.put("confirmed-id", location.getProperties().getUid());
                        optionNode.put("property", propertyObject);

                        options.put(optionNode);
                    }
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
        return form;
    }

}
