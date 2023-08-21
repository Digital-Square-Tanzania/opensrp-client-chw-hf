package org.smartregister.chw.hf.interactor;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.vmmc.VmmcHtsActionHelper;
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
        JSONObject vmmcMedicalHistory = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.MEDICAL_HISTORY);

        VmmcVisitTypeActionHelper actionHelper = new VmmcVisitTypeActionHelper(memberObject.getBaseEntityId());
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_medical_history))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(vmmcMedicalHistory.toString())
                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.MEDICAL_HISTORY)
                .build();
        actionList.put(context.getString(R.string.vmmc_medical_history), action);

    }

    private void evaluateVmmcPhysicalExam(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject vmmcPhysicalExam = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.PHYSICAL_EXAMINATION);

        VmmcPhysicalExamActionHelper actionHelper = new VmmcPhysicalExamActionHelper(memberObject.getBaseEntityId());
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_physical_examination))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(vmmcPhysicalExam.toString())
                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.PHYSICAL_EXAMINATION)
                .build();
        actionList.put(context.getString(R.string.vmmc_physical_examination), action);
    }

    private void evaluateVmmcHTS(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject vmmcHTS = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.HTS);

        VmmcHtsActionHelper actionHelper = new VmmcHtsActionHelper();
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_hts))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(vmmcHTS.toString())
                .withHelper(actionHelper)
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
        return Constants.EVENT_TYPE.VMMC_SERVICES;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.VMMC_SERVICE;
    }

    private class VmmcVisitTypeActionHelper extends org.smartregister.chw.hf.actionhelper.vmmc.VmmcVisitTypeActionHelper {
        public VmmcVisitTypeActionHelper(String baseEntityId) {
            super(baseEntityId);
        }


        @Override
        public String postProcess(String s) {
            if (StringUtils.isNotBlank(medical_history)) {
                try {
                    evaluateVmmcPhysicalExam(details);
                    evaluateVmmcHTS(details);
                } catch (BaseVmmcVisitAction.ValidationException e) {
                    e.printStackTrace();
                }
            } else {
////                actionList.remove(context.getString(R.string.vmmc_medical_history));
////                actionList.remove(context.getString(R.string.vmmc_physical_examination));
////                actionList.remove(context.getString(R.string.vmmc_hts));
            }
            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
            return super.postProcess(s);
        }

    }

    private class VmmcPhysicalExamActionHelper extends org.smartregister.chw.hf.actionhelper.vmmc.VmmcPhysicalExamActionHelper {
        public VmmcPhysicalExamActionHelper(String baseEntityId) {
            super(baseEntityId);
        }

        @Override
        public String postProcess(String s) {
            if (StringUtils.isNotBlank(medical_history)) {
                try {
                    evaluateVmmcHTS(details);
                } catch (BaseVmmcVisitAction.ValidationException e) {
                    e.printStackTrace();
                }
            } else {
////                actionList.remove(context.getString(R.string.vmmc_medical_history));
////                actionList.remove(context.getString(R.string.vmmc_physical_examination));
////                actionList.remove(context.getString(R.string.vmmc_hts));
            }
            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
            return super.postProcess(s);
        }

    }
}
