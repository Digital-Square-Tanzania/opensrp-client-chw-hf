package org.smartregister.chw.hf.actionhelper.vmmc;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hf.utils.VisitUtils;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;
import org.smartregister.family.util.JsonFormUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

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

    private HashMap<String, Boolean> checkObject = new HashMap<>();


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

            checkObject.clear();

            checkObject.put("has_client_had_any_sti", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "has_client_had_any_sti")));
            checkObject.put("any_complaints", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "any_complaints")));
            checkObject.put("is_client_diagnosed_with_any", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "is_client_diagnosed_with_any")));
//            checkObject.put("blood_for_glucose_test", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "blood_for_glucose_test")));
//            checkObject.put("type_of_blood_for_glucose_test", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "type_of_blood_for_glucose_test")));
//            checkObject.put("blood_for_glucose", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "blood_for_glucose")));
//            checkObject.put("ctc_number", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "ctc_number")));
//            checkObject.put("ctc_name", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "ctc_name")));
//            checkObject.put("diabetes_treatment", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "diabetes_treatment")));
//            checkObject.put("hypertension_treatment", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "hypertension_treatment")));
            checkObject.put("surgical_procedure", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "surgical_procedure")));
//            checkObject.put("complications_previous_surgical", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "complications_previous_surgical")));
//            checkObject.put("type_complication", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "type_complication")));
            checkObject.put("known_allergies", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "known_allergies")));
            checkObject.put("tetanus_vaccination", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "tetanus_vaccination")));
            checkObject.put("any_hematological_disease_symptoms", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "any_hematological_disease_symptoms")));



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
    public String postProcess(String jsonPayload) {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(jsonPayload);
            JSONArray fields = JsonFormUtils.fields(jsonObject);
            JSONObject medicalHistoryCompletionStatus = JsonFormUtils.getFieldJSONObject(fields, "medical_history_completion_status");
            assert medicalHistoryCompletionStatus != null;
            medicalHistoryCompletionStatus.put(com.vijay.jsonwizard.constants.JsonFormConstants.VALUE, VisitUtils.getActionStatus(checkObject));
        } catch (JSONException e) {
            Timber.e(e);
        }

        if (jsonObject != null) {
            return jsonObject.toString();
        }
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseVmmcVisitAction.Status evaluateStatusOnPayload() {
        String status = VisitUtils.getActionStatus(checkObject);

        if (status.equalsIgnoreCase(VisitUtils.Complete)) {
            return BaseVmmcVisitAction.Status.COMPLETED;
        }
        if (status.equalsIgnoreCase(VisitUtils.Ongoing)) {
            return BaseVmmcVisitAction.Status.PARTIALLY_COMPLETED;
        }
        return BaseVmmcVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseVmmcVisitAction baseVmmcVisitAction) {
        Timber.v("onPayloadReceived");
    }
}
