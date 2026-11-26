package org.smartregister.chw.hf.utils;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.KEY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPTIONS_FIELD_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.chw.hf.utils.JsonFormUtils.clearFormAnswer;

import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.util.JsonFormUtils;

public final class MarpDiscordantCoupleScreening {
    public static final String STEP = "step4";
    public static final String PARTNER_PARAMETERS = "partner_hiv_care_status";
    public static final String GROUP_CODE = "serodiscordant_couple";

    private static final String SEXUAL_BEHAVIOUR_STEP = "step3";
    private static final String SEXUAL_INTERCOURSE = "sexual_intercourse";
    private static final String KNOWS_STATUS = "client_knows_hiv_status";
    private static final String HIV_STATUS = "hiv_status_on_screening";
    private static final String POSITIVE_PARTNER = "hiv_positive_partner";
    private static final String ELIGIBILITY = "is_sdc";

    private MarpDiscordantCoupleScreening() {
    }

    public static boolean isScreeningStep(JSONObject form, String step) {
        return STEP.equals(step) && form != null
                && Constants.EVENT_TYPE.KVP_REGISTRATION.equalsIgnoreCase(form.optString("encounter_type"))
                && field(form, STEP, KNOWS_STATUS) != null;
    }

    public static void onRadioAnswerChanged(JsonApi api, String step, String key) throws JSONException {
        JSONObject form = api.getmJSONObject();
        boolean sexualBehaviourChanged = SEXUAL_BEHAVIOUR_STEP.equals(step) && SEXUAL_INTERCOURSE.equals(key);
        boolean discordantAnswerChanged = STEP.equals(step)
                && (KNOWS_STATUS.equals(key) || HIV_STATUS.equals(key) || POSITIVE_PARTNER.equals(key));
        if (!(sexualBehaviourChanged || discordantAnswerChanged) || !isScreeningStep(form, STEP)) {
            return;
        }

        JSONObject knows = field(form, STEP, KNOWS_STATUS);
        JSONObject status = field(form, STEP, HIV_STATUS);
        JSONObject partner = field(form, STEP, POSITIVE_PARTNER);
        boolean sexuallyActive = "yes".equals(answer(form, SEXUAL_BEHAVIOUR_STEP, SEXUAL_INTERCOURSE));
        if (!sexuallyActive) {
            clearFormAnswer(api, STEP, knows);
        }
        if (!sexuallyActive || !"yes".equals(Utils.getValueAfterTranslation(knows))) {
            clearFormAnswer(api, STEP, status);
        }
        if (!"negative".equals(Utils.getValueAfterTranslation(status))) {
            clearFormAnswer(api, STEP, partner);
        }
        if (!"yes".equals(Utils.getValueAfterTranslation(partner))) {
            clearFormAnswer(api, STEP, field(form, STEP, PARTNER_PARAMETERS));
        }
        refreshEligibility(api);
    }

    public static boolean isEligible(JSONObject form) {
        if (!"yes".equals(answer(form, SEXUAL_BEHAVIOUR_STEP, SEXUAL_INTERCOURSE))
                || !"yes".equals(answer(form, STEP, KNOWS_STATUS))
                || !"negative".equals(answer(form, STEP, HIV_STATUS))
                || !"yes".equals(answer(form, STEP, POSITIVE_PARTNER))) {
            return false;
        }

        JSONArray options = field(form, STEP, PARTNER_PARAMETERS).optJSONArray(OPTIONS_FIELD_NAME);
        boolean qualifies = false;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option.optBoolean(VALUE)) {
                if ("none".equals(option.optString(KEY))) {
                    return false;
                }
                qualifies = true;
            }
        }
        return qualifies;
    }

    public static void refreshEligibility(JsonApi api) throws JSONException {
        JSONObject form = api.getmJSONObject();
        boolean eligible = isEligible(form);
        // Later enrollment rules use the same flag for the group prompt and secondary groups.
        api.writeValue(STEP, ELIGIBILITY, eligible ? "yes" : "no", "", "concept", ELIGIBILITY, false);
        if (!eligible) {
            String enrollmentStep = form.has(MarpPregnancyScreening.INTAKE_STEP) ? "step9" : "step8";
            JSONObject group = field(form, enrollmentStep, "client_group");
            if (GROUP_CODE.equals(Utils.getValueAfterTranslation(group))) {
                clearFormAnswer(api, enrollmentStep, group);
            }
        }
    }

    private static String answer(JSONObject form, String step, String key) {
        return Utils.getValueAfterTranslation(field(form, step, key));
    }

    private static JSONObject field(JSONObject form, String step, String key) {
        JSONObject page = form.optJSONObject(step);
        return page == null ? null : JsonFormUtils.getFieldJSONObject(page.optJSONArray(FIELDS), key);
    }
}
