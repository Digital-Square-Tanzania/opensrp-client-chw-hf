package org.smartregister.chw.hf.utils;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static org.smartregister.chw.hf.utils.JsonFormUtils.clearFormAnswer;
import static org.smartregister.util.JsonFormUtils.getFieldJSONObject;

import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class MarpSexualBehaviourScreening {
    private static final String STEP = "step3";
    private static final String INTERCOURSE = "sexual_intercourse";

    private MarpSexualBehaviourScreening() {
    }

    public static String nextStepAfterNo(JsonApi api, String step) throws JSONException {
        JSONObject form = api.getmJSONObject();
        if (!STEP.equals(step) || !MarpDiscordantCoupleScreening.isScreeningStep(form, MarpDiscordantCoupleScreening.STEP)) {
            return "";
        }
        String otherGroups = MarpOtherGroupsScreening.checklistStep(form);
        if (!MarpOtherGroupsScreening.isScreeningStep(form, otherGroups)
                || !"no".equals(Utils.getValueAfterTranslation(
                        getFieldJSONObject(form.getJSONObject(step).getJSONArray(FIELDS), INTERCOURSE)))) {
            return "";
        }

        // Skipped pages are never rendered to clear their answers after backtracking.
        MarpDiscordantCoupleScreening.onRadioAnswerChanged(api, step, INTERCOURSE);
        clearStepAnswers(api, form, "step5");
        boolean female = "step7".equals(otherGroups);
        if (female) {
            clearStepAnswers(api, form, "step6");
        }

        String enrollmentStep = female ? "step9" : "step8";
        JSONObject group = getFieldJSONObject(form.getJSONObject(enrollmentStep).getJSONArray(FIELDS), "client_group");
        String selected = Utils.getValueAfterTranslation(group);
        if ("msm".equals(selected) || "fsw".equals(selected) || "agyw".equals(selected)) {
            clearFormAnswer(api, enrollmentStep, group);
        }
        return otherGroups;
    }

    private static void clearStepAnswers(JsonApi api, JSONObject form, String step) throws JSONException {
        JSONArray fields = form.getJSONObject(step).getJSONArray(FIELDS);
        for (int i = 0; i < fields.length(); i++) {
            clearFormAnswer(api, step, fields.getJSONObject(i));
        }
    }
}
