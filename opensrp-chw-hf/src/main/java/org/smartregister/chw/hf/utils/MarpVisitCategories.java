package org.smartregister.chw.hf.utils;

import static com.vijay.jsonwizard.constants.JsonFormConstants.TEXT;
import static org.smartregister.AllConstants.OPTIONS;
import static org.smartregister.util.JsonFormUtils.KEY;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.kvp.util.Constants;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class MarpVisitCategories {
    public static final String NONE = "none";

    private MarpVisitCategories() {
    }

    public static void configure(JSONObject field, Context context, String dominantGroup, String gender, int age)
            throws JSONException {
        field.put("label", context.getString(R.string.marp_other_client_category));
        field.getJSONObject("v_required").put("err", context.getString(R.string.marp_other_category_required));
        JSONArray allOptions = field.getJSONArray(OPTIONS);
        JSONArray eligibleOptions = new JSONArray();

        for (int i = 0; i < allOptions.length(); i++) {
            JSONObject option = allOptions.getJSONObject(i);
            String code = option.getString(KEY);
            int label = labelResource(code);
            if (label != 0 && isEligible(code, dominantGroup, gender, age)) {
                option.put(TEXT, context.getString(label));
                eligibleOptions.put(option);
            }
        }
        field.put(OPTIONS, eligibleOptions);
    }

    private static boolean isEligible(String code, String dominantGroup, String gender, int age) {
        if (NONE.equals(code)) {
            return true;
        }
        if (code.equalsIgnoreCase(dominantGroup)) {
            return false;
        }
        switch (code) {
            case "msm":
                return Constants.MALE.equalsIgnoreCase(gender);
            case "fsw":
                return Constants.FEMALE.equalsIgnoreCase(gender);
            case MarpAgywScreening.GROUP_CODE:
                return Constants.FEMALE.equalsIgnoreCase(gender) && age >= 15 && age <= 24;
            default:
                return true;
        }
    }

    private static int labelResource(String code) {
        switch (code) {
            case "pwud":
                return R.string.marp_pwud_enrollment_option;
            case "pwid":
                return R.string.marp_pwid_enrollment_option;
            case "msm":
                return R.string.marp_mhr_enrollment_option;
            case "fsw":
                return R.string.marp_whr_enrollment_option;
            case MarpAgywScreening.GROUP_CODE:
                return R.string.marp_agyw_option;
            case MarpDiscordantCoupleScreening.GROUP_CODE:
                return R.string.marp_discordant_partner_option;
            case NONE:
                return R.string.marp_other_category_none;
            default:
                MarpOtherGroupsScreening.Group group = MarpOtherGroupsScreening.Group.fromCode(code);
                return group == null ? 0 : group.label;
        }
    }

    public static Set<String> codes(String stored) {
        Set<String> codes = new LinkedHashSet<>();
        String list = StringUtils.strip(StringUtils.trimToEmpty(stored), "[]");
        for (String item : list.split(",")) {
            String code = StringUtils.strip(item.trim(), "\"").toLowerCase(Locale.ROOT);
            if (!code.isEmpty()) {
                codes.add(code);
            }
        }
        return codes;
    }

    public static void applyScreeningDefaults(JSONObject field, String screenedGroups) throws JSONException {
        Set<String> defaults = codes(screenedGroups);
        if (defaults.isEmpty()) {
            defaults.add(NONE);
        } else if (defaults.size() > 1) {
            defaults.remove(NONE);
        }
        setSelection(field, defaults);
    }

    public static void setSelection(JSONObject field, Set<String> selection) throws JSONException {
        JSONArray options = field.getJSONArray(OPTIONS);
        JSONArray selected = new JSONArray();
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            String code = option.getString(KEY);
            boolean checked = selection.contains(code);
            option.put(VALUE, checked);
            if (checked) {
                selected.put(code);
            }
        }
        field.put(VALUE, selected);
    }
}
