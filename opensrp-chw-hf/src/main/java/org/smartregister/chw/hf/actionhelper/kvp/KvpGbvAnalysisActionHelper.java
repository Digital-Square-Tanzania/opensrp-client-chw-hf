package org.smartregister.chw.hf.actionhelper.kvp;

import android.content.Context;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.utils.MarpPep;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class KvpGbvAnalysisActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    private static final String SCREENING = "gbv_screening";
    private static final String POST_GBV_CARE = "post_gbv_care_provision";

    private boolean screeningComplete;
    private boolean violenceReported;
    private boolean sexualViolence;

    public boolean hasSexualViolence() {
        return sexualViolence;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
    }

    @Override
    public String getPreProcessed() {
        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        screeningComplete = false;
        violenceReported = false;
        sexualViolence = false;
        if (StringUtils.isBlank(jsonPayload)) {
            return;
        }
        try {
            JSONObject screening = FormUtils.getFieldFromForm(new JSONObject(jsonPayload), SCREENING);
            if (screening == null) {
                return;
            }
            JSONArray options = screening.getJSONArray(JsonFormConstants.OPTIONS_FIELD_NAME);
            Set<String> selected = new HashSet<>();
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);
                if (option.optBoolean(JsonFormConstants.VALUE)) {
                    selected.add(option.getString(JsonFormConstants.KEY));
                }
            }
            boolean exclusive = selected.contains("none") || selected.contains("not_assessed");
            screeningComplete = exclusive ? selected.size() == 1 : !selected.isEmpty();
            violenceReported = screeningComplete && !exclusive;
            sexualViolence = violenceReported && selected.contains(MarpPep.SEXUAL_VIOLENCE);
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
        if (screeningComplete && !violenceReported) {
            try {
                JSONObject form = new JSONObject(jsonPayload);
                JSONObject care = FormUtils.getFieldFromForm(form, POST_GBV_CARE);
                if (care != null && care.remove(JsonFormConstants.VALUE) != null) {
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
        return screeningComplete ? BaseKvpVisitAction.Status.COMPLETED : BaseKvpVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseKvpVisitAction baseKvpVisitAction) {
        //overridden
    }
}
