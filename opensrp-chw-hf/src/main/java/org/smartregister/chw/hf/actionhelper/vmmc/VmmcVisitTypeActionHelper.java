package org.smartregister.chw.hf.actionhelper.vmmc;

import android.content.Context;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;

import java.util.List;
import java.util.Map;

public class VmmcVisitTypeActionHelper implements BaseVmmcVisitAction.VmmcVisitActionHelper {

    protected String medical_history;
    protected String jsonPayload;
    protected static String hiv_info;
    protected static String client_diagnosed;
    protected static String any_complaints;
    protected static String complications_previous_surgical;
    protected static String hematological_disease;
    protected static String known_allergies;
    protected static String type_of_blood_for_glucose_test;
    protected static String blood_for_glucose;
    protected static String blood_for_glucose_test;

    private String baseEntityId;

    public VmmcVisitTypeActionHelper(String baseEntityId) {
        this.baseEntityId = baseEntityId;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
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
            JSONObject global = jsonObject.getJSONObject("global");

            hiv_info = CoreJsonFormUtils.getValue(jsonObject, "client_diagnosed");

            client_diagnosed = CoreJsonFormUtils.getValue(jsonObject, "client_diagnosed");
            any_complaints = CoreJsonFormUtils.getValue(jsonObject, "any_complaints");
            complications_previous_surgical = CoreJsonFormUtils.getValue(jsonObject, "complications_previous_surgical");
            hematological_disease = CoreJsonFormUtils.getValue(jsonObject, "hematological_disease");
            known_allergies = CoreJsonFormUtils.getValue(jsonObject, "known_allergies");
            type_of_blood_for_glucose_test = CoreJsonFormUtils.getValue(jsonObject, "type_of_blood_for_glucose_test");
            blood_for_glucose = CoreJsonFormUtils.getValue(jsonObject, "blood_for_glucose");
            blood_for_glucose_test = CoreJsonFormUtils.getValue(jsonObject, "blood_for_glucose_test");

            global.put("hiv_info", hiv_info);

            Log.d("vmmc-test", hiv_info);

            medical_history = CoreJsonFormUtils.getValue(jsonObject, "has_client_had_any_sti");
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
        if (StringUtils.isBlank(medical_history))
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
