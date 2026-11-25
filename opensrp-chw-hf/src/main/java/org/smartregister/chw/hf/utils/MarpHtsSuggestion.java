package org.smartregister.chw.hf.utils;

import static org.smartregister.util.JsonFormUtils.VALUE;
import static org.smartregister.util.JsonFormUtils.getFieldJSONObject;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.R;

public final class MarpHtsSuggestion {
    private static final String FLAG = "marp_hts_suggestion";
    private static final String REVIEW = "review_marp_hts_suggestion";
    private static final String VERIFY = "verify_marp_hts_suggestion";
    private static final String RESULT = "hiv_status";
    private static final String NEGATIVE = "negative";
    private static final String PREP_PROMPT = "prompt_for_hiv_negative";
    private static final String STANDARD_RULES = "kvp_hts_relevance.yml";
    private static final String SUGGESTION_RULES = "marp_hts_suggestion_relevance.yml";

    private MarpHtsSuggestion() {
    }

    public static boolean isEligible(String dominantGroup, String pregnancyStatus) {
        return MarpPregnancyScreening.GROUP_CODE.equals(dominantGroup)
                || MarpPregnancyStatus.PREGNANT.equals(pregnancyStatus);
    }

    public static JSONObject forDisplay(JSONObject original, Context context) throws JSONException {
        JSONArray originalFields = fields(original);
        if (StringUtils.isNotBlank(answer(originalFields, RESULT))
                || StringUtils.isNotBlank(answer(originalFields, "tested_hiv"))) {
            return original;
        }

        // The Biomedical action keeps the original so Submit cannot map the suggestion.
        JSONObject form = new JSONObject(original.toString());
        form.getJSONObject("global").put(FLAG, "yes");
        JSONObject step = form.getJSONObject("step1");
        JSONArray fields = step.getJSONArray("fields");
        JSONArray displayFields = new JSONArray().put(new JSONObject()
                .put("key", REVIEW)
                .put("type", "toaster_notes")
                .put("text", context.getString(R.string.marp_hts_suggestion_review)));
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            displayFields.put(field);
            if (RESULT.equals(field.optString("key"))) {
                field.put(VALUE, NEGATIVE).put("relevance", rules(SUGGESTION_RULES));
                displayFields.put(verificationField(context));
            } else if (PREP_PROMPT.equals(field.optString("key"))) {
                field.put("relevance", rules(SUGGESTION_RULES));
            }
        }
        step.put("fields", displayFields);
        return form;
    }

    public static boolean isShown(JSONObject form) {
        JSONObject global = form.optJSONObject("global");
        return global != null && "yes".equals(global.optString(FLAG));
    }

    public static String verifiedResult(JSONObject form) throws JSONException {
        JSONArray fields = fields(form);
        String result = answer(fields, RESULT);
        String method = answer(fields, "previous_hiv_testing_method");
        String site = answer(fields, "testing_site");
        String kits = answer(fields, "kits_distributed");
        boolean hasMethod = "hiv_cv_test".equals(method) || "hivst_and_hiv_cv_test".equals(method)
                || ("other_method".equals(method)
                && StringUtils.isNotBlank(answer(fields, "other_previous_hiv_testing_method")));
        boolean hasActualDetails = "yes".equals(answer(fields, "tested_hiv")) && hasMethod
                && ("community".equals(site) || "facility".equals(site))
                && ("yes".equals(kits) || "no".equals(kits));
        if (!hasActualDetails || (NEGATIVE.equals(result) && !"yes".equals(answer(fields, VERIFY)))) {
            return "";
        }
        return result;
    }

    public static void removeFromPayload(JSONObject form, String verifiedResult) throws JSONException {
        form.getJSONObject("global").remove(FLAG);
        JSONArray fields = fields(form);
        JSONObject result = getFieldJSONObject(fields, RESULT);
        result.put("relevance", rules(STANDARD_RULES));
        if (StringUtils.isBlank(verifiedResult)) {
            result.remove(VALUE);
        }
        getFieldJSONObject(fields, PREP_PROMPT).put("relevance", new JSONObject().put("step1:hiv_status",
                new JSONObject().put("type", "string").put("ex", "equalTo(., \"negative\")")));
        for (int i = fields.length() - 1; i >= 0; i--) {
            String key = fields.getJSONObject(i).optString("key");
            if (REVIEW.equals(key) || VERIFY.equals(key)) {
                fields.remove(i);
            }
        }
    }

    private static JSONArray fields(JSONObject form) throws JSONException {
        return form.getJSONObject("step1").getJSONArray("fields");
    }

    private static String answer(JSONArray fields, String key) {
        JSONObject field = getFieldJSONObject(fields, key);
        return field == null ? "" : field.optString(VALUE);
    }

    private static JSONObject rules(String file) throws JSONException {
        return new JSONObject().put("rules-engine",
                new JSONObject().put("ex-rules", new JSONObject().put("rules-file", file)));
    }

    private static JSONObject verificationField(Context context) throws JSONException {
        return new JSONObject()
                .put("key", VERIFY)
                .put("type", "native_radio")
                .put("label", context.getString(R.string.marp_hts_suggestion_verify))
                .put("openmrs_entity", "").put("openmrs_entity_parent", "").put("openmrs_entity_id", "")
                .put("options", new JSONArray()
                        .put(verificationOption("yes", context.getString(R.string.marp_hts_suggestion_verified)))
                        .put(verificationOption("no", context.getString(R.string.marp_hts_suggestion_unverified))))
                .put("v_required", new JSONObject().put(VALUE, true)
                        .put("err", context.getString(R.string.marp_hts_suggestion_verify_required)))
                .put("relevance", rules(SUGGESTION_RULES));
    }

    private static JSONObject verificationOption(String key, String text) throws JSONException {
        return new JSONObject().put("key", key).put("text", text)
                .put("openmrs_entity", "").put("openmrs_entity_parent", "").put("openmrs_entity_id", "");
    }
}
