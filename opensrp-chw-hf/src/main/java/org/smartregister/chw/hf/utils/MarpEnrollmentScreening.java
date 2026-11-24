package org.smartregister.chw.hf.utils;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.KEY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPENMRS_ENTITY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPENMRS_ENTITY_ID;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPENMRS_ENTITY_PARENT;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPTIONS_FIELD_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.TEXT;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.util.JsonFormUtils.getFieldJSONObject;

import android.content.Context;

import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.kvp.util.Constants;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MarpEnrollmentScreening {
    private static final String MALE_ENROLLMENT_STEP = "step8";
    private static final String CLIENT_GROUP = "client_group";
    private static final String BIRTH_REGION = "birth_region";
    private static final String SHOULD_ENROLL = "should_enroll";
    private static final String SECONDARY_GROUPS = "other_screened_client_group";

    private MarpEnrollmentScreening() {
    }

    public static boolean isEnrollmentStep(JSONObject form, String step) {
        return form != null && Constants.EVENT_TYPE.KVP_REGISTRATION.equalsIgnoreCase(form.optString("encounter_type"))
                && (form.has(MarpAgywScreening.ENROLLMENT_STEP)
                ? MarpAgywScreening.ENROLLMENT_STEP.equals(step) : MALE_ENROLLMENT_STEP.equals(step));
    }

    public static void prepareStep(JsonApi api, JSONObject form, String stepName, JSONObject step, Context context)
            throws JSONException {
        Map<String, Integer> eligible = eligibleGroups(form);
        JSONObject group = field(step, CLIENT_GROUP);
        JSONArray options = new JSONArray();
        for (Map.Entry<String, Integer> entry : eligible.entrySet()) {
            String code = entry.getKey();
            options.put(new JSONObject().put(KEY, code).put(TEXT, context.getString(entry.getValue()))
                    .put(OPENMRS_ENTITY_PARENT, "").put(OPENMRS_ENTITY, "concept").put(OPENMRS_ENTITY_ID, code));
        }
        group.put(OPTIONS_FIELD_NAME, options);
        if (!eligible.containsKey(Utils.getValueAfterTranslation(group))) {
            JsonFormUtils.clearFormValue(api, stepName, group);
            if (eligible.size() == 1) {
                group.put(VALUE, options.getJSONObject(0).getString(KEY));
            }
        }

        // Relevance reads this value when the enrollment step is rendered.
        JSONObject outcome = field(step, SHOULD_ENROLL);
        outcome.remove("calculation");
        outcome.put(VALUE, eligible.isEmpty() ? "no" : "yes");
        // A restored form snapshot can still include Screening 11's secondary-group rule.
        field(step, SECONDARY_GROUPS).remove("calculation");
    }

    public static boolean prepareEnrollmentResult(JSONObject form, String stepName) throws JSONException {
        Map<String, Integer> eligible = eligibleGroups(form);
        JSONObject step = form.getJSONObject(stepName);
        JSONObject group = field(step, CLIENT_GROUP);
        JSONObject region = field(step, BIRTH_REGION);
        String dominant = Utils.getValueAfterTranslation(group);
        if (eligible.isEmpty()) {
            group.remove(VALUE);
            region.remove(VALUE);
        } else if (!eligible.containsKey(dominant) || Utils.getValueAfterTranslation(region).isEmpty()) {
            return false;
        }

        field(step, SHOULD_ENROLL).put(VALUE, eligible.isEmpty() ? "no" : "yes");
        eligible.remove(dominant);
        // Preserve the bracketed code list consumed by the existing MARP enrollment reader.
        field(step, SECONDARY_GROUPS).put(VALUE, "[" + String.join(",", eligible.keySet()) + "]");
        return true;
    }

    private static Map<String, Integer> eligibleGroups(JSONObject form) {
        Map<String, Integer> eligible = new LinkedHashMap<>();
        boolean female = form.has(MarpAgywScreening.ENROLLMENT_STEP);
        boolean usedDrugs = "yes".equals(answer(form, "step1", "drug_use"));
        String injecting = answer(form, "step2", "drug_injection");
        boolean hadSex = "yes".equals(answer(form, "step3", "sexual_intercourse"));
        String partner = answer(form, "step5", "sexual_intercourse_with");

        if (MarpDiscordantCoupleScreening.isEligible(form)) {
            eligible.put(MarpDiscordantCoupleScreening.GROUP_CODE, R.string.marp_discordant_partner_option);
        }
        // The injection question applies only after a Yes to recent drug use.
        if (usedDrugs && "yes".equals(injecting)) {
            eligible.put("pwid", R.string.marp_pwid_enrollment_option);
        }
        if (!female && hadSex && ("man".equals(partner) || "both".equals(partner))) {
            eligible.put("msm", R.string.marp_mhr_enrollment_option);
        }
        if (female && hadSex && MarpAgywScreening.qualifiesForWhr(form)) {
            eligible.put("fsw", R.string.marp_whr_enrollment_option);
        }
        if (female && MarpAgywScreening.isEligible(form)) {
            eligible.put(MarpAgywScreening.GROUP_CODE, R.string.marp_agyw_option);
        }
        if (usedDrugs && "no".equals(injecting)) {
            eligible.put("pwud", R.string.marp_pwud_enrollment_option);
        }
        for (MarpOtherGroupsScreening.Group group : MarpOtherGroupsScreening.eligibleGroups(form)) {
            eligible.put(group.code, group.label);
        }
        return eligible;
    }

    private static String answer(JSONObject form, String step, String key) {
        JSONObject page = form.optJSONObject(step);
        return page == null ? "" : Utils.getValueAfterTranslation(field(page, key));
    }

    private static JSONObject field(JSONObject step, String key) {
        return getFieldJSONObject(step.optJSONArray(FIELDS), key);
    }
}
