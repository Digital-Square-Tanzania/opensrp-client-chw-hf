package org.smartregister.chw.hf.utils;

import static com.vijay.jsonwizard.constants.JsonFormConstants.CHECK_BOX;
import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.FIRST_STEP_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.KEY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.NEXT;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPENMRS_ENTITY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPENMRS_ENTITY_ID;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPENMRS_ENTITY_PARENT;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPTIONS_FIELD_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.TYPE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.chw.core.utils.CoreJsonFormUtils.ENCOUNTER_TYPE;

import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.util.JsonFormUtils;

public final class MarpPregnancyScreening {
    public static final String INTAKE_STEP = "step10";
    // Screening 3 adds the destination under this ID and increases the form count.
    public static final String HIGH_RISK_STEP = "step11";
    public static final String PREGNANCY_FIELD = "is_pregnant";
    public static final String BREASTFEEDING_FIELD = "is_breastfeeding";

    private MarpPregnancyScreening() {
    }

    public static String firstStep(JSONObject form) {
        return isIntakeStep(form, INTAKE_STEP) ? INTAKE_STEP : FIRST_STEP_NAME;
    }

    public static boolean isIntakeStep(JSONObject form, String step) {
        return INTAKE_STEP.equals(step) && form != null && form.has(INTAKE_STEP)
                && Constants.EVENT_TYPE.KVP_REGISTRATION.equalsIgnoreCase(form.optString(ENCOUNTER_TYPE));
    }

    public static String nextStep(JSONObject form) {
        JSONObject intake = form.optJSONObject(INTAKE_STEP);
        if (intake == null) {
            return "";
        }
        JSONArray fields = intake.optJSONArray(FIELDS);
        String pregnant = Utils.getValueAfterTranslation(JsonFormUtils.getFieldJSONObject(fields, PREGNANCY_FIELD));
        if ("yes".equals(pregnant)) {
            return HIGH_RISK_STEP;
        }
        if ("no".equals(pregnant)) {
            String breastfeeding = Utils.getValueAfterTranslation(JsonFormUtils.getFieldJSONObject(fields, BREASTFEEDING_FIELD));
            if ("yes".equals(breastfeeding)) {
                return HIGH_RISK_STEP;
            }
            if ("no".equals(breastfeeding)) {
                return intake.optString(NEXT);
            }
        }
        return "";
    }

    public static void discardBranchAnswers(JsonApi api, String previousStep, String nextStep) throws JSONException {
        if (previousStep.equals(nextStep)) {
            return;
        }
        JSONObject form = api.getmJSONObject();
        String stepName = previousStep;
        while (form.has(stepName)) {
            JSONObject step = form.getJSONObject(stepName);
            JSONArray fields = step.getJSONArray(FIELDS);
            for (int i = 0; i < fields.length(); i++) {
                clearAnswer(api, stepName, fields.getJSONObject(i));
            }
            stepName = step.optString(NEXT);
        }
    }

    private static void clearAnswer(JsonApi api, String stepName, JSONObject field) throws JSONException {
        String key = field.getString(KEY);
        if (CHECK_BOX.equals(field.optString(TYPE))) {
            JSONArray options = field.getJSONArray(OPTIONS_FIELD_NAME);
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);
                if (option.optBoolean(VALUE)) {
                    api.writeValue(stepName, key, OPTIONS_FIELD_NAME, option.getString(KEY), "false",
                            option.optString(OPENMRS_ENTITY_PARENT), option.optString(OPENMRS_ENTITY),
                            option.optString(OPENMRS_ENTITY_ID), false);
                    option.remove(VALUE);
                }
            }
        } else if (field.has(VALUE)) {
            // Update the wizard's write cache so a repeated selection will be accepted after returning.
            api.writeValue(stepName, key, "", field.optString(OPENMRS_ENTITY_PARENT),
                    field.optString(OPENMRS_ENTITY), field.optString(OPENMRS_ENTITY_ID), false);
        }
        field.remove(VALUE);
    }
}
