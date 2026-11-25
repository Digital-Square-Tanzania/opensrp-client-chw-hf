package org.smartregister.chw.hf.actionhelper.kvp;

import static org.smartregister.chw.hf.utils.MarpHts.CTC_NUMBER;
import static org.smartregister.chw.hf.utils.MarpHts.CTC_SERVICES;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.MarpHts;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.util.JsonFormUtils;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class KvpHtsActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    private final String lastHivStatus;
    protected String hiv_status;
    private String testedHiv;
    private String jsonPayload;
    private Context context;
    private boolean editingSavedHts;

    public KvpHtsActionHelper(String lastHivStatus) {
        this.lastHivStatus = lastHivStatus;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
        this.context = context;
        editingSavedHts = map != null && map.containsKey("tested_hiv");
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject global = jsonObject.getJSONObject("global");

            global.put("hiv_status", lastHivStatus);
            global.put("editing_saved_hts", editingSavedHts ? "yes" : "no");

            JSONArray fields = jsonObject.getJSONObject("step1").getJSONArray("fields");
            JSONObject ctcServices = JsonFormUtils.getFieldJSONObject(fields, CTC_SERVICES);
            ctcServices.put("label", context.getString(R.string.marp_hts_ctc_services));
            ctcServices.getJSONObject("v_required").put("err", context.getString(R.string.marp_hts_ctc_services_required));
            JSONArray options = ctcServices.getJSONArray("options");
            JsonFormUtils.getFieldJSONObject(options, "enrolled_but_not_on_arv")
                    .put("text", context.getString(R.string.marp_hts_ctc_enrolled_without_art));
            JsonFormUtils.getFieldJSONObject(options, "enrolled_and_on_arv")
                    .put("text", context.getString(R.string.marp_hts_ctc_enrolled_on_art));

            JSONObject ctcNumber = JsonFormUtils.getFieldJSONObject(fields, CTC_NUMBER);
            ctcNumber.put("hint", context.getString(R.string.marp_hts_ctc_id));
            ctcNumber.getJSONObject("v_required").put("err", context.getString(R.string.marp_hts_ctc_id_required));
            ctcNumber.getJSONObject("v_regex").put("err", context.getString(R.string.marp_hts_ctc_id_invalid));

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
            testedHiv = CoreJsonFormUtils.getValue(jsonObject, "tested_hiv");
            hiv_status = CoreJsonFormUtils.getValue(jsonObject, "hiv_status");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BaseKvpVisitAction.ScheduleStatus getPreProcessedStatus() {
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
    public BaseKvpVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(testedHiv))
            return BaseKvpVisitAction.Status.PENDING;
        else {
            return BaseKvpVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseKvpVisitAction baseKvpVisitAction) {
        try {
            JSONObject form = new JSONObject(baseKvpVisitAction.getJsonPayload());
            JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
            JSONObject ctcServices = JsonFormUtils.getFieldJSONObject(fields, CTC_SERVICES);
            JSONObject ctcNumber = JsonFormUtils.getFieldJSONObject(fields, CTC_NUMBER);
            if (!MarpHts.isPositiveResult(hiv_status)) {
                ctcServices.remove(VALUE);
            }
            if (!MarpHts.isCtcEnrolled(ctcServices.optString(VALUE))) {
                ctcNumber.remove(VALUE);
            } else if (ctcNumber.has(VALUE)) {
                ctcNumber.put(VALUE, MarpHts.formatCtcId(ctcNumber.getString(VALUE)));
            }
            baseKvpVisitAction.setProcessedJsonPayload(form.toString());
        } catch (JSONException e) {
            Timber.e(e, "Unable to prepare the MARP HTS payload");
        }
    }
}
