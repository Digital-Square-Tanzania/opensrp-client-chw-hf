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
import org.smartregister.chw.hf.utils.MarpHtsSuggestion;
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
    private String testingMethod;
    private String jsonPayload;
    private Context context;
    private boolean editingSavedHts;
    private boolean savingSuggestion;

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
            localizeYesNo(fields, "tested_hiv", R.string.marp_hts_tested_hiv);
            localizeYesNo(fields, "kits_distributed", R.string.marp_hts_kits_distributed);

            JSONArray methods = localizeRadio(fields, "previous_hiv_testing_method", R.string.marp_hts_testing_method);
            localizeChoice(methods, "hiv_cv_test", R.string.marp_hts_conventional_test);
            localizeChoice(methods, "hivst_and_hiv_cv_test", R.string.marp_hts_hivst_and_conventional_test);
            localizeChoice(methods, "other_method", R.string.marp_hts_other_method);

            JSONArray sites = localizeRadio(fields, "testing_site", R.string.marp_hts_testing_site);
            localizeChoice(sites, "community", R.string.marp_hts_community);
            localizeChoice(sites, "facility", R.string.marp_hts_facility);

            JSONArray results = localizeRadio(fields, "hiv_status", R.string.marp_hts_testing_results);
            localizeChoice(results, "negative", R.string.marp_hts_negative);
            localizeChoice(results, "positive", R.string.marp_hts_positive);
            localizeChoice(results, "inconclusive", R.string.marp_hts_inconclusive);
            localizeChoice(results, "refused_results", R.string.refused_results);
            localizeChoice(results, "known_positive", R.string.marp_hts_known_positive);

            JSONObject ctcServices = JsonFormUtils.getFieldJSONObject(fields, CTC_SERVICES);
            ctcServices.put("label", context.getString(R.string.marp_hts_ctc_services));
            ctcServices.getJSONObject("v_required").put("err", context.getString(R.string.marp_hts_ctc_services_required));
            JSONArray ctcOptions = ctcServices.getJSONArray("options");
            localizeChoice(ctcOptions, "enrolled_but_not_on_arv", R.string.marp_hts_ctc_enrolled_without_art);
            localizeChoice(ctcOptions, "enrolled_and_on_arv", R.string.marp_hts_ctc_enrolled_on_art);

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

    private JSONArray localizeRadio(JSONArray fields, String key, int label) throws JSONException {
        JSONObject field = JsonFormUtils.getFieldJSONObject(fields, key);
        field.put("label", context.getString(label));
        return field.getJSONArray("options");
    }

    private void localizeYesNo(JSONArray fields, String key, int label) throws JSONException {
        JSONArray options = localizeRadio(fields, key, label);
        localizeChoice(options, "yes", R.string.marp_hts_yes);
        localizeChoice(options, "no", R.string.marp_hts_no);
    }

    private void localizeChoice(JSONArray options, String key, int text) throws JSONException {
        JsonFormUtils.getFieldJSONObject(options, key).put("text", context.getString(text));
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            testedHiv = CoreJsonFormUtils.getValue(jsonObject, "tested_hiv");
            testingMethod = CoreJsonFormUtils.getValue(jsonObject, "previous_hiv_testing_method");
            savingSuggestion = MarpHtsSuggestion.isShown(jsonObject);
            hiv_status = savingSuggestion ? MarpHtsSuggestion.verifiedResult(jsonObject)
                    : CoreJsonFormUtils.getValue(jsonObject, "hiv_status");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected boolean hasAssessmentInput() {
        // These testing paths can inform assessment without a reportable result.
        return StringUtils.isNotBlank(hiv_status) || "no".equals(testedHiv)
                || ("yes".equals(testedHiv) && ("hivst".equals(testingMethod) || "not_tested".equals(testingMethod)));
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
            if (savingSuggestion) {
                MarpHtsSuggestion.removeFromPayload(form, hiv_status);
            }
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
