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

public class VmmcPhysicalExamActionHelper implements BaseVmmcVisitAction.VmmcVisitActionHelper {

    protected String jsonPayload;

    protected String baseEntityId;

    protected String medical_history;

    protected static String genital_examination;

    protected static String diastolic;

    protected static String systolic;

    private HashMap<String, Boolean> checkObject = new HashMap<>();

    public VmmcPhysicalExamActionHelper(String baseEntityId) {
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

            genital_examination = CoreJsonFormUtils.getValue(jsonObject, "genital_examination");
            global.put("contraindication", genital_examination);

            diastolic = CoreJsonFormUtils.getValue(jsonObject, "diastolic");
            systolic = CoreJsonFormUtils.getValue(jsonObject, "systolic");

            medical_history = CoreJsonFormUtils.getValue(jsonObject, "physical_abnormality");


            checkObject.clear();

            checkObject.put("physical_abnormality", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "physical_abnormality")));
            checkObject.put("client_weight", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "client_weight")));
            checkObject.put("client_height", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "client_height")));
            checkObject.put("bmi", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "bmi")));
            checkObject.put("pulse_rate", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "pulse_rate")));
            checkObject.put("systolic", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "systolic")));
            checkObject.put("diastolic", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "diastolic")));
            checkObject.put("temperature", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "temperature")));
            checkObject.put("respiration_rate", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "respiration_rate")));
            checkObject.put("genital_examination", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "genital_examination")));
            checkObject.put("preferred_client_mc_method", StringUtils.isNotBlank(CoreJsonFormUtils.getValue(jsonObject, "preferred_client_mc_method")));

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
            JSONObject physcialExamCompletionStatus = JsonFormUtils.getFieldJSONObject(fields, "physical_exam_completion_status");
            assert physcialExamCompletionStatus != null;
            physcialExamCompletionStatus.put(com.vijay.jsonwizard.constants.JsonFormConstants.VALUE, VisitUtils.getActionStatus(checkObject));
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
        //overridden
    }

}
