package org.smartregister.chw.hf.utils;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.KEY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPTIONS_FIELD_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.chw.hf.utils.JsonFormUtils.clearFormAnswer;
import static org.smartregister.util.JsonFormUtils.getFieldJSONObject;

import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.util.Constants;

public final class MarpAgywScreening {
    public static final String STEP = "step6";
    public static final String ENROLLMENT_STEP = "step9";
    public static final String CHECKLIST_FIELD = "harsh_condition_impact";
    public static final String SCHOOL_FIELD = "client_in_school";
    public static final String GROUP_CODE = "agyw";

    private static final String ELIGIBILITY = "is_agyw";
    private static final String NONE = "not_applicable";
    private static final String LEGACY_RECENT_SEX = "sex_last_three_months";

    private MarpAgywScreening() {
    }

    public static boolean isScreeningStep(JSONObject form, String step) {
        return STEP.equals(step) && form != null
                && Constants.EVENT_TYPE.KVP_REGISTRATION.equalsIgnoreCase(form.optString("encounter_type"))
                && field(form, STEP, CHECKLIST_FIELD) != null;
    }

    public static void onRadioAnswerChanged(JsonApi api, String step, String key) throws JSONException {
        JSONObject form = api.getmJSONObject();
        boolean changedSchool = STEP.equals(step) && SCHOOL_FIELD.equals(key);
        boolean changedWhr = "step5".equals(step)
                && ("sex_for_money".equals(key) || "main_income_source_is_sex".equals(key));
        if (!(changedSchool || changedWhr) || !isScreeningStep(form, STEP)) {
            return;
        }

        JSONObject school = field(form, STEP, SCHOOL_FIELD);
        if (changedWhr && qualifiesForWhr(form)) {
            clearFormAnswer(api, STEP, school);
        }
        if (!"no".equals(Utils.getValueAfterTranslation(school))) {
            clearFormAnswer(api, STEP, field(form, STEP, CHECKLIST_FIELD));
        }
        refreshEligibility(api);
    }

    public static boolean isEligible(JSONObject form) {
        if (!isScreeningStep(form, STEP)) {
            return false;
        }
        int age = age(form);
        if (age < 15 || age > 24 || qualifiesForWhr(form)
                || !"yes".equals(answer(form, "step3", "sexual_intercourse"))
                || !"no".equals(answer(form, STEP, SCHOOL_FIELD))
                || MarpPregnancyScreening.HIGH_RISK_STEP.equals(MarpPregnancyScreening.nextStep(form))) {
            return false;
        }

        JSONArray options = field(form, STEP, CHECKLIST_FIELD).optJSONArray(OPTIONS_FIELD_NAME);
        boolean qualifies = false;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option == null || !option.optBoolean(VALUE)) {
                continue;
            }
            String key = option.optString(KEY);
            if (NONE.equals(key)) {
                return false;
            }
            // The older answer records any recent sex and cannot stand for unprotected sex.
            if (!LEGACY_RECENT_SEX.equals(key)) {
                qualifies = true;
            }
        }
        return qualifies;
    }

    public static boolean refreshEligibility(JsonApi api) throws JSONException {
        JSONObject form = api.getmJSONObject();
        if (!isScreeningStep(form, STEP)) {
            return false;
        }
        boolean eligible = isEligible(form);
        String value = eligible ? "yes" : "no";
        if (!value.equals(answer(form, STEP, ELIGIBILITY))) {
            api.writeValue(STEP, ELIGIBILITY, value, "", "concept", ELIGIBILITY, false);
        }
        if (!eligible) {
            JSONObject group = field(form, ENROLLMENT_STEP, "client_group");
            if (GROUP_CODE.equals(Utils.getValueAfterTranslation(group))) {
                clearFormAnswer(api, ENROLLMENT_STEP, group);
            }
        }
        return eligible;
    }

    static boolean qualifiesForWhr(JSONObject form) {
        // Use the inputs here because Native Forms publishes is_fsw asynchronously.
        return age(form) >= 18 && "yes".equals(answer(form, "step5", "sex_for_money"))
                && "yes".equals(answer(form, "step5", "main_income_source_is_sex"));
    }

    private static int age(JSONObject form) {
        JSONObject global = form.optJSONObject("global");
        return global == null ? 0 : global.optInt("age");
    }

    private static String answer(JSONObject form, String step, String key) {
        return Utils.getValueAfterTranslation(field(form, step, key));
    }

    private static JSONObject field(JSONObject form, String step, String key) {
        JSONObject page = form.optJSONObject(step);
        return page == null ? null : getFieldJSONObject(page.optJSONArray(FIELDS), key);
    }
}
