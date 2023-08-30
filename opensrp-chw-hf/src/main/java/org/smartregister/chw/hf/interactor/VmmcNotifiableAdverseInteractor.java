package org.smartregister.chw.hf.interactor;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.vmmc.VmmcNotifiableAdverseActionHelper;
import org.smartregister.chw.vmmc.contract.BaseVmmcVisitContract.InteractorCallBack;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.interactor.BaseVmmcVisitInteractor;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.chw.vmmc.util.Constants;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class VmmcNotifiableAdverseInteractor extends BaseVmmcVisitInteractor {

    String visitType;
    protected InteractorCallBack callBack;

    public VmmcNotifiableAdverseInteractor(String visitType) {
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
                evaluateVisitType(details);

            } catch (BaseVmmcVisitAction.ValidationException e) {
                Timber.e(e);
            }

            appExecutors.mainThread().execute(() -> callBack.preloadActions(actionList));
        };

        appExecutors.diskIO().execute(runnable);
    }

    private void evaluateVisitType(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {
        JSONObject vmmcMedicalHistory = FormUtils.getFormUtils().getFormJson(Constants.FORMS.VMMC_NOTIFIABLE);

        VmmcNotifiableAdverseActionHelper actionHelper = new VmmcNotifiableAdverseActionHelper(memberObject.getBaseEntityId());
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_notifiable_adverse))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(vmmcMedicalHistory.toString())
                .withHelper(actionHelper)
                .withFormName(Constants.FORMS.VMMC_NOTIFIABLE)
                .build();
        actionList.put(context.getString(R.string.vmmc_notifiable_adverse), action);

    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.VMMC_NOTIFIABLE_EVENTS;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.VMMC_NOTIFIABLE_EVENT;
    }

}
