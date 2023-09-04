package org.smartregister.chw.hf.interactor;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.AppExecutors;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.actionhelper.vmmc.VmmcNotifiableAdverseActionHelper;
import org.smartregister.chw.hf.dao.HfVmmcDao;
import org.smartregister.chw.vmmc.contract.BaseVmmcVisitContract.InteractorCallBack;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.interactor.BaseVmmcVisitInteractor;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.chw.vmmc.util.Constants;
import org.smartregister.chw.vmmc.util.VmmcJsonFormUtils;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class VmmcFollowUpInteractor extends BaseVmmcVisitInteractor {

    String visitType;

    protected InteractorCallBack callBack;

    public VmmcFollowUpInteractor(String visitType) {
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
        JSONObject vmmcFollowUpVisit = FormUtils.getFormUtils().getFormJson(Constants.FORMS.VMMC_FOLLOW_UP_VISIT);

        //trial ang
        try {
            JSONArray fields = vmmcFollowUpVisit.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            //update visit number
            JSONObject visitNumber = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "visit_number");
            visitNumber.put(org.smartregister.chw.pmtct.util.JsonFormUtils.VALUE, HfVmmcDao.getVisitNumber(memberObject.getBaseEntityId()));

            //loads details to the form
            if (details != null && !details.isEmpty()) {
                VmmcJsonFormUtils.populateForm(vmmcFollowUpVisit, details);
            }
        } catch (JSONException e) {
            Timber.e(e);
        }

        VmmcFollowUpActionHelper actionHelper = new VmmcFollowUpActionHelper(memberObject.getBaseEntityId());
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_followup_visit))
                .withOptional(false)
                .withDetails(details)
                .withJsonPayload(vmmcFollowUpVisit.toString())
                .withHelper(actionHelper)
                .withFormName(Constants.FORMS.VMMC_FOLLOW_UP_VISIT)
                .build();
        actionList.put(context.getString(R.string.vmmc_followup_visit), action);

    }

    private void evaluateVmmcNAE(Map<String, List<VisitDetail>> details) throws BaseVmmcVisitAction.ValidationException {

        VmmcNotifiableAdverseActionHelper actionHelper = new VmmcNotifiableAdverseActionHelper(memberObject.getBaseEntityId());
        BaseVmmcVisitAction action = getBuilder(context.getString(R.string.vmmc_notifiable_adverse))
                .withOptional(false)
                .withDetails(details)
                .withHelper(actionHelper)
                .withFormName(Constants.FORMS.VMMC_NOTIFIABLE)
                .build();
        actionList.put(context.getString(R.string.vmmc_notifiable_adverse), action);

    }

    @Override
    protected String getEncounterType() {
        return Constants.EVENT_TYPE.VMMC_FOLLOW_UP_VISIT;
    }

    @Override
    protected String getTableName() {
        return Constants.TABLES.VMMC_FOLLOW_UP;
    }

    private class VmmcFollowUpActionHelper extends org.smartregister.chw.hf.actionhelper.vmmc.VmmcFollowUpActionHelper {
        public VmmcFollowUpActionHelper(String baseEntityId) {
            super(baseEntityId);
        }

        @Override
        public String postProcess(String s) {
            if (notifiable_adverse_event_occured.equalsIgnoreCase("yes")) {
                try {
                    evaluateVmmcNAE(details);
                } catch (BaseVmmcVisitAction.ValidationException e) {
                    e.printStackTrace();
                }
            } else {
                actionList.remove(context.getString(R.string.vmmc_notifiable_adverse));
            }
            new AppExecutors().mainThread().execute(() -> callBack.preloadActions(actionList));
            return super.postProcess(s);
        }

    }

}
