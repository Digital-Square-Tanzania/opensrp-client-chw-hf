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

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class KvpHealthEducationHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    private static final String TOPICS = "health_education_provided";
    private static final String OTHER_TEXT = "others_health_education_provided";

    private boolean complete;
    private boolean otherSelected;

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        complete = false;
        otherSelected = false;
        if (StringUtils.isBlank(jsonPayload)) {
            return;
        }

        try {
            JSONObject form = new JSONObject(jsonPayload);
            JSONObject topics = FormUtils.getFieldFromForm(form, TOPICS);
            if (topics == null) {
                return;
            }
            JSONArray options = topics.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
            int selectedCount = 0;
            boolean noneSelected = false;
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);
                if (option.optBoolean(JsonFormConstants.VALUE)) {
                    selectedCount++;
                    String key = option.getString(JsonFormConstants.KEY);
                    noneSelected |= "none".equals(key);
                    otherSelected |= "other".equals(key);
                }
            }

            complete = selectedCount > 0 && (!noneSelected || selectedCount == 1);
            if (complete && otherSelected) {
                JSONObject otherText = FormUtils.getFieldFromForm(form, OTHER_TEXT);
                complete = otherText != null && StringUtils.isNotBlank(otherText.optString(JsonFormConstants.VALUE));
            }
        } catch (JSONException e) {
            Timber.e(e);
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
    public String postProcess(String jsonPayload) {
        if (!otherSelected && StringUtils.isNotBlank(jsonPayload)) {
            try {
                JSONObject form = new JSONObject(jsonPayload);
                JSONObject otherText = FormUtils.getFieldFromForm(form, OTHER_TEXT);
                if (otherText != null && otherText.remove(JsonFormConstants.VALUE) != null) {
                    return form.toString();
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
        return null;
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

