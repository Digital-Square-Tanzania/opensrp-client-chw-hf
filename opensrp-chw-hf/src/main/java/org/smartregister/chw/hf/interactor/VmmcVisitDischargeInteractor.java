package org.smartregister.chw.hf.interactor;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.vmmc.contract.BaseVmmcVisitContract;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.interactor.BaseVmmcVisitInteractor;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.chw.vmmc.util.Constants;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class VmmcVisitDischargeInteractor extends BaseVmmcVisitInteractor {

    String visitType;
    protected BaseVmmcVisitContract.InteractorCallBack callBack;

    public VmmcVisitDischargeInteractor(String visitType) {
        this.visitType = visitType;
    }

    @Override
    protected String getCurrentVisitType() {
        if (StringUtils.isNotBlank(visitType)) {
            return visitType;
        }
        return super.getCurrentVisitType();
    }

    @Override
    protected void populateActionList(BaseVmmcVisitContract.InteractorCallBack callBack) {
        this.callBack = callBack;
        final Runnable runnable = () -> {
            try {
                evaluatePostForm(details);
                evaluateFirstVitalProcedure(details);
                evaluateSecondVital(details);
                evaluateVmmcDischarge(details);

            } catch (BaseVmmcVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluatePostForm(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
//        JSONObject prepVisitType = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.MEDICAL_HISTORY);

//        try {
//            if (HfVmmcDao.hasPrepFollowup(memberObject.getBaseEntityId())) {
////                JSONArray fields = prepVisitType.getJSONObject(STEP1).getJSONArray(FIELDS);
////                JSONObject visitType = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "medical_history");
////                visitType.getJSONArray("options").remove(2);
////                visitType.getJSONArray("options").remove(0);
//            }
//        } catch (Exception e) {
//            Timber.e(e);
//        }

//        VmmcVisitTypeActionHelper actionHelper = new VmmcVisitTypeActionHelper();
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_post))
                .withOptional(false)
                .withDetails(details)
//                .withJsonPayload(prepVisitType.toString())
//                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.POST_OP)
                .build();
        actionList.put(context.getString(R.string.vmmc_post), action);

    }

    private void evaluateFirstVitalProcedure(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_first_vital))
                .withOptional(false)
                .withDetails(details)
//                .withJsonPayload(prepVisitType.toString())
//                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.FIRST_VITAL_SIGN)
                .build();
        actionList.put(context.getString(R.string.vmmc_first_vital), action);
    }

    private void evaluateSecondVital(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
//        JSONObject prepInitiation = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.HTS);

//        try {
//            if (HfVmmcDao.isPrEPInitiated(memberObject.getBaseEntityId())) {
//                JSONArray fields = prepInitiation.getJSONObject(STEP1).getJSONArray(FIELDS);
//                JSONObject prepStatus = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "prep_status");
//                prepStatus.getJSONArray("options").remove(3);
//                prepStatus.getJSONArray("options").remove(0);
//            }
//        } catch (Exception e) {
//            Timber.e(e);
//        }

        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_second_vital))
                .withOptional(false)
                .withDetails(details)
//                .withJsonPayload(prepVisitType.toString())
//                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.SECOND_VITAL_SIGN)
                .build();
        actionList.put(context.getString(R.string.vmmc_second_vital), action);
    }

    private void evaluateVmmcDischarge(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {

//        PrEPOtherServicesActionHelper actionHelper = new PrEPOtherServicesActionHelper();
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_post_discharge))
                .withOptional(true)
                .withDetails(details)
//                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.DISCHARGE)
                .build();

        actionList.put(context.getString(R.string.vmmc_discharge), action);
    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.VMMC_DISCHARGE;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.VMMC_CONFIRMATION;
    }

//    private class VmmcVisitTypeActionHelper extends org.smartregister.chw.hf.actionhelper.vmmc.VmmcVisitTypeActionHelper {
//        @Override
//        public String postProcess(String s) {
//            if (StringUtils.isNotBlank(medical_history)) {
//                try {
//                    evaluatePrEPScreening(details);
//                } catch (BaseVmmcVisitAction.ValidationException e) {
//                    e.printStackTrace();
//                }
//            } else {
////                actionList.remove(context.getString(R.string.vmmc_medical_history));
////                actionList.remove(context.getString(R.string.vmmc_physical_examination));
////                actionList.remove(context.getString(R.string.vmmc_hts));
//            }
//            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
//            return super.postProcess(s);
//        }
//
//    }

//    private class VmmcScreeningActionHelper extends org.smartregister.chw.hf.actionhelper.vmmc.VmmcScreeningActionHelper {
//        public VmmcScreeningActionHelper(String baseEntityId) {
//            super(baseEntityId);
//        }
//
//        @Override
//        public String postProcess(String s) {
//            if (should_initiate.equalsIgnoreCase("yes")) {
//                try {
//                    evaluatePrEPInitiation(details);
////                    evaluateOtherServices(details);
//                } catch (BaseVmmcVisitAction.ValidationException e) {
//                    e.printStackTrace();
//                }
//            } else {
//                actionList.remove(context.getString(R.string.prep_initiation));
//                actionList.remove(context.getString(R.string.other_services));
//            }
//            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
//            return super.postProcess(s);
//        }
//    }
}
