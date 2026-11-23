package org.smartregister.chw.hf.activity;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.KEY;
import static com.vijay.jsonwizard.constants.JsonFormConstants.OPTIONS_FIELD_NAME;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.chw.hf.utils.JsonFormUtils.getFieldJSONObject;

import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.fragment.KvpScreeningJsonFormFragment;
import org.smartregister.chw.hf.utils.JsonFormUtils;
import org.smartregister.chw.hf.utils.MarpAgywScreening;
import org.smartregister.chw.hf.utils.MarpDiscordantCoupleScreening;
import org.smartregister.chw.hf.utils.MarpOtherGroupsScreening;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.family.activity.FamilyWizardFormActivity;

import java.util.Arrays;
import java.util.List;

public class KvpJsonWizardFormActivity extends FamilyWizardFormActivity {
    private static final List<String> RETIRED_GROUPS = Arrays.asList(
            "prisoner", "rumandee", "mobile_population", "other_vulnerable_population");
    private static final List<String> RETIRED_QUESTIONS = Arrays.asList(
            "prisoner_or_rumandee", "is_prisoner", "is_rumandee", "mobile_population", "is_mobile_population",
            "is_ovp_kvp");
    private static final List<String> RETIRED_PROMPTS = Arrays.asList(
            "prompt_for_prisoners_kvp", "prompt_for_rumandee_kvp", "prompt_for_mobile_population_kvp");

    @Override
    public void setmJSONObject(JSONObject form) {
        if (form != null && Constants.EVENT_TYPE.KVP_REGISTRATION.equalsIgnoreCase(form.optString("encounter_type"))) {
            discardRetiredScreeningGroups(form);
        }
        super.setmJSONObject(form);
    }

    // Remove answers from older form snapshots before Native Forms indexes fields or restores the UI.
    private static void discardRetiredScreeningGroups(JSONObject form) {
        boolean female = form.has("step9");
        JSONObject otherGroups = form.optJSONObject(female ? "step7" : "step6");
        JSONObject enrollment = form.optJSONObject(female ? "step9" : "step8");
        if (otherGroups == null || enrollment == null) {
            return;
        }
        JSONArray fields = enrollment.optJSONArray(FIELDS);
        JSONObject group = getFieldJSONObject(fields, "client_group");
        if (group == null) {
            return;
        }

        boolean hadRetiredQuestions = removeEntries(otherGroups.optJSONArray(FIELDS), RETIRED_QUESTIONS);
        removeEntries(fields, RETIRED_PROMPTS);
        removeEntries(group.optJSONArray(OPTIONS_FIELD_NAME), RETIRED_GROUPS);
        boolean retiredSelection = RETIRED_GROUPS.contains(Utils.getValueAfterTranslation(group));
        if (retiredSelection) {
            group.remove(VALUE);
        }
        JSONObject secondaryGroups = getFieldJSONObject(fields, "other_screened_client_group");
        String secondary = secondaryGroups == null ? "" : secondaryGroups.optString(VALUE);
        if (hadRetiredQuestions || retiredSelection || RETIRED_GROUPS.stream().anyMatch(secondary::contains)) {
            // Recalculate the outcome and secondary groups from the current screening rules.
            JSONObject shouldEnroll = getFieldJSONObject(fields, "should_enroll");
            if (shouldEnroll != null) {
                shouldEnroll.remove(VALUE);
            }
            if (secondaryGroups != null) {
                secondaryGroups.remove(VALUE);
            }
        }
    }

    private static boolean removeEntries(JSONArray entries, List<String> keys) {
        if (entries == null) {
            return false;
        }
        int previousSize = entries.length();
        for (int i = previousSize - 1; i >= 0; i--) {
            JSONObject entry = entries.optJSONObject(i);
            if (entry != null && keys.contains(entry.optString(KEY))) {
                entries.remove(i);
            }
        }
        return entries.length() < previousSize;
    }

    @Override
    public synchronized void initializeFormFragment() {
        String step = MarpPregnancyScreening.firstStep(getmJSONObject());
        KvpScreeningJsonFormFragment formFragment = KvpScreeningJsonFormFragment.getFormFragment(step);
        getSupportFragmentManager().beginTransaction()
                .add(com.vijay.jsonwizard.R.id.container, formFragment).commitAllowingStateLoss();
    }

    @Override
    protected void widgetsWriteValue(String step, String key, String value, String entityParent,
                                     String entity, String entityId, boolean popup) throws JSONException {
        JSONObject form = getmJSONObject();
        if (!popup && MarpPregnancyScreening.isIntakeStep(form, step)) {
            String previousRoute = MarpPregnancyScreening.nextStep(form);
            super.widgetsWriteValue(step, key, value, entityParent, entity, entityId, false);
            MarpPregnancyScreening.discardBranchAnswers(this, previousRoute, MarpPregnancyScreening.nextStep(form));
        } else {
            super.widgetsWriteValue(step, key, value, entityParent, entity, entityId, popup);
            if (!popup && !value.isEmpty()) {
                MarpDiscordantCoupleScreening.onRadioAnswerChanged(this, step, key);
                MarpAgywScreening.onRadioAnswerChanged(this, step, key);
            }
            if (!popup && form != null && "step1".equals(step) && "drug_use".equals(key) && "no".equals(value)
                    && Constants.EVENT_TYPE.KVP_REGISTRATION.equalsIgnoreCase(form.optString("encounter_type"))) {
                JSONObject injectionStep = form.optJSONObject("step2");
                if (injectionStep != null) {
                    JsonFormUtils.clearFormValue(this, "step2", JsonFormUtils.getFieldJSONObject(
                            injectionStep.optJSONArray("fields"), "drug_injection"));
                    JsonFormUtils.clearFormValue(this, "step2", JsonFormUtils.getFieldJSONObject(
                            injectionStep.optJSONArray("fields"), "is_pwid"));
                }
            }
        }
    }

    @Override
    protected void checkBoxWriteValue(String step, String parentKey, String childObjectKey,
                                      String childKey, String value, boolean popup) throws JSONException {
        super.checkBoxWriteValue(step, parentKey, childObjectKey, childKey, value, popup);
        JSONObject form = getmJSONObject();
        if (!popup && MarpDiscordantCoupleScreening.PARTNER_PARAMETERS.equals(parentKey)
                && MarpDiscordantCoupleScreening.isScreeningStep(form, step)) {
            MarpDiscordantCoupleScreening.refreshEligibility(this);
        }
        if (!popup && MarpAgywScreening.CHECKLIST_FIELD.equals(parentKey)
                && MarpAgywScreening.isScreeningStep(form, step)) {
            MarpAgywScreening.refreshEligibility(this);
        }
        if (!popup && MarpOtherGroupsScreening.CHECKLIST_FIELD.equals(parentKey)
                && MarpOtherGroupsScreening.isScreeningStep(form, step)) {
            MarpOtherGroupsScreening.refreshEligibility(this, step);
        }
        if (!popup && MarpPregnancyScreening.HIGH_RISK_STEP.equals(step)
                && MarpPregnancyScreening.CHECKLIST_FIELD.equals(parentKey)
                && MarpPregnancyScreening.HIGH_RISK_STEP.equals(MarpPregnancyScreening.nextStep(form))
                && !MarpPregnancyScreening.ENROLLMENT_STEP.equals(MarpPregnancyScreening.highRiskOutcome(form))) {
            MarpPregnancyScreening.discardBranchAnswers(this, MarpPregnancyScreening.ENROLLMENT_STEP, "");
        }
    }

    @Override
    protected void toggleViewVisibility(View view, boolean visible, boolean popup) {
        super.toggleViewVisibility(view, visible, popup);
        if (visible && !popup && view instanceof ViewGroup && "client_group".equals(view.getTag(R.id.key))) {
            selectSoleEnrollmentOption((ViewGroup) view);
        }
    }

    static void selectSoleEnrollmentOption(ViewGroup group) {
        List<RadioButton> options = Utils.getRadioButtons(group);
        if (options.size() == 1) {
            RadioButton option = options.get(0);
            Object key = option.getTag(R.id.childKey);
            // Use the widget after relevance makes it available so Save validates the checked radio.
            if (option.isEnabled() && !option.isChecked()
                    && ("msm".equals(key) || "fsw".equals(key)
                    || MarpOtherGroupsScreening.Group.fromCode(key) != null)) {
                option.setChecked(true);
            }
        }
    }
}
