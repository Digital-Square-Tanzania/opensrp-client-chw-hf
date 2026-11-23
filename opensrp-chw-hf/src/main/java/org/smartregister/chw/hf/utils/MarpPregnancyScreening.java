package org.smartregister.chw.hf.utils;

import static com.vijay.jsonwizard.constants.JsonFormConstants.CHECK_BOX;
import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.FIRST_STEP_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.HIDDEN;
import static com.vijay.jsonwizard.constants.JsonFormConstants.KEY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.NEXT;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPENMRS_ENTITY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPENMRS_ENTITY_ID;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPENMRS_ENTITY_PARENT;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPTIONS_FIELD_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.TYPE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUES;
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
    public static final String HIGH_RISK_STEP = "step11";
    public static final String ENROLLMENT_STEP = "step12";
    public static final String NO_GROUP_STEP = "step13";
    public static final String PREGNANCY_FIELD = "is_pregnant";
    public static final String BREASTFEEDING_FIELD = "is_breastfeeding";
    public static final String CHECKLIST_FIELD = "pregnancy_breastfeeding_risk_criteria";
    public static final String GROUP_CODE = "pregnant_breastfeeding_high_risk";

    private static final String GENERAL_ENROLLMENT_STEP = "step9";
    private static final String BIRTH_REGION = "birth_region";
    private static final String CLIENT_GROUP = "client_group";

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

    public static String highRiskOutcome(JSONObject form) {
        JSONObject highRisk = form.optJSONObject(HIGH_RISK_STEP);
        if (highRisk == null || !HIGH_RISK_STEP.equals(nextStep(form))) {
            return "";
        }
        JSONObject checklist = JsonFormUtils.getFieldJSONObject(highRisk.optJSONArray(FIELDS), CHECKLIST_FIELD);
        JSONArray options = checklist.optJSONArray(OPTIONS_FIELD_NAME);
        boolean criterionSelected = false;
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (option.optBoolean(VALUE)) {
                if ("none".equals(option.optString(KEY))) {
                    return NO_GROUP_STEP;
                }
                criterionSelected = true;
            }
        }
        return criterionSelected ? highRisk.optString(NEXT) : "";
    }

    public static boolean prepareEnrollmentResult(JSONObject form, String step) throws JSONException {
        if (!step.equals(highRiskOutcome(form))) {
            return false;
        }

        boolean enroll = ENROLLMENT_STEP.equals(step);
        JSONArray result = new JSONArray().put(new JSONObject()
                .put(KEY, "should_enroll")
                .put(TYPE, HIDDEN)
                .put(OPENMRS_ENTITY, "concept")
                .put(OPENMRS_ENTITY_ID, "should_enroll")
                .put(VALUE, enroll ? "yes" : "no"));
        if (enroll) {
            JSONArray fields = form.getJSONObject(ENROLLMENT_STEP).getJSONArray(FIELDS);
            JSONObject region = JsonFormUtils.getFieldJSONObject(fields, BIRTH_REGION);
            JSONObject group = JsonFormUtils.getFieldJSONObject(fields, CLIENT_GROUP);
            String regionName = Utils.getValueAfterTranslation(region);
            if (regionName.isEmpty() || !GROUP_CODE.equals(Utils.getValueAfterTranslation(group))) {
                return false;
            }
            JSONObject savedRegion = new JSONObject(region.toString()).put(VALUE, regionName);
            // These region names have no coded choices. Persist the selected name as the observation value.
            savedRegion.remove(VALUES);
            result.put(savedRegion).put(group);
        }

        // The KVP enrollment writer reads numbered steps 1 through 10.
        form.getJSONObject(GENERAL_ENROLLMENT_STEP).put(FIELDS, result);
        JSONArray intakeFields = form.getJSONObject(INTAKE_STEP).getJSONArray(FIELDS);
        if ("yes".equals(Utils.getValueAfterTranslation(JsonFormUtils.getFieldJSONObject(intakeFields, PREGNANCY_FIELD)))) {
            JsonFormUtils.getFieldJSONObject(intakeFields, BREASTFEEDING_FIELD).remove(VALUE);
        }
        return true;
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
