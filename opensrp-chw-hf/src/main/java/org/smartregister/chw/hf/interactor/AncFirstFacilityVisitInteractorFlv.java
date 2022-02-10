package org.smartregister.chw.hf.interactor;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.AncLibrary;
import org.smartregister.chw.anc.contract.BaseAncHomeVisitContract;
import org.smartregister.chw.anc.domain.MemberObject;
import org.smartregister.chw.anc.domain.Visit;
import org.smartregister.chw.anc.domain.VisitDetail;
import org.smartregister.chw.anc.model.BaseAncHomeVisitAction;
import org.smartregister.chw.anc.util.JsonFormUtils;
import org.smartregister.chw.anc.util.VisitUtils;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.BuildConfig;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.AncBirthReviewAction;
import org.smartregister.chw.hf.actionhelper.AncCounsellingAction;
import org.smartregister.chw.hf.actionhelper.AncPartnerRegistrationAction;
import org.smartregister.chw.hf.actionhelper.AncPartnerTestingAction;
import org.smartregister.chw.hf.actionhelper.AncTtVaccinationAction;
import org.smartregister.chw.hf.dao.HfAncDao;
import org.smartregister.chw.hf.repository.HfLocationRepository;
import org.smartregister.chw.hf.utils.Constants;
import org.smartregister.chw.hf.utils.ContactUtil;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.domain.Location;
import org.smartregister.domain.LocationTag;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class AncFirstFacilityVisitInteractorFlv implements AncFirstFacilityVisitInteractor.Flavor {

    @Override
    public LinkedHashMap<String, BaseAncHomeVisitAction> calculateActions(BaseAncHomeVisitContract.View view, MemberObject memberObject, BaseAncHomeVisitContract.InteractorCallBack callBack) throws BaseAncHomeVisitAction.ValidationException {
        LinkedHashMap<String, BaseAncHomeVisitAction> actionList = new LinkedHashMap<>();

        Context  context = view.getContext();

        Map<String, List<VisitDetail>> details = null;
        // get the preloaded data
        if (view.getEditMode()) {
            Visit lastVisit = AncLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.Events.ANC_FIRST_FACILITY_VISIT);
            if (lastVisit != null) {
                details = VisitUtils.getVisitGroups(AncLibrary.getInstance().visitDetailsRepository().getVisits(lastVisit.getVisitId()));
            }
        }

        // get contact
        LocalDate lastContact = new DateTime(memberObject.getDateCreated()).toLocalDate();
        boolean isFirst = (StringUtils.isBlank(memberObject.getLastContactVisit()));
        LocalDate lastMenstrualPeriod = new LocalDate();
        try {
            lastMenstrualPeriod = DateTimeFormat.forPattern("dd-MM-yyyy").parseLocalDate(memberObject.getLastMenstrualPeriod());
        } catch (Exception e) {
            Timber.e(e);
        }


        if (StringUtils.isNotBlank(memberObject.getLastContactVisit())) {
            lastContact = DateTimeFormat.forPattern("dd-MM-yyyy").parseLocalDate(memberObject.getLastContactVisit());
        }

        Map<Integer, LocalDate> dateMap = new LinkedHashMap<>();

        // today is the due date for the very first visit
        if (isFirst) {
            dateMap.put(0, LocalDate.now());
        }

        dateMap.putAll(ContactUtil.getContactWeeks(isFirst, lastContact, lastMenstrualPeriod));

        evaluateMedicalAndSurgicalHistory(actionList, details, memberObject, context);

        return actionList;
    }

    private void evaluateMedicalAndSurgicalHistory(LinkedHashMap<String, BaseAncHomeVisitAction> actionList,
                                                   Map<String, List<VisitDetail>> details,
                                                   final MemberObject memberObject,
                                                   final Context context) throws BaseAncHomeVisitAction.ValidationException {
        JSONObject obstetricForm = null;
        try {
            obstetricForm = setMinFundalHeight(FormUtils.getFormUtils().getFormJson(Constants.JsonForm.AncFirstVisit.OBSTETRIC_EXAMINATION), memberObject.getBaseEntityId());
            obstetricForm.getJSONObject("global").put("last_menstrual_period", memberObject.getLastMenstrualPeriod());
            if (details != null && !details.isEmpty()) {
                JsonFormUtils.populateForm(obstetricForm, details);
            }
        } catch (JSONException e) {
            Timber.e(e);
        }

        JSONObject medicalSurgicalHistoryForm = null;
        try {
            medicalSurgicalHistoryForm = firstPregnancyAboveThirtyFive(FormUtils.getFormUtils().getFormJson(Constants.JsonForm.AncFirstVisit.getMedicalAndSurgicalHistory()), memberObject, context);
            if (details != null && !details.isEmpty()) {
                JsonFormUtils.populateForm(medicalSurgicalHistoryForm, details);
            }
        } catch (JSONException e) {
            Timber.e(e);
        }

        JSONObject baselineInvestigationForm = null;
        try{
            baselineInvestigationForm = FormUtils.getFormUtils().getFormJson(Constants.JsonForm.AncFirstVisit.getBaselineInvestigation());
            baselineInvestigationForm.getJSONObject("global").put("gestational_age", memberObject.getGestationAge());
            if(details != null && !details.isEmpty()){
                JsonFormUtils.populateForm(baselineInvestigationForm, details);
            }
        }catch (JSONException e){
            Timber.e(e);
        }

        JSONObject partnerTestingForm = null;
        try{
            partnerTestingForm = FormUtils.getFormUtils().getFormJson(Constants.JsonForm.AncRecurringVisit.PARTNER_TESTING);
            partnerTestingForm.getJSONObject("global").put("gestational_age", memberObject.getGestationAge());
            if(details != null && !details.isEmpty()){
                JsonFormUtils.populateForm(partnerTestingForm, details);
            }
        }catch (JSONException e){
            Timber.e(e);
        }


        BaseAncHomeVisitAction medicalAndSurgicalHistory = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_first_visit_medical_and_surgical_history))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(medicalSurgicalHistoryForm.toString())
                .withFormName(Constants.JsonForm.AncFirstVisit.getMedicalAndSurgicalHistory())
                .withHelper(new AncMedicalAndSurgicalHistoryAction(memberObject))
                .build();
        actionList.put(context.getString(R.string.anc_first_visit_medical_and_surgical_history), medicalAndSurgicalHistory);

        BaseAncHomeVisitAction obstetricExaminationAction = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_first_visit_obstetric_examination))
                .withOptional(true)
                .withDetails(details)
                .withJsonPayload(obstetricForm.toString())
                .withFormName(Constants.JsonForm.AncFirstVisit.getObstetricExamination())
                .withHelper(new AncObstetricExaminationAction(memberObject))
                .build();
        actionList.put(context.getString(R.string.anc_first_visit_obstetric_examination), obstetricExaminationAction);

        BaseAncHomeVisitAction baselineInvestigationAction = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_first_visit_baseline_investigation))
                .withOptional(true)
                .withDetails(details)
                .withJsonPayload(baselineInvestigationForm.toString())
                .withFormName(Constants.JsonForm.AncFirstVisit.getBaselineInvestigation())
                .withHelper(new AncBaselineInvestigationAction(memberObject))
                .build();
        actionList.put(context.getString(R.string.anc_first_visit_baseline_investigation), baselineInvestigationAction);

        BaseAncHomeVisitAction vaccinationAction = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_first_visit_tt_vaccination))
                .withOptional(true)
                .withDetails(details)
                .withFormName(Constants.JsonForm.AncFirstVisit.getTtVaccination())
                .withHelper(new AncTtVaccinationAction(memberObject))
                .build();
        actionList.put(context.getString(R.string.anc_first_visit_tt_vaccination), vaccinationAction);

        BaseAncHomeVisitAction counsellingAction = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_first_and_recurring_visit_counselling))
                .withOptional(true)
                .withDetails(details)
                .withFormName(Constants.JsonForm.getCounselling())
                .withHelper(new AncCounsellingAction(memberObject))
                .build();
        actionList.put(context.getString(R.string.anc_first_and_recurring_visit_counselling), counsellingAction);

        BaseAncHomeVisitAction partnerRegistration = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.partner_registration_action_title))
                .withOptional(true)
                .withDetails(details)
                .withHelper(new AncPartnerRegistrationAction(memberObject))
                .withFormName(Constants.JsonForm.AncRecurringVisit.getPartnerRegistration())
                .build();
        actionList.put(context.getString(R.string.partner_registration_action_title), partnerRegistration);

        BaseAncHomeVisitAction partnerTesting = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.partner_testing_action_title))
                .withOptional(true)
                .withDetails(details)
                .withHelper(new AncPartnerTestingAction(memberObject))
                .withJsonPayload(partnerTestingForm.toString())
                .withFormName(Constants.JsonForm.AncRecurringVisit.getPartnerTesting())
                .build();
        actionList.put(context.getString(R.string.partner_testing_action_title), partnerTesting);

        JSONObject birthReviewForm = initializeHealthFacilitiesList(FormUtils.getFormUtils().getFormJson(Constants.JsonForm.AncRecurringVisit.BIRTH_REVIEW_AND_EMERGENCY_PLAN));
        BaseAncHomeVisitAction birthReview = new BaseAncHomeVisitAction.Builder(context, context.getString(R.string.anc_recuring_visit_review_birth_and_emergency_plan))
                .withOptional(true)
                .withDetails(details)
                .withJsonPayload(birthReviewForm.toString())
                .withFormName(Constants.JsonForm.AncRecurringVisit.getBirthReviewAndEmergencyPlan())
                .withHelper(new AncBirthReviewAction(memberObject))
                .build();
        actionList.put(context.getString(R.string.anc_recuring_visit_review_birth_and_emergency_plan), birthReview);
    }


    private class AncMedicalAndSurgicalHistoryAction extends org.smartregister.chw.hf.actionhelper.AncMedicalAndSurgicalHistoryAction {
        private String medical_and_surgical_history_present;
        private Context context;

        public AncMedicalAndSurgicalHistoryAction(MemberObject memberObject) {
            super(memberObject);
        }

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                medical_and_surgical_history_present = CoreJsonFormUtils.getCheckBoxValue(jsonObject, "medical_surgical_history");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public String evaluateSubTitle() {
            return MessageFormat.format("Medical and Surgical History : {0}", medical_and_surgical_history_present);
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(medical_and_surgical_history_present)) {
                return BaseAncHomeVisitAction.Status.PENDING;
            } else {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            }
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }
    }

    private class AncObstetricExaminationAction extends org.smartregister.chw.hf.actionhelper.AncObstetricExaminationAction {
        private String abdominal_scars;
        private Context context;

        public AncObstetricExaminationAction(MemberObject memberObject) {
            super(memberObject);
        }

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(abdominal_scars))
                return BaseAncHomeVisitAction.Status.PENDING;
            else {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            }
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                abdominal_scars = CoreJsonFormUtils.getValue(jsonObject, "abdominal_scars");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }

        @Override
        public String evaluateSubTitle() {
            if (!StringUtils.isBlank(abdominal_scars))
                return "Obstetric Examination Complete";
            return "";
        }
    }

    private static JSONObject initializeHealthFacilitiesList(JSONObject form) {
        HfLocationRepository locationRepository = new HfLocationRepository();
        List<Location> locations = locationRepository.getAllLocationsWithTags();
        if (locations != null && form != null) {

            Collections.sort(locations, (location1, location2) -> StringUtils.capitalize(location1.getProperties().getName()).compareTo(StringUtils.capitalize(location2.getProperties().getName())));
            try {
                JSONArray fields = form.getJSONObject(Constants.JsonFormConstants.STEP1)
                        .getJSONArray(JsonFormConstants.FIELDS);
                JSONObject referralHealthFacilities = null;
                for (int i = 0; i < fields.length(); i++) {
                    if (fields.getJSONObject(i)
                            .getString(JsonFormConstants.KEY).equals(Constants.JsonFormConstants.NAME_OF_HF)
                    ) {
                        referralHealthFacilities = fields.getJSONObject(i);
                        break;
                    }
                }
                JSONArray tree = referralHealthFacilities.getJSONArray("tree");
                String parentTagName = "Zone";
                for (Location location : locations) {
                    Set<LocationTag> locationTags = location.getLocationTags();
                    if (locationTags.iterator().next().getName().equalsIgnoreCase(parentTagName)) {
                        JSONObject treeNode = new JSONObject();
                        treeNode.put("name", StringUtils.capitalize(location.getProperties().getName()));
                        treeNode.put("key", StringUtils.capitalize(location.getProperties().getName()));

                        JSONArray childNodes = setChildNodes(locations, location.getId(), parentTagName);
                        if (childNodes != null)
                            treeNode.put("nodes", childNodes);

                        tree.put(treeNode);
                    }
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
        return form;
    }

    private static JSONArray setChildNodes(List<Location> locations, String parentLocationId, String parentTagName) {
        JSONArray nodes = new JSONArray();
        ArrayList<String> locationHierarchyTags = new ArrayList<>(Arrays.asList(BuildConfig.LOCATION_HIERACHY));

        for (Location location : locations) {
            Set<LocationTag> locationTags = location.getLocationTags();
            String childTagName = locationHierarchyTags.get(locationHierarchyTags.indexOf(parentTagName) + 1);
            if (locationTags.iterator().next().getName().equalsIgnoreCase(childTagName) && location.getProperties().getParentId().equals(parentLocationId)) {
                JSONObject childNode = new JSONObject();
                try {
                    childNode.put("name", StringUtils.capitalize(location.getProperties().getName()));
                    childNode.put("key", StringUtils.capitalize(location.getProperties().getName()));

                    JSONArray childNodes = setChildNodes(locations, location.getId(), childTagName);
                    if (childNodes != null)
                        childNode.put("nodes", childNodes);

                    nodes.put(childNode);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (nodes.length() > 0) {
            return nodes;
        } else return null;

    }

    private static JSONObject setMinFundalHeight(JSONObject form, String baseEntityId) {
        String fundalHeight = HfAncDao.getFundalHeight(baseEntityId);
        try {
            JSONArray fields = form.getJSONObject(Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            JSONObject fundalHeightQnObj = null;
            for (int i = 0; i < fields.length(); i++) {
                if (fields.getJSONObject(i).getString(JsonFormConstants.KEY).equals("fundal_height")) {
                    fundalHeightQnObj = fields.getJSONObject(i);
                    break;
                }
            }

            assert fundalHeightQnObj != null;
            JSONObject v_min = fundalHeightQnObj.getJSONObject("v_min");
            v_min.put("value", fundalHeight);
            v_min.put("err", "Fundal height must be equal or greater than " + fundalHeight + " CM");


        } catch (JSONException e) {
            Timber.e(e);
        }
        return form;
    }

    private class AncBaselineInvestigationAction extends org.smartregister.chw.hf.actionhelper.AncBaselineInvestigationAction {
        private String glucose_in_urine;
        private Context context;

        public AncBaselineInvestigationAction(MemberObject memberObject) {
            super(memberObject);
        }

        @Override
        public void onJsonFormLoaded(String s, Context context, Map<String, List<VisitDetail>> map) {
            this.context = context;
        }

        @Override
        public String getPreProcessed() {
            return null;
        }

        @Override
        public void onPayloadReceived(String jsonPayload) {
            try {
                JSONObject jsonObject = new JSONObject(jsonPayload);
                glucose_in_urine = CoreJsonFormUtils.getValue(jsonObject, "glucose_in_urine");
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        @Override
        public BaseAncHomeVisitAction.ScheduleStatus getPreProcessedStatus() {
            return null;
        }

        @Override
        public String getPreProcessedSubTitle() {
            return null;
        }

        @Override
        public String postProcess(String s) {
            return null;
        }

        @Override
        public void onPayloadReceived(BaseAncHomeVisitAction baseAncHomeVisitAction) {
            Timber.v("onPayloadReceived");
        }

        @Override
        public BaseAncHomeVisitAction.Status evaluateStatusOnPayload() {
            if (StringUtils.isBlank(glucose_in_urine))
                return BaseAncHomeVisitAction.Status.PENDING;
            else {
                return BaseAncHomeVisitAction.Status.COMPLETED;
            }
        }

        @Override
        public String evaluateSubTitle() {
            if (!StringUtils.isBlank(glucose_in_urine))
                return "Baseline Investigation Conducted";
            return "";
        }
    }

    private static JSONObject firstPregnancyAboveThirtyFive(JSONObject form, MemberObject memberObject, Context context) throws JSONException{

            JSONArray fields = form.getJSONObject(Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            JSONObject medicalSurgicalHistory = null;
            for (int i = 0; i < fields.length(); i++) {
                if (fields.getJSONObject(i).getString(JsonFormConstants.KEY).equals("medical_surgical_history")) {
                    medicalSurgicalHistory = fields.getJSONObject(i);
                    break;
                }
            }

            JSONArray options = medicalSurgicalHistory.getJSONArray("options");

            JSONObject pregnantAtAboveThirtyFive = new JSONObject();
            pregnantAtAboveThirtyFive.put("key", "first_pregnancy_at_or_above_thirty_five");
            pregnantAtAboveThirtyFive.put("text", context.getString(R.string.first_pregnancy_option));
            pregnantAtAboveThirtyFive.put("value", false);
            pregnantAtAboveThirtyFive.put("openmrs_entity", "concept");
            pregnantAtAboveThirtyFive.put("openmrs_entity_id", "first_pregnancy_at_or_above_thirty_five");

            JSONObject none = new JSONObject();
            none.put("key", "none");
            none.put("text", context.getString(R.string.none_option_for_medical_surgical_history));
            none.put("value", false);
            none.put("openmrs_entity", "concept");
            none.put("openmrs_entity_id", "none");

            if (memberObject.getAge() >= 35) {
                options.put(pregnantAtAboveThirtyFive);
            }

            options.put(none);

        return form;
    }

}
