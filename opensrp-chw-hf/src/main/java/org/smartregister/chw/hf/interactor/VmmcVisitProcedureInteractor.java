package org.smartregister.chw.hf.interactor;

import static org.smartregister.chw.vmmc.contract.BaseVmmcVisitContract.*;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.vmmc.VmmcProcedureActionHelper;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.interactor.BaseVmmcVisitInteractor;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.chw.vmmc.util.Constants;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class VmmcVisitProcedureInteractor extends BaseVmmcVisitInteractor {

    String visitType;

    protected InteractorCallBack callBack;

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
    protected void populateActionList(InteractorCallBack callBack) {
        this.callBack = callBack;
        final Runnable runnable = () -> {
            try {
                evaluateConsentForm(details);

            } catch (BaseVmmcVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateConsentForm(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject consentVisitType = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.CONSENT_FORM);

        VmmcConsentFormActionHelper actionHelper = new VmmcConsentFormActionHelper();
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.consent_form))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(consentVisitType.toString())
                .withHelper(actionHelper)
                .withFormName(Constants.VMMC_FOLLOWUP_FORMS.CONSENT_FORM)
                .build();
        actionList.put(context.getString(R.string.consent_form), action);

    }

    private void evaluateMcProcedure(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject procedureVisitType = FormUtils.getFormUtils().getFormJson(Constants.VMMC_FOLLOWUP_FORMS.MC_PROCEDURE);


        VmmcProcedureActionHelper actionHelper = new VmmcProcedureActionHelper();
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_mc_procedure))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(procedureVisitType.toString())
                .withHelper(actionHelper)
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

    private class VmmcConsentFormActionHelper extends org.smartregister.chw.hf.actionhelper.vmmc.VmmcConsentFormActionHelper {
        @Override
        public String postProcess(String s) {
            if (StringUtils.isNotBlank(mc_procedure)) {
                try {
                    evaluateMcProcedure(details);
                } catch (BaseVmmcVisitAction.ValidationException e) {
                    e.printStackTrace();
                }
            } else {
                actionList.remove(context.getString(R.string.vmmc_mc_procedure));
            }
            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
            return super.postProcess(s);
        }

    }

}

