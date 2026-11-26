package org.smartregister.chw.hf.utils;

import static com.vijay.jsonwizard.constants.JsonFormConstants.CHECK_BOX;
import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.KEY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPTIONS_FIELD_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.TYPE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;

import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.kvp.util.Constants;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class MarpOtherGroupsScreening {
    public static final String CHECKLIST_FIELD = "other_groups_at_risk";
    private static final String ELIGIBLE_GROUPS_FIELD = "eligible_other_marp_groups";
    private static final String NONE = "none";

    private MarpOtherGroupsScreening() {
    }

    public enum Group {
        TRUCK_DRIVERS("long_distance_truck_drivers", R.string.kvp_long_distance_truck_drivers),
        FISHERFOLK("fisherfolk", R.string.kvp_fisherfolk),
        MINERS("miners", R.string.kvp_miners),
        OTHER("other_key_and_vulnerable_populations", R.string.kvp_other_key_and_vulnerable_populations);

        public final String code;
        public final int label;

        Group(String code, int label) {
            this.code = code;
            this.label = label;
        }

        public static Group fromCode(Object code) {
            for (Group group : values()) {
                if (group.code.equals(code)) {
                    return group;
                }
            }
            return null;
        }
    }

    public static String checklistStep(JSONObject form) {
        return form.has("step9") ? "step7" : "step6";
    }

    public static boolean isScreeningStep(JSONObject form, String step) {
        if (form == null || !Constants.EVENT_TYPE.KVP_REGISTRATION.equalsIgnoreCase(form.optString("encounter_type"))
                || !checklistStep(form).equals(step)) {
            return false;
        }
        JSONObject checklist = field(form, step, CHECKLIST_FIELD);
        return checklist != null && CHECK_BOX.equals(checklist.optString(TYPE));
    }

    public static Set<Group> eligibleGroups(JSONObject form) {
        Set<Group> groups = EnumSet.noneOf(Group.class);
        String step = checklistStep(form);
        if (!isScreeningStep(form, step)) {
            return groups;
        }
        JSONArray options = field(form, step, CHECKLIST_FIELD).optJSONArray(OPTIONS_FIELD_NAME);
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.optJSONObject(i);
            if (!option.optBoolean(VALUE)) {
                continue;
            }
            String code = option.optString(KEY);
            if (NONE.equals(code)) {
                groups.clear();
                return groups;
            }
            Group group = Group.fromCode(code);
            if (group != null && group != Group.OTHER) {
                groups.add(group);
            } else if ("farmers_on_large_farms".equals(code) || "construction_workers".equals(code)
                    || "disabled".equals(code)) {
                groups.add(Group.OTHER);
            }
        }
        return groups;
    }

    public static void refreshEligibility(JsonApi api, String step) throws JSONException {
        JSONObject form = api.getmJSONObject();
        Set<Group> groups = eligibleGroups(form);
        List<String> codes = new ArrayList<>();
        for (Group group : groups) {
            codes.add(group.code);
        }
        // Native Forms keeps the previous value when an empty string is written to a hidden field.
        api.writeValue(step, ELIGIBLE_GROUPS_FIELD, codes.isEmpty() ? NONE : String.join(",", codes),
                "", "", "", false);

        String enrollmentStep = "step7".equals(step) ? "step9" : "step8";
        JSONObject dominant = field(form, enrollmentStep, "client_group");
        Group selected = Group.fromCode(Utils.getValueAfterTranslation(dominant));
        if (selected != null && !groups.contains(selected)) {
            JsonFormUtils.clearFormValue(api, enrollmentStep, dominant);
        }
    }

    private static JSONObject field(JSONObject form, String step, String key) {
        JSONObject page = form.optJSONObject(step);
        return page == null ? null : JsonFormUtils.getFieldJSONObject(page.optJSONArray(FIELDS), key);
    }
}
