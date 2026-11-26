package org.smartregister.chw.hf.actionhelper.kvp;

import android.content.Context;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class KvpHealthEducationHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    private static final String TOPICS = "health_education_provided";
    private static final String OTHER_TEXT = "others_health_education_provided";

    private static final String DELIVERY_MODES = "health_education_delivery_modes_";
    private static final String DELIVERY_OTHER = "health_education_delivery_other_";
    private static final String OTHER = "other";
    private static final String NONE = "none";

    private boolean complete;
    private boolean restoringLegacy;
    private String processedPayload;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        // KVP evaluates the restored payload before the provider opens the form.
        restoringLegacy = details != null && details.containsKey(TOPICS)
                && details.keySet().stream().noneMatch(key -> key.startsWith(DELIVERY_MODES) || key.startsWith(DELIVERY_OTHER));
    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        complete = false;
        processedPayload = null;
        boolean restoredLegacy = restoringLegacy;
        restoringLegacy = false;
        if (StringUtils.isBlank(jsonPayload)) {
            return;
        }

        try {
            JSONObject form = new JSONObject(jsonPayload);
            JSONObject topics = FormUtils.getFieldFromForm(form, TOPICS);
            JSONArray topicOptions = topics.optJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
            if (topicOptions == null) {
                return;
            }

            Set<String> selectedTopics = selectedOptions(topicOptions);
            boolean noneSelected = selectedTopics.contains(NONE);
            boolean valid = !selectedTopics.isEmpty() && (!noneSelected || selectedTopics.size() == 1);
            JSONObject topicText = FormUtils.getFieldFromForm(form, OTHER_TEXT);
            valid &= validateText(topicText, !noneSelected && selectedTopics.contains(OTHER));

            for (int i = 0; i < topicOptions.length(); i++) {
                String topic = topicOptions.getJSONObject(i).getString(JsonFormConstants.KEY);
                if (NONE.equals(topic)) {
                    continue;
                }

                JSONObject modes = FormUtils.getFieldFromForm(form, DELIVERY_MODES + topic);
                boolean topicSelected = !noneSelected && selectedTopics.contains(topic);
                JSONArray modeOptions = modes.optJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
                if (!topicSelected && modeOptions != null) {
                    clearChecklist(modes, modeOptions);
                }
                Set<String> selectedModes = selectedOptions(modeOptions);
                JSONObject channelText = FormUtils.getFieldFromForm(form, DELIVERY_OTHER + topic);
                valid &= validateText(channelText, topicSelected && selectedModes.contains(OTHER));
                if (topicSelected && !restoredLegacy) {
                    valid &= !selectedModes.isEmpty();
                }
            }

            processedPayload = form.toString();
            complete = valid;
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    private static Set<String> selectedOptions(JSONArray options) throws JSONException {
        Set<String> selected = new HashSet<>();
        if (options != null) {
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);
                if (option.optBoolean(JsonFormConstants.VALUE)) {
                    selected.add(option.getString(JsonFormConstants.KEY));
                }
            }
        }
        return selected;
    }

    private static void clearChecklist(JSONObject checklist, JSONArray options) throws JSONException {
        checklist.remove(JsonFormConstants.VALUE);
        for (int i = 0; i < options.length(); i++) {
            options.getJSONObject(i).remove(JsonFormConstants.VALUE);
        }
    }

    private static boolean validateText(JSONObject field, boolean required) {
        if (required) {
            return field != null && !field.isNull(JsonFormConstants.VALUE)
                    && StringUtils.isNotBlank(field.optString(JsonFormConstants.VALUE));
        }
        if (field != null) {
            field.remove(JsonFormConstants.VALUE);
        }
        return true;
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
    public String postProcess(String jsonPayload) {
        return processedPayload;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseKvpVisitAction.Status evaluateStatusOnPayload() {
        return complete ? BaseKvpVisitAction.Status.COMPLETED : BaseKvpVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseKvpVisitAction baseKvpVisitAction) {
        //overridden
    }
}

