package org.smartregister.chw.hf.model;

import static org.smartregister.chw.hf.utils.Constants.JsonFormConstants.NAME_OF_HF;
import static org.smartregister.chw.lab.util.Constants.EVENT_TYPE.LAB_SET_MANIFEST_SETTINGS;
import static org.smartregister.chw.lab.util.LabUtil.getHfrCode;
import static org.smartregister.client.utils.constants.JsonFormConstants.ENCOUNTER_TYPE;
import static org.smartregister.client.utils.constants.JsonFormConstants.GLOBAL;
import static org.smartregister.client.utils.constants.JsonFormConstants.READ_ONLY;
import static org.smartregister.client.utils.constants.JsonFormConstants.VALUE;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.chw.hf.dao.HeiDao;
import org.smartregister.chw.hf.dao.HfPmtctDao;
import org.smartregister.chw.hf.repository.HfLocationRepository;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hiv.domain.HivMemberObject;
import org.smartregister.chw.lab.dao.LabDao;
import org.smartregister.chw.lab.model.BaseLabRegisterModel;
import org.smartregister.chw.lab.util.LabJsonFormUtils;
import org.smartregister.chw.pmtct.dao.PmtctDao;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;
import org.smartregister.repository.LocationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class LabRegisterModel extends BaseLabRegisterModel {
    private static List<Location> setChildOptions(List<Location> childLocations, List<Location> locations, String parentLocationId, String tagName) {
        for (Location location : locations) {
            Set<LocationTag> locationTags = location.getLocationTags();
            if (location.getProperties().getParentId().equals(parentLocationId)) {

                if (locationTags.iterator().next().getName().equalsIgnoreCase(tagName)) {
                    childLocations.add(location);
                } else {
                    setChildOptions(childLocations, locations, location.getId(), tagName);
                }
            }
        }
        if (!childLocations.isEmpty()) {
            return childLocations;
        } else return new ArrayList<>();

    }

    @Override
    public JSONObject getFormAsJson(String formName, String entityId, String currentLocationId) throws Exception {
        JSONObject form = LabJsonFormUtils.getFormAsJson(formName);
        LabJsonFormUtils.getRegistrationForm(form, entityId, currentLocationId);

        String patientId;
        if (PmtctDao.isRegisteredForPmtct(entityId)) {
            HivMemberObject hivMemberObject = HivDao.getMember(entityId);
            if (hivMemberObject != null && hivMemberObject.getCtcNumber() != null) {
                patientId = hivMemberObject.getCtcNumber();
                if (HfPmtctDao.hasPendingLabSampleCollection(entityId)) {
                    refreshHvlRequesterDetails(form, entityId);
                }
            } else return null;
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

                for (Location location : locations) {
                    Set<LocationTag> locationTags = location.getLocationTags();
                    if (locationTags.iterator().next().getName().equalsIgnoreCase("Testing Lab")) {
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
                String healthFacilityTagName = "Facility";
                String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);

                Location currentLocation = new LocationRepository().getLocationByUUId(locationId);
                String parentLocationId = getParentLocationId(locations, currentLocation.getProperties().getParentId(), "Region");

                List<Location> childLocations = setChildOptions(new ArrayList<>(), locations, parentLocationId, healthFacilityTagName);

                for (Location location : childLocations) {
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

                if (!LabDao.getDestinationHubUuid().isEmpty()) {
                    Location destinationHuub = new LocationRepository().getLocationByUUId(LabDao.getDestinationHubUuid());

                    JSONObject optionNode = new JSONObject();
                    optionNode.put("text", StringUtils.capitalize(destinationHuub.getProperties().getName()));
                    optionNode.put("key", StringUtils.capitalize(destinationHuub.getProperties().getName()));
                    JSONObject propertyObject = new JSONObject();
                    propertyObject.put("presumed-id", destinationHuub.getProperties().getUid());
                    propertyObject.put("confirmed-id", destinationHuub.getProperties().getUid());
                    optionNode.put("property", propertyObject);

                    JSONArray values = new JSONArray();
                    values.put(optionNode);

                    referralHealthFacilities.put(VALUE, values.toString());
                }

            } catch (JSONException e) {
                Timber.e(e);
            }
        }
        return form;
    }

    public String getParentLocationId(List<Location> locations, String parentLocationId, String parentTag) {
        for (Location location : locations) {
            if (location.getId().equalsIgnoreCase(parentLocationId)) {
                Set<LocationTag> locationTags = location.getLocationTags();
                if (locationTags.iterator().next().getName().equalsIgnoreCase(parentTag)) {
                    return location.getId();
                } else {
                    return getParentLocationId(locations, location.getProperties().getParentId(), parentTag);
                }
            }
        }
        return null;
    }

    private void refreshHvlRequesterDetails(JSONObject form, String entityId) {
        try {
            JSONArray fields = form.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1)
                    .getJSONArray(JsonFormConstants.FIELDS);

            JSONObject sampleRequestDate = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_request_date");
            if (sampleRequestDate != null) {
                sampleRequestDate.put(VALUE, HfPmtctDao.getSampleRequestDate(entityId));
                sampleRequestDate.put(READ_ONLY, true);
            }

            JSONObject requesterClinicianName = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "requester_clinician_name");
            if (requesterClinicianName != null) {
                requesterClinicianName.put(VALUE, HfPmtctDao.getRequesterClinicianName(entityId));
                requesterClinicianName.put(READ_ONLY, true);
            }
        } catch (Exception e) {
            Timber.e(e);
        }
    }

}
