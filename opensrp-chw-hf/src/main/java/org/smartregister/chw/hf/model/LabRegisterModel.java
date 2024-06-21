package org.smartregister.chw.hf.model;

import static org.smartregister.chw.hf.utils.Constants.JsonFormConstants.NAME_OF_HF;
import static org.smartregister.chw.lab.util.Constants.EVENT_TYPE.LAB_SET_MANIFEST_SETTINGS;
import static org.smartregister.chw.lab.util.LabUtil.getHfrCode;
import static org.smartregister.client.utils.constants.JsonFormConstants.ENCOUNTER_TYPE;
import static org.smartregister.client.utils.constants.JsonFormConstants.GLOBAL;
import static org.smartregister.client.utils.constants.JsonFormConstants.MIN_DATE;
import static org.smartregister.client.utils.constants.JsonFormConstants.READ_ONLY;
import static org.smartregister.client.utils.constants.JsonFormConstants.VALUE;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.chw.hf.dao.HeiDao;
import org.smartregister.chw.hf.dao.HfAncDao;
import org.smartregister.chw.hf.dao.HfPmtctDao;
import org.smartregister.chw.hf.repository.HfLocationRepository;
import org.smartregister.chw.hf.repository.UniqueLabTestSampleTrackingIdRepository;
import org.smartregister.chw.hiv.dao.HivDao;
import org.smartregister.chw.hiv.domain.HivMemberObject;
import org.smartregister.chw.lab.dao.LabDao;
import org.smartregister.chw.lab.model.BaseLabRegisterModel;
import org.smartregister.chw.lab.util.LabJsonFormUtils;
import org.smartregister.chw.lab.util.LabUtil;
import org.smartregister.chw.pmtct.dao.PmtctDao;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;
import org.smartregister.domain.UniqueId;
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
                refreshHvlRequesterDetails(form, entityId);
            } else return null;
        } else {
            patientId = HeiDao.getHeiNumber(entityId);
            refreshHeidRequesterDetails(form, entityId);
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

            JSONObject sampleRequestSampleId = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_id");
            UniqueId uniqueId = new UniqueLabTestSampleTrackingIdRepository().getNextUniqueId();

            if (uniqueId != null) {
                sampleRequestSampleId.put(VALUE, "9" + LabUtil.getHfrCode() + uniqueId.getOpenmrsId());
                sampleRequestSampleId.put(READ_ONLY, true);
            }

            //TODO handle cases whereby there are no unique sample ids available


            JSONObject sampleRequestDate = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_request_date");
            JSONObject sampleRequestTime = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_request_time");
            JSONObject sampleCollectionDate = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_collection_date");
            if (HfPmtctDao.getSampleRequestDate(entityId) != null) {
                sampleRequestDate.put(VALUE, HfPmtctDao.getSampleRequestDate(entityId));
                sampleRequestTime.put(VALUE, HfPmtctDao.getSampleRequestTime(entityId));

                sampleRequestDate.put(READ_ONLY, true);
                sampleRequestTime.put(READ_ONLY, true);

                sampleCollectionDate.put(MIN_DATE, HfPmtctDao.getSampleRequestDate(entityId));
            }

            JSONObject requesterClinicianName = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "requester_clinician_name");
            JSONObject requesterPhoneNumber = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "requester_phone_number");
            JSONObject reasonForRequestingTest = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "reason_for_requesting_test");
            if (HfPmtctDao.getRequesterClinicianName(entityId) != null) {
                requesterClinicianName.put(VALUE, HfPmtctDao.getRequesterClinicianName(entityId));
                requesterPhoneNumber.put(VALUE, HfPmtctDao.getRequesterPhoneNumber(entityId));
                reasonForRequestingTest.put(VALUE, HfPmtctDao.getReasonsForRequestingTest(entityId));

                requesterClinicianName.put(READ_ONLY, true);
                requesterPhoneNumber.put(READ_ONLY, true);
                reasonForRequestingTest.put(READ_ONLY, true);
            }

            JSONObject hasTb = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "has_tb");
            JSONObject isOnTbTreatment = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "is_on_tb_treatment");
            if (HfPmtctDao.getOnTbTreatment(entityId) != null) {
                hasTb.put(VALUE, HfPmtctDao.getOnTbTreatment(entityId).equalsIgnoreCase("yes") ? "Yes" : "No");
                isOnTbTreatment.put(VALUE, HfPmtctDao.getOnTbTreatment(entityId).equalsIgnoreCase("yes") ? "Yes" : "No");
            } else {
                hasTb.put(VALUE, "Unknown");
                isOnTbTreatment.put(VALUE, "Unknown");
            }

            JSONObject isPregnant = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "is_pregnant");
            JSONObject isBreastFeeding = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "is_breast_feeding");
            isPregnant.put(VALUE, HfAncDao.isANCMember(entityId));
            isBreastFeeding.put(VALUE, !HfAncDao.isANCMember(entityId));


            JSONObject ageGroup = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "client_age_group");
            ageGroup.put(VALUE, PmtctDao.getMember(entityId).getAge() > 15 ? "Adult" : "Children");


            JSONObject drugLine = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "drug_line");

            String arvLine = HfPmtctDao.getArvLine(entityId);
            if (arvLine != null && drugLine != null) {
                switch (arvLine) {
                    case "first_line":
                        drugLine.put(VALUE, "First line");
                        break;
                    case "second_line":
                        drugLine.put(VALUE, "Second line");
                        break;
                    case "third_line":
                        drugLine.put(VALUE, "Third line");
                        break;
                    default:
                        drugLine.put(VALUE, "NA");
                        break;
                }
            }

            JSONObject artDrug = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "art_drug");
            if (HfPmtctDao.getArvDrug(entityId) != null) {
                artDrug.put(VALUE, HfPmtctDao.getArvDrug(entityId));
            } else {
                artDrug.put(VALUE, "NA");
            }

        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void refreshHeidRequesterDetails(JSONObject form, String entityId) {
        try {
            JSONArray fields = form.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1)
                    .getJSONArray(JsonFormConstants.FIELDS);

            JSONObject sampleRequestSampleId = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_id");
            UniqueId uniqueId = new UniqueLabTestSampleTrackingIdRepository().getNextUniqueId();

            if (uniqueId != null) {
                sampleRequestSampleId.put(VALUE, "9" + LabUtil.getHfrCode() + uniqueId.getOpenmrsId());
                sampleRequestSampleId.put(READ_ONLY, true);
            }

            JSONObject sampleRequestDate = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_request_date");
            JSONObject sampleRequestTime = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_request_time");
            JSONObject sampleCollectionDate = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_collection_date");
            if (HeiDao.getSampleRequestDate(entityId) != null) {
                sampleRequestDate.put(VALUE, HeiDao.getSampleRequestDate(entityId));
                sampleRequestTime.put(VALUE, HeiDao.getSampleRequestTime(entityId));

                sampleRequestDate.put(READ_ONLY, true);
                sampleRequestTime.put(READ_ONLY, true);
                sampleCollectionDate.put(MIN_DATE, HeiDao.getSampleRequestDate(entityId));
            }

            JSONObject requesterClinicianName = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "requester_clinician_name");
            JSONObject requesterPhoneNumber = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "requester_phone_number");
            JSONObject reasonForRequestingTest = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "reason_for_requesting_test");

            if (HeiDao.getRequesterClinicianName(entityId) != null) {
                requesterClinicianName.put(VALUE, HeiDao.getRequesterClinicianName(entityId));
                requesterPhoneNumber.put(VALUE, HeiDao.getRequesterPhoneNumber(entityId));
                reasonForRequestingTest.put(VALUE, HeiDao.getReasonsForRequestingTest(entityId));

                requesterClinicianName.put(READ_ONLY, true);
                requesterPhoneNumber.put(READ_ONLY, true);
                reasonForRequestingTest.put(READ_ONLY, true);
            }

            JSONObject motherBreastFeeding = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "mother_breast_feeding");
            if (HeiDao.getInfantFeedingPractice(entityId) != null) {
                motherBreastFeeding.put(VALUE, HeiDao.getInfantFeedingPractice(entityId).toUpperCase());
            } else {
                motherBreastFeeding.put(READ_ONLY, "NA");
            }

            JSONObject drugGivenToBaby = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "drug_given_to_baby");
            if (HeiDao.getPrescribedCtx(entityId) != null) {
                drugGivenToBaby.put(VALUE, HeiDao.getPrescribedCtx(entityId).equalsIgnoreCase("yes") ? "CTX" : "NA");
            } else {
                drugGivenToBaby.put(READ_ONLY, "NA");
            }

            JSONObject drugPeriodInDays = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "baby_drug_period_in_days");
            if (HeiDao.getPrescribedCtx(entityId) != null) {
                drugPeriodInDays.put(VALUE, HeiDao.getNumberOfCtxDaysDispensed(entityId));
            } else {
                drugPeriodInDays.put(READ_ONLY, "NA");
            }

            JSONObject motherDrugDuringPregnancy = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "mother_drug_during_pregnancy");
            motherDrugDuringPregnancy.put(READ_ONLY, "NA");

            JSONObject motherDrugDuringLabor = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "mother_drug_during_labor");
            motherDrugDuringLabor.put(READ_ONLY, "NA");

        } catch (Exception e) {
            Timber.e(e);
        }
    }

}
