package org.smartregister.chw.hf.actionhelper.kvp;

import static org.smartregister.chw.hf.utils.MarpPep.NEED;
import static org.smartregister.chw.hf.utils.MarpPep.NO;
import static org.smartregister.chw.hf.utils.MarpPep.NOT_PROVIDED;
import static org.smartregister.chw.hf.utils.MarpPep.PROVIDED;
import static org.smartregister.chw.hf.utils.MarpPep.SERVICE;
import static org.smartregister.chw.hf.utils.MarpPep.YES;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.MarpPep;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.util.JsonFormUtils;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class KvpPrepPepActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    private String legacyService;
    private Context context;
    private JSONObject form;
    private boolean complete;
    private boolean reviewNeeded;

    public void setReviewNeeded(boolean reviewNeeded, BaseKvpVisitAction action) {
        this.reviewNeeded = reviewNeeded;
        action.setSubTitle(reviewNeeded ? context.getString(R.string.marp_pep_review_needed) : null);
        action.evaluateStatus();
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        this.context = context;
        if (StringUtils.isBlank(MarpPep.answer(details, NEED))) {
            legacyService = MarpPep.answer(details, SERVICE);
        }
        try {
            form = new JSONObject(jsonPayload);
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    @Override
    public String getPreProcessed() {
        if (form == null) {
            return null;
        }
        try {
            JSONObject step = form.getJSONObject("step1");
            step.put("title", context.getString(R.string.kvp_pep_assesment));
            JSONArray fields = step.getJSONArray("fields");
            localizeRadio(fields, NEED, R.string.kvp_pep_required, YES, R.string.marp_pep_yes,
                    NO, R.string.marp_pep_no);
            localizeRadio(fields, SERVICE, R.string.kvp_pep_provision, PROVIDED, R.string.marp_pep_provided,
                    NOT_PROVIDED, R.string.marp_pep_not_provided);
            return form.toString();
        } catch (JSONException e) {
            Timber.e(e);
            return null;
        }
    }

    private void localizeRadio(JSONArray fields, String key, int label, String firstKey, int firstLabel,
                               String secondKey, int secondLabel) throws JSONException {
        JSONObject field = JsonFormUtils.getFieldJSONObject(fields, key);
        field.put("label", context.getString(label));
        field.getJSONObject("v_required").put("err", context.getString(R.string.marp_pep_answer_required));
        JSONArray options = field.getJSONArray("options");
        JsonFormUtils.getFieldJSONObject(options, firstKey).put("text", context.getString(firstLabel));
        JsonFormUtils.getFieldJSONObject(options, secondKey).put("text", context.getString(secondLabel));
    }

    public JSONObject forDisplay(JSONObject displayForm) throws JSONException {
        if (PROVIDED.equals(legacyService)) {
            JSONArray fields = displayForm.getJSONObject("step1").getJSONArray("fields");
            JsonFormUtils.getFieldJSONObject(fields, NEED).put(VALUE, YES);
            JsonFormUtils.getFieldJSONObject(fields, SERVICE).put(VALUE, PROVIDED);
        }
        return displayForm;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        // Process the action payload below, after the shared action has stored it.
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
    public String postProcess(String payload) {
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseKvpVisitAction.Status evaluateStatusOnPayload() {
        if (reviewNeeded) {
            return BaseKvpVisitAction.Status.PARTIALLY_COMPLETED;
        }
        return complete ? BaseKvpVisitAction.Status.COMPLETED : BaseKvpVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseKvpVisitAction action) {
        try {
            JSONObject payload = new JSONObject(action.getJsonPayload());
            JSONArray fields = payload.getJSONObject("step1").getJSONArray("fields");
            JSONObject needField = JsonFormUtils.getFieldJSONObject(fields, NEED);
            JSONObject serviceField = JsonFormUtils.getFieldJSONObject(fields, SERVICE);
            String need = needField.optString(VALUE);
            String service = serviceField.optString(VALUE);

            // An unchanged historical display must never become an explicit need answer.
            if (PROVIDED.equals(legacyService) && YES.equals(need) && PROVIDED.equals(service)) {
                needField.remove(VALUE);
                need = "";
            }
            boolean answered = NO.equals(need) || (YES.equals(need) && MarpPep.isServiceAnswer(service));
            if (!answered || NO.equals(need)) {
                serviceField.remove(VALUE);
            }
            if (!answered) {
                needField.remove(VALUE);
            } else {
                legacyService = null;
            }
            complete = answered || MarpPep.isServiceAnswer(legacyService);
            action.setProcessedJsonPayload(payload.toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }
}
