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

public class VmmcVisitProcedureInteractor extends BaseVmmcVisitInteractor {

    String visitType;
    protected BaseVmmcVisitContract.InteractorCallBack callBack;

    public VmmcVisitProcedureInteractor(String visitType) {
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
                evaluateConsentForm(details);
                evaluateMcProcedure(details);

            } catch (BaseVmmcVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateConsentForm(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject consentVisitType = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.CONSENT_FORM);

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
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_consent_form))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(consentVisitType.toString())
//                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.CONSENT_FORM)
                .build();
        actionList.put(context.getString(R.string.vmmc_consent_form), action);

    }

    private void evaluateMcProcedure(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject procedureVisitType = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.MC_PROCEDURE);


        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_mc_procedure))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(procedureVisitType.toString())
//                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.MC_PROCEDURE)
                .build();
        actionList.put(context.getString(R.string.vmmc_mc_procedure), action);
    }


    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.VMMC_PROCEDURE;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.VMMC_PROCEDURE;
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

