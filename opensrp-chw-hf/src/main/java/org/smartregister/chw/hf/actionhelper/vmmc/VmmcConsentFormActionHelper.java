package org.smartregister.chw.hf.actionhelper.vmmc;

import static org.smartregister.chw.core.utils.Utils.getCommonPersonObjectClient;
import static org.smartregister.chw.core.utils.Utils.getDuration;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hf.utils.Constants;
import org.smartregister.chw.pmtct.util.JsonFormUtils;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.family.util.DBConstants;

import java.util.List;
import java.util.Map;

public class VmmcConsentFormActionHelper implements BaseVmmcVisitAction.VmmcVisitActionHelper {

    protected String consent_form;

    protected String jsonPayload;

    protected String mc_procedure;

    protected String baseEntityId;

    protected Integer age;


    public VmmcConsentFormActionHelper(String baseEntityId, Integer age) {
        this.baseEntityId = baseEntityId;
        this.age = age;
    }


    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONArray fields = jsonObject.getJSONObject(Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);

            JSONObject actualAge = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "actual_age");
//            CommonPersonObjectClient client = getCommonPersonObjectClient(baseEntityId);
            actualAge.put(JsonFormUtils.VALUE, age);

            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            consent_form = CoreJsonFormUtils.getValue(jsonObject, "client_consent_for_mc_procedure");
            mc_procedure = CoreJsonFormUtils.getValue(jsonObject, "consent_from");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BaseVmmcVisitAction.ScheduleStatus getPreProcessedStatus() {
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
        return null;
    }

    @Override
    public BaseVmmcVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(consent_form))
            return BaseVmmcVisitAction.Status.PENDING;
        else {
            return BaseVmmcVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseVmmcVisitAction baseVmmcVisitAction) {
        //overridden
    }
}
