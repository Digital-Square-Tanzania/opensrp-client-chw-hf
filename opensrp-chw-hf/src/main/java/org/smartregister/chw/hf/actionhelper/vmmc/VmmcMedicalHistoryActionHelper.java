package org.smartregister.chw.hf.actionhelper.vmmc;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;

import java.util.List;
import java.util.Map;

public class VmmcMedicalHistoryActionHelper implements BaseVmmcVisitAction.VmmcVisitActionHelper {

    protected String jsonPayload;

    protected static String is_client_diagnosed_with_any;

    protected String medical_history;

    protected static String any_complaints;

    protected static String complications_previous_surgical;

    protected static String any_hematological_disease_symptoms;

    protected static String known_allergies;

    protected static String type_of_blood_for_glucose_test;

    protected static String blood_for_glucose;

    protected static String blood_for_glucose_test;

    protected String baseEntityId;

    public VmmcMedicalHistoryActionHelper(String baseEntityId) {
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

            is_client_diagnosed_with_any = CoreJsonFormUtils.getValue(jsonObject, "is_client_diagnosed_with_any");
            any_complaints = CoreJsonFormUtils.getValue(jsonObject, "any_complaints");
            complications_previous_surgical = CoreJsonFormUtils.getValue(jsonObject, "complications_previous_surgical");
            any_hematological_disease_symptoms = CoreJsonFormUtils.getValue(jsonObject, "any_hematological_disease_symptoms");
            known_allergies = CoreJsonFormUtils.getValue(jsonObject, "known_allergies");
            type_of_blood_for_glucose_test = CoreJsonFormUtils.getValue(jsonObject, "type_of_blood_for_glucose_test");
            blood_for_glucose = CoreJsonFormUtils.getValue(jsonObject, "blood_for_glucose");
            blood_for_glucose_test = CoreJsonFormUtils.getValue(jsonObject, "blood_for_glucose_test");

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
