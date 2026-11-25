package org.smartregister.chw.hf.interactor;

import static org.smartregister.AllConstants.OPTIONS;
import static org.smartregister.chw.hf.actionhelper.kvp.KvpClientStatusActionHelper.OTHER_CATEGORY;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.CLIENT_STATUS;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.ENROLLMENT_DATE;
import static org.smartregister.util.JsonFormUtils.VALUE;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.kvp.KvpCervicalCancerScreeningActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpCondomProvisionActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpFamilyPlanningActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpHepatitisActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpMatActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpPrepPepActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpStiScreeningActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpTbScreeningActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpVmmcActionHelper;
import org.smartregister.chw.hf.dao.HfKvpDao;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.hf.utils.MarpHts;
import org.smartregister.chw.hf.utils.MarpHtsSuggestion;
import org.smartregister.chw.hf.utils.MarpPregnancyStatus;
import org.smartregister.chw.hf.utils.MarpVisitCategories;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.contract.BaseKvpVisitContract;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.interactor.BaseKvpVisitInteractor;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class KvpBioMedicalServiceInteractor extends BaseKvpVisitInteractor {

    private static final String NEW_VISIT_TYPE = "new_client";
    private static final String CERVICAL_SCREENING_FIELD = "cervical_cancer_screening";

    protected BaseKvpVisitContract.InteractorCallBack callBack;
    private final boolean editingVisit;
    private boolean offerHtsSuggestion;
    private Event replacedBiomedicalEvent;
    private KvpClientStatusActionHelper clientStatusActionHelper;
    private BaseKvpVisitAction familyPlanningAction;
    private BaseKvpVisitAction cervicalScreeningAction;
    private String wasCondomGiven;

    public KvpBioMedicalServiceInteractor(boolean editingVisit) {
        super(Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT);
        this.editingVisit = editingVisit;
    }

    @Override
    protected void populateActionList(BaseKvpVisitContract.InteractorCallBack callBack) {
        this.callBack = callBack;
        final Runnable runnable = () -> {
            try {
                evaluateClientStatus(details);
                evaluateHts(details);
                evaluateCondomProvision(details);
                updateFamilyPlanning();
                evaluateTbScreening(details);
                evaluateStiScreening(details);
                evaluateHepatitis(details);
                if (Constants.MALE.equalsIgnoreCase(memberObject.getGender())) {
                    evaluateVmmc(details);
                }
                updateCervicalScreening();
                if (shouldShowMat())
                    evaluateMat(details);
            } catch (Exception e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateClientStatus(Map<String, List<VisitDetail>> details) throws Exception {

        JSONObject clientStatusForm = FormUtils.getFormUtils().getFormJson(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_CLIENT_STATUS);
        JSONArray fields = clientStatusForm.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);

        clientStatusForm.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1)
                .put("title", context.getString(R.string.kvp_client_status));
        JSONObject clientStatus = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, CLIENT_STATUS);
        JSONObject enrollmentDate = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, ENROLLMENT_DATE);
        localizeClientStatus(clientStatus, enrollmentDate);
        String dominantGroup = KvpDao.getDominantKVPGroup(memberObject.getBaseEntityId());
        filterVisitTypeOptions(clientStatus, HfKvpDao.getClientStatus(memberObject.getBaseEntityId()), details);

        JSONObject otherCategories = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, OTHER_CATEGORY);
        String gender = memberObject.getGender();
        int age = Constants.FEMALE.equalsIgnoreCase(gender) ? getAgeForCategoryEligibility() : 0;
        MarpVisitCategories.configure(otherCategories, context, dominantGroup, gender, age);

        clientStatusActionHelper = new KvpClientStatusActionHelper(memberObject,
                MarpPregnancyStatus.appliesTo(memberObject.getGender(), dominantGroup));
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_client_status))
                .withOptional(false)
                .withDetails(details)
                .withHelper(clientStatusActionHelper)
                .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_CLIENT_STATUS)
                .withJsonPayload(clientStatusForm.toString())
                .build();

        actionList.put(context.getString(R.string.kvp_client_status), action);
    }

    static void filterVisitTypeOptions(JSONObject clientStatus, String previousClientStatus,
                                       Map<String, List<VisitDetail>> details) throws Exception {
        List<VisitDetail> savedStatus = details == null ? null : details.get(CLIENT_STATUS);
        boolean savedAsNew = savedStatus != null && !savedStatus.isEmpty()
                && NEW_VISIT_TYPE.equals(KvpJsonFormUtils.getValue(savedStatus.get(0)));
        // The radio option is needed to serialize the restored answer when saving a history edit.
        if (StringUtils.isNotBlank(previousClientStatus) && !savedAsNew) {
            KvpJsonFormUtils.removeOptionFromCheckboxListWithKey(clientStatus, NEW_VISIT_TYPE);
        }
    }

    private int getAgeForCategoryEligibility() {
        try {
            return memberObject.getAge();
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    /** Resolves the answer saved in Client Status, including before Biomedical submission. */
    public String getPregnancyBreastfeedingStatus() {
        return clientStatusActionHelper == null ? "" : clientStatusActionHelper.getPregnancyBreastfeedingStatus();
    }

    private void localizeClientStatus(JSONObject clientStatus, JSONObject enrollmentDate) throws JSONException {
        clientStatus.put("label", context.getString(R.string.marp_visit_type));
        clientStatus.getJSONObject("v_required").put("err", context.getString(R.string.marp_visit_type_required));
        JSONArray options = clientStatus.getJSONArray(OPTIONS);
        org.smartregister.util.JsonFormUtils.getFieldJSONObject(options, NEW_VISIT_TYPE)
                .put("text", context.getString(R.string.marp_visit_type_new));
        org.smartregister.util.JsonFormUtils.getFieldJSONObject(options, "transfer_in")
                .put("text", context.getString(R.string.marp_visit_type_transfer_in));
        org.smartregister.util.JsonFormUtils.getFieldJSONObject(options, "return")
                .put("text", context.getString(R.string.marp_visit_type_return));
        enrollmentDate.put("hint", context.getString(R.string.marp_visit_programme_enrolment_date));
        enrollmentDate.getJSONObject("v_required").put("err", context.getString(R.string.marp_visit_enrolment_date_required));
    }

    @Override
    protected Visit saveVisit(boolean editMode, String memberId, String encounterType, Map<String, String> payloads,
                              String parentEventType) throws Exception {
        if (editMode) {
            // The shared save path replaces the latest Biomedical visit after preparing its new event.
            Visit visit = visitRepository().getLatestVisit(memberId, getEncounterType());
            replacedBiomedicalEvent = HfKvpDao.readSubmittedBiomedicalEvent(visit);
        }
        try {
            return super.saveVisit(editMode, memberId, encounterType, payloads, parentEventType);
        } finally {
            replacedBiomedicalEvent = null;
        }
    }

    @Override
    protected void prepareEvent(Event event) {
        super.prepareEvent(event);
        MarpHts.retainRemovedAnswers(replacedBiomedicalEvent, event);
        retainHiddenFamilyPlanningAnswers(replacedBiomedicalEvent, event);
        // The shared history save replaces the event and rebuilds its visit details from the observations.
        if (replacedBiomedicalEvent != null && replacedBiomedicalEvent.getObs() != null
                && !actionList.containsKey(context.getString(R.string.kvp_cervical_screening))) {
            for (Obs obs : replacedBiomedicalEvent.getObs()) {
                if (CERVICAL_SCREENING_FIELD.equals(obs.getFormSubmissionField())) {
                    event.addObs(obs);
                }
            }
        }
        String visitType = MarpEnrollmentDate.observation(event, CLIENT_STATUS);
        if (StringUtils.isNotBlank(MarpEnrollmentDate.observation(replacedBiomedicalEvent, CLIENT_STATUS))) {
            MarpEnrollmentDate.retainOnResave(replacedBiomedicalEvent, event);
        } else if (MarpEnrollmentDate.isTransferOrReturn(visitType)
                && StringUtils.isBlank(HfKvpDao.getClientStatus(event.getBaseEntityId()))) {
            MarpEnrollmentDate.recordFirstVisitDate(event);
        }
    }

    static void retainHiddenFamilyPlanningAnswers(Event original, Event replacement) {
        if (original == null || original.getObs() == null
                || !MarpPregnancyStatus.PREGNANT.equals(
                        MarpEnrollmentDate.observation(replacement, MarpPregnancyStatus.FIELD))) {
            return;
        }
        // A history edit replaces the event. Preserve Family Planning answers hidden from this edit.
        for (Obs obs : original.getObs()) {
            String field = obs.getFormSubmissionField();
            if ("family_planning_service".equals(field) || "condom_provided".equals(field)) {
                replacement.addObs(obs);
            }
        }
    }

    private void evaluateHts(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        String memberId = memberObject.getBaseEntityId();
        String lastHivStatus = HfKvpDao.getHivStatus(memberId);
        Calendar today = Calendar.getInstance();
        offerHtsSuggestion = canOfferHtsSuggestion(editingVisit, lastHivStatus, visitRepository(), memberId, today)
                && !HfKvpDao.hasRecordedMarpVisitForHts(memberId, today)
                && MarpHtsSuggestion.isEligible(KvpDao.getDominantKVPGroup(memberId),
                        HfKvpDao.getLatestPregnancyBreastfeedingStatus(memberId));
        KvpHtsActionHelper actionHelper = new KvpHtsActionHelper(lastHivStatus);
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_hts))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_HTS)
                .build();

        actionList.put(context.getString(R.string.kvp_hts), action);
    }

    public JSONObject prepareHtsForm(JSONObject form) throws JSONException {
        return offerHtsSuggestion ? MarpHtsSuggestion.forDisplay(form, context) : form;
    }

    static boolean canOfferHtsSuggestion(boolean editingVisit, String lastHivStatus,
                                         VisitRepository repository, String memberId, Calendar today) {
        if (editingVisit || StringUtils.isNotBlank(lastHivStatus)) {
            return false;
        }
        Visit latest = repository.getLatestVisit(memberId, Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT);
        return (latest == null || MarpVisitStatus.isDraft(latest))
                && !MarpVisitStatus.hasSubmittedVisitBeforeToday(repository, memberId, today);
    }

    private void evaluatePrepPep(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {
        JSONObject kvpPrepPepJson = FormUtils.getFormUtils().getFormJson(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_PrEP_PEP);
        try {
            JSONArray fields = kvpPrepPepJson.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            //update enrolled_to_prep
            JSONObject enrolled_to_prep_object = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "enrolled_to_prep");
            enrolled_to_prep_object.put(VALUE, KvpDao.getPrEPMember(memberObject.getBaseEntityId()) != null);
        } catch (Exception e) {
            Timber.e(e);
        }

        KvpPrepPepActionHelper actionHelper = new KvpPrepPepActionHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_pep_assesment))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withJsonPayload(kvpPrepPepJson.toString())
                .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_PrEP_PEP)
                .build();

        actionList.put(context.getString(R.string.kvp_pep_assesment), action);
    }

    private void evaluateCondomProvision(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpCondomProvisionActionHelper actionHelper = new KvpCondomProvisionActionHelper() {
            @Override
            public void processCondomsResults(String answer) {
                wasCondomGiven = answer;
                if (familyPlanningAction == null) {
                    return;
                }
                familyPlanningAction = null;
                try {
                    updateFamilyPlanning();
                } catch (BaseKvpVisitAction.ValidationException e) {
                    Timber.e(e);
                }
                appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
            }
        };

        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_condom_provision))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_CONDOM_PROVISION)
                .build();

        actionList.put(context.getString(R.string.kvp_condom_provision), action);
    }

    private void updateFamilyPlanning() throws BaseKvpVisitAction.ValidationException {
        String title = context.getString(R.string.kvp_family_planning);
        if (clientStatusActionHelper.isPregnantForBiomedicalServices()) {
            actionList.remove(title);
            return;
        }
        // Keep the form in memory when only Client Status changes, so restoring it keeps its answers.
        if (familyPlanningAction == null) {
            String formName = Constants.MALE.equalsIgnoreCase(memberObject.getGender())
                    ? Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_MALE_FAMILY_PLANNING_SERVICES
                    : Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_FEMALE_FAMILY_PLANNING_SERVICES;
            familyPlanningAction = getBuilder(title)
                    .withOptional(true)
                    .withDetails(details)
                    .withHelper(new KvpFamilyPlanningActionHelper(wasCondomGiven))
                    .withFormName(formName)
                    .build();
        }
        actionList.put(title, familyPlanningAction);
    }

    private void evaluateTbScreening(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpTbScreeningActionHelper actionHelper = new KvpTbScreeningActionHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_tb_screening))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_TB_INVESTIGATION)
                .build();

        actionList.put(context.getString(R.string.kvp_tb_screening), action);
    }

    private void evaluateStiScreening(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpStiScreeningActionHelper actionHelper = new KvpStiScreeningActionHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_sti_screening))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_STI_SCREENING)
                .build();

        actionList.put(context.getString(R.string.kvp_sti_screening), action);
    }

    private void evaluateHepatitis(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpHepatitisActionHelper actionHelper = new KvpHepatitisActionHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_heptitis_test))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_HEPATITIS)
                .build();

        actionList.put(context.getString(R.string.kvp_heptitis_test), action);
    }

    private void evaluateVmmc(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpVmmcActionHelper actionHelper = new KvpVmmcActionHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_vmmc))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_VMMC_SERVICES)
                .build();

        String vmmcServices = HfKvpDao.getVmmcServices(memberObject.getBaseEntityId());
        if (StringUtils.isBlank(vmmcServices) || vmmcServices.equalsIgnoreCase("not_provided"))
            actionList.put(context.getString(R.string.kvp_vmmc), action);
    }

    private void updateCervicalScreening() throws BaseKvpVisitAction.ValidationException {
        String title = context.getString(R.string.kvp_cervical_screening);
        if (!Constants.FEMALE.equalsIgnoreCase(memberObject.getGender())
                || clientStatusActionHelper.isPregnantForBiomedicalServices()) {
            actionList.remove(title);
            return;
        }
        if (actionList.containsKey(title)) {
            return;
        }
        if (cervicalScreeningAction == null) {
            cervicalScreeningAction = getBuilder(title)
                    .withOptional(true)
                    .withDetails(details)
                    .withHelper(new KvpCervicalCancerScreeningActionHelper())
                    .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_CERVICAL_CANCER_SCREENING)
                    .build();
        }

        // Restore the action before MAT and keep its in-progress answer across status changes.
        String matTitle = context.getString(R.string.kvp_mat);
        BaseKvpVisitAction matAction = actionList.remove(matTitle);
        actionList.put(title, cervicalScreeningAction);
        if (matAction != null) {
            actionList.put(matTitle, matAction);
        }
    }

    private void evaluateMat(Map<String, List<VisitDetail>> details) throws BaseKvpVisitAction.ValidationException {

        KvpMatActionHelper actionHelper = new KvpMatActionHelper();
        BaseKvpVisitAction action = getBuilder(context.getString(R.string.kvp_mat))
                .withOptional(true)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.KVP_BIO_MEDICAL_SERVICE_FORMS.KVP_MAT)
                .build();

        actionList.put(context.getString(R.string.kvp_mat), action);
    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.KVP_FOLLOW_UP;
    }

    private boolean shouldShowMat() {
        return KvpDao.getDominantKVPGroup(memberObject.getBaseEntityId()).equalsIgnoreCase("pwud") ||
                KvpDao.getDominantKVPGroup(memberObject.getBaseEntityId()).equalsIgnoreCase("pwid");
    }

    private class KvpHtsActionHelper extends org.smartregister.chw.hf.actionhelper.kvp.KvpHtsActionHelper {

        public KvpHtsActionHelper(String lastHivStatus) {
            super(lastHivStatus);
        }

        @Override
        public String postProcess(String s) {
            if (!hasAssessmentInput()) {
                actionList.remove(context.getString(R.string.kvp_pep_assesment));
                return super.postProcess(s);
            }
            if (!MarpHts.isPositiveResult(hiv_status)) {
                try {
                    if (KvpDao.getPrEPMember(memberObject.getBaseEntityId()) == null)
                        evaluatePrepPep(details);
                } catch (BaseKvpVisitAction.ValidationException e) {
                    e.printStackTrace();
                }
            } else {
                actionList.remove(context.getString(R.string.kvp_pep_assesment));
            }
            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
            return super.postProcess(s);
        }
    }

    private class KvpClientStatusActionHelper extends org.smartregister.chw.hf.actionhelper.kvp.KvpClientStatusActionHelper {
        public KvpClientStatusActionHelper(MemberObject memberObject, boolean requiresPregnancyStatus) {
            super(memberObject, requiresPregnancyStatus);
        }

        @Override
        public String postProcess(String payload) {
            if (actionList.containsKey(context.getString(R.string.kvp_client_status))) {
                try {
                    updateFamilyPlanning();
                    updateCervicalScreening();
                } catch (BaseKvpVisitAction.ValidationException e) {
                    Timber.e(e);
                }
            }
            if (hasDrugUseCategory() && !shouldShowMat()) {
                try {
                    evaluateMat(details);
                } catch (Exception e) {
                    Timber.e(e);
                }
            } else if (!shouldShowMat()) {
                actionList.remove(context.getString(R.string.kvp_mat));
            }

            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));

            return super.postProcess(payload);
        }
    }
}
