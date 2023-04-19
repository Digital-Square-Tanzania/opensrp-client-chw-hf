package org.smartregister.chw.hf.interactor;

import static org.smartregister.client.utils.constants.JsonFormConstants.FIELDS;
import static org.smartregister.client.utils.constants.JsonFormConstants.STEP1;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.vmmc.VmmcInitiationActionHelper;
import org.smartregister.chw.hf.actionhelper.prep.PrEPOtherServicesActionHelper;
import org.smartregister.chw.hf.dao.HfVmmcDao;
import org.smartregister.chw.vmmc.contract.BaseVmmcVisitContract;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.interactor.BaseVmmcVisitInteractor;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.chw.vmmc.util.Constants;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class VmmcVisitInteractor extends BaseVmmcVisitInteractor {

    String visitType;
    protected BaseVmmcVisitContract.InteractorCallBack callBack;

    public VmmcVisitInteractor(String visitType) {
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
                evaluateVisitType(details);
                evaluateVmmcPhysicalExam(details);
                evaluateVmmcHTS(details);

            } catch (BaseVmmcVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateVisitType(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject prepVisitType = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.MEDICAL_HISTORY);

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
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_medical_history))
                .withOptional(false)
                .withDetails(details)
//                .withJsonPayload(prepVisitType.toString())
//                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.MEDICAL_HISTORY)
                .build();
        actionList.put(context.getString(R.string.vmmc_medical_history), action);

    }

    private void evaluateVmmcPhysicalExam(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_physical_examination))
                .withOptional(false)
                .withDetails(details)
//                .withJsonPayload(prepVisitType.toString())
//                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.PHYSICAL_EXAMINATION)
                .build();
        actionList.put(context.getString(R.string.vmmc_physical_examination), action);
    }

    private void evaluateVmmcHTS(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject prepInitiation = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.HTS);

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

        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_hts))
                .withOptional(false)
                .withDetails(details)
//                .withJsonPayload(prepVisitType.toString())
//                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.HTS)
                .build();
        actionList.put(context.getString(R.string.vmmc_hts), action);
    }

//    private void evaluateOtherServices(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
//
//        PrEPOtherServicesActionHelper actionHelper = new PrEPOtherServicesActionHelper();
//        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.other_services))
//                .withOptional(true)
//                .withDetails(details)
//                .withHelper(actionHelper)
//                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.HTS)
//                .build();
//
//        actionList.put(context.getString(R.string.other_services), action);
//    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.VMMC_CONFIRMATION;
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
