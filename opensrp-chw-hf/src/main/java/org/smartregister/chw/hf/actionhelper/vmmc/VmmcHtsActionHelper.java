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

public class VmmcHtsActionHelper implements BaseVmmcVisitAction.VmmcVisitActionHelper {

    protected String medical_history;

    protected String jsonPayload;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject global = jsonObject.getJSONObject("global");


            String client_diagnosed = VmmcMedicalHistoryActionHelper.client_diagnosed;
            String any_complaints = VmmcMedicalHistoryActionHelper.any_complaints;
            String known_allergies = VmmcMedicalHistoryActionHelper.known_allergies;
            String hematological_disease = VmmcMedicalHistoryActionHelper.hematological_disease;
            String complications_previous_surgical = VmmcMedicalHistoryActionHelper.complications_previous_surgical;
            String type_of_blood_for_glucose_test = VmmcMedicalHistoryActionHelper.type_of_blood_for_glucose_test;
            String blood_for_glucose = VmmcMedicalHistoryActionHelper.blood_for_glucose;
            String blood_for_glucose_test = VmmcMedicalHistoryActionHelper.blood_for_glucose_test;
            String genital_examination = VmmcPhysicalExamActionHelper.genital_examination;

            global.put("client_diagnosed", client_diagnosed);
            global.put("any_complaints", any_complaints);
            global.put("known_allergies", known_allergies);
            global.put("hematological_disease", hematological_disease);
            global.put("complications_previous_surgical", complications_previous_surgical);
            global.put("type_of_blood_for_glucose_test", type_of_blood_for_glucose_test);
            global.put("blood_for_glucose", blood_for_glucose);
            global.put("blood_for_glucose_test", blood_for_glucose_test);
            global.put("genital_examination", genital_examination);

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
            medical_history = CoreJsonFormUtils.getValue(jsonObject, "was_client_referred");
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
