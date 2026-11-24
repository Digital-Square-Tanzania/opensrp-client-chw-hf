package org.smartregister.chw.hf.presenter;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.widget.LinearLayout;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.interfaces.OnFieldsInvalid;
import com.vijay.jsonwizard.utils.AppExecutors;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.activity.KvpJsonWizardFormActivity;
import org.smartregister.chw.hf.fragment.KvpScreeningJsonFormFragment;
import org.smartregister.chw.hf.utils.MarpAgywScreening;
import org.smartregister.chw.hf.utils.MarpDiscordantCoupleScreening;
import org.smartregister.chw.hf.utils.MarpOtherGroupsScreening;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.smartregister.chw.hf.utils.MarpSexualBehaviourScreening;
import org.mockito.ArgumentCaptor;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.kvp.util.KvpUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.util.JsonFormUtils;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KvpScreeningEnrollmentFilteringTest {

    private static final String DISCORDANT_STEP = MarpDiscordantCoupleScreening.STEP;
    private static final String DISCORDANT_GROUP = MarpDiscordantCoupleScreening.GROUP_CODE;
    private static final String PARTNER_PARAMETERS = MarpDiscordantCoupleScreening.PARTNER_PARAMETERS;
    private static final Locale KISWAHILI = new Locale("sw");
    private static final String MHR_EN = "A man at higher risk (MHR)";
    private static final String MHR_SW = "Mwanaume aliye katika hatari zaidi (MHR)";
    private static final String WHR_EN = "A woman at high risk (WHR)";
    private static final String WHR_SW = "Mwanamke aliye katika hatari zaidi (WHR)";

    private static final String PWID_EN = "People who inject drugs";
    private static final String PWID_SW = "Wajidunga dawa za kulevya";
    private static final String PWUD_EN = "People who use drugs through non-injecting routes";
    private static final String PWUD_SW = "Watumiaji wa dawa za kulevya kwa njia nyingine";
    private static final String OTHER_GROUP = "other_key_and_vulnerable_populations";
    private static final String TRUCK_DRIVERS = "long_distance_truck_drivers";

    @Test
    public void drugUseOutcomesInEnglish() throws Exception {
        assertDrugUseOutcomes(Locale.ENGLISH, "MARP Enrollment", PWID_EN, PWUD_EN);
    }

    @Test
    public void drugUseOutcomesInKiswahili() throws Exception {
        assertDrugUseOutcomes(KISWAHILI, "Usajili wa MARP", PWID_SW, PWUD_SW);
    }

    @Test
    public void noDrugUsePreservesAnIndependentlyEligibleGroupAndDropsAStaleDrugSelection() throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_male", Locale.ENGLISH);
        field(form.getJSONObject("step1"), "is_pwud").put("value", "no");
        field(form.getJSONObject("step2"), "is_pwid").put("value", "yes");
        field(form.getJSONObject("step5"), "is_msm").put("value", "yes");
        field(form.getJSONObject("step8"), "client_group").put("value", "pwid");

        assertEnrollmentOptions(form, "step8", Locale.ENGLISH, "MARP Enrollment", "msm", MHR_EN, "msm");
    }

    @Test
    public void switchingToNoClearsTheInjectionAnswerAndItsCalculatedFlagInBothForms() throws Exception {
        for (String sex : List.of("male", "female")) {
            JSONObject form = loadScreeningForm("kvp_screening_" + sex, KISWAHILI);
            KvpJsonWizardFormActivity activity = formActivity(form);
            writeAnswer(activity, "step1", "drug_use", "yes");
            writeAnswer(activity, "step2", "drug_injection", "yes");
            writeAnswer(activity, "step2", "is_pwid", "yes");

            writeAnswer(activity, "step1", "drug_use", "no");
            assertFalse(field(form.getJSONObject("step2"), "drug_injection").has("value"));
            assertFalse(field(form.getJSONObject("step2"), "is_pwid").has("value"));
            writeAnswer(activity, "step1", "drug_use", "yes");
            assertFalse(field(form.getJSONObject("step2"), "drug_injection").has("value"));

            // The identical answer must be accepted again by the real Native Forms write cache.
            writeAnswer(activity, "step2", "drug_injection", "yes");
            assertEquals("yes", field(form.getJSONObject("step2"), "drug_injection").getString("value"));
        }
    }

    @Test
    public void saveSerializesReselectedPwidAfterBacktrackingWithAnotherEligibleGroup() throws Exception {
        String step = "step8";
        JSONObject form = loadScreeningForm("kvp_screening_male", Locale.ENGLISH);
        KvpJsonWizardFormActivity activity = formActivity(form);
        writeAnswer(activity, "step1", "drug_use", "yes");
        writeAnswer(activity, "step1", "is_pwud", "yes");
        writeAnswer(activity, "step2", "drug_injection", "yes");
        writeAnswer(activity, "step2", "is_pwid", "yes");
        checkOtherGroup(activity, "step6", "disabled", true);
        writeAnswer(activity, step, "client_group", "pwid");

        writeAnswer(activity, "step1", "drug_use", "no");
        writeAnswer(activity, "step1", "is_pwud", "no");
        JSONObject group = field(renderEnrollment(form, step, Locale.ENGLISH, activity), "client_group");
        assertEquals(OTHER_GROUP, group.getString("value"));
        assertEquals(OTHER_GROUP, group.getJSONArray("options").getJSONObject(0).getString("key"));
        assertEquals(1, group.getJSONArray("options").length());

        writeAnswer(activity, "step1", "drug_use", "yes");
        writeAnswer(activity, "step1", "is_pwud", "yes");
        writeAnswer(activity, "step2", "drug_injection", "yes");
        writeAnswer(activity, "step2", "is_pwid", "yes");
        group = field(renderEnrollment(form, step, Locale.ENGLISH, activity), "client_group");
        assertEquals(2, group.getJSONArray("options").length());

        writeAnswer(activity, step, "should_enroll", "yes");
        writeAnswer(activity, step, "birth_region", "Tanga");
        writeAnswer(activity, step, "client_group", "pwid");
        JSONObject submittedForm = saveEnrollment(activity, step);
        assertEnrollmentEvent(submittedForm, "pwid");
    }

    @Test
    public void olderScreeningStateCannotRestoreRetiredGroupsOnEnrollmentOrBacktracking() throws Exception {
        for (String sex : List.of("male", "female")) {
            String otherGroupsStep = "male".equals(sex) ? "step6" : "step7";
            String enrollmentStep = "male".equals(sex) ? "step8" : "step9";
            for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
                for (String retiredGroup : List.of("prisoner", "rumandee", "mobile_population")) {
                    JSONObject form = loadScreeningForm("kvp_screening_" + sex, locale);
                    JSONObject otherGroups = form.getJSONObject(otherGroupsStep);
                    JSONObject enrollment = form.getJSONObject(enrollmentStep);
                    boolean mobilePopulation = "mobile_population".equals(retiredGroup);
                    String question = mobilePopulation ? "mobile_population" : "prisoner_or_rumandee";
                    String answer = mobilePopulation ? "long_distance_drivers" : retiredGroup;
                    String promptGroup = "prisoner".equals(retiredGroup) ? "prisoners" : retiredGroup;
                    String prompt = "prompt_for_" + promptGroup + "_kvp";
                    otherGroups.getJSONArray("fields")
                            .put(carriedField(question, answer))
                            .put(carriedField("is_" + retiredGroup, "yes"));
                    enrollment.getJSONArray("fields").put(carriedField(prompt, ""));
                    JSONObject group = field(enrollment, "client_group");
                    group.getJSONArray("options").put(carriedField(retiredGroup, ""));
                    group.put("value", retiredGroup);
                    field(enrollment, "should_enroll").put("value", "yes");
                    field(enrollment, "other_screened_client_group").put("value", "[" + retiredGroup + "]");

                    KvpJsonWizardFormActivity activity = formActivity(form);
                    assertFalse(form.toString().contains(retiredGroup));
                    assertFalse(field(enrollment, "should_enroll").has("value"));
                    assertFalse(field(enrollment, "other_screened_client_group").has("value"));
                    JSONObject renderedGroup = field(renderEnrollment(form, enrollmentStep, locale, activity), "client_group");
                    assertFalse(renderedGroup.has("value"));
                    assertEquals(0, renderedGroup.getJSONArray("options").length());

                    // Represent a valid answer on returning to Other Groups and reenabling enrollment.
                    checkOtherGroup(activity, otherGroupsStep, "disabled", true);
                    renderedGroup = field(renderEnrollment(form, enrollmentStep, locale, activity), "client_group");
                    assertEquals(1, renderedGroup.getJSONArray("options").length());
                    assertEquals(OTHER_GROUP, renderedGroup.getJSONArray("options")
                            .getJSONObject(0).getString("key"));
                    writeAnswer(activity, enrollmentStep, "should_enroll", "yes");
                    writeAnswer(activity, enrollmentStep, "birth_region", "Tanga");
                    writeAnswer(activity, enrollmentStep, "client_group", OTHER_GROUP);
                    writeAnswer(activity, enrollmentStep, "other_screened_client_group", "[" + OTHER_GROUP + "]");

                    JSONObject submitted = saveEnrollment(activity, enrollmentStep);
                    assertFalse(submitted.toString().contains(retiredGroup));
                    assertEnrollmentEvent(submitted, OTHER_GROUP);
                }
            }
        }
    }

    @Test
    public void clearsCarriedMobilePopulationFromSecondaryGroupsAndKeepsSupportedDominantChoice() throws Exception {
        String enrollmentStep = "step8";
        JSONObject form = loadScreeningForm("kvp_screening_male", Locale.ENGLISH);
        JSONObject enrollment = form.getJSONObject(enrollmentStep);
        field(form.getJSONObject("step5"), "is_msm").put("value", "yes");
        field(enrollment, "client_group").put("value", "msm");
        field(enrollment, "should_enroll").put("value", "yes");
        JSONObject secondaryGroups = field(enrollment, "other_screened_client_group");
        secondaryGroups.put("value", "[msm,mobile_population]");

        KvpJsonWizardFormActivity activity = formActivity(form);

        assertFalse(secondaryGroups.has("value"));
        JSONObject rendered = field(renderEnrollment(form, enrollmentStep, Locale.ENGLISH, activity), "client_group");
        assertEquals("msm", rendered.getString("value"));
        assertEquals(1, rendered.getJSONArray("options").length());
    }

    private static JSONObject carriedField(String key, String value) throws JSONException {
        return new JSONObject().put("key", key).put("value", value);
    }

    @Test
    public void discordantOptionIsBilingualAndSavesItsExistingGroupCode() throws Exception {
        for (String sex : List.of("male", "female")) {
            String enrollmentStep = "male".equals(sex) ? "step8" : "step9";
            for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
                JSONObject form = loadScreeningForm("kvp_screening_" + sex, locale);
                KvpJsonWizardFormActivity activity = formActivity(form);
                answerDiscordantQuestions(activity);
                checkParameter(activity, "not_started_art", true);
                checkParameter(activity, "art_less_than_six_months", true);
                assertEquals("yes", field(form.getJSONObject(DISCORDANT_STEP), "is_sdc").getString("value"));
                checkParameter(activity, "not_started_art", false);

                String title = KISWAHILI.equals(locale) ? "Usajili wa MARP" : "MARP Enrollment";
                assertEnrollmentOptions(form, enrollmentStep, locale, title, DISCORDANT_GROUP,
                        discordantLabel(locale), "");
                writeAnswer(activity, enrollmentStep, "should_enroll", "yes");
                writeAnswer(activity, enrollmentStep, "birth_region", "Tanga");
                writeAnswer(activity, enrollmentStep, "client_group", DISCORDANT_GROUP);

                JSONObject submitted = saveEnrollment(activity, enrollmentStep);
                assertEnrollmentEvent(submitted, DISCORDANT_GROUP);
                Event event = JsonFormUtils.createEvent(KvpJsonFormUtils.kvpFormFields(submitted),
                        submitted.getJSONObject("metadata"), new FormTag(), "synthetic-client",
                        "KVP Registration", "KVP Registration");
                Obs parameter = event.getObs().stream()
                        .filter(obs -> PARTNER_PARAMETERS.equals(obs.getFieldCode()))
                        .findFirst().orElseThrow();
                assertEquals(List.of("art_less_than_six_months"), parameter.getValues());
            }
        }
    }

    @Test
    public void changingPartnerStatusOrSexualHistoryDiscardsInactiveAnswers() throws Exception {
        for (String sex : List.of("male", "female")) {
            JSONObject form = loadScreeningForm("kvp_screening_" + sex, KISWAHILI);
            KvpJsonWizardFormActivity activity = formActivity(form);
            JSONObject page = form.getJSONObject(DISCORDANT_STEP);
            JSONObject checklist = field(page, PARTNER_PARAMETERS);
            answerDiscordantQuestions(activity);
            checkParameter(activity, "not_started_art", true);

            writeAnswer(activity, DISCORDANT_STEP, "hiv_positive_partner", "no");
            assertFalse(checklist.has("value"));
            assertEquals("no", field(page, "is_sdc").getString("value"));
            writeAnswer(activity, DISCORDANT_STEP, "hiv_positive_partner", "yes");
            assertFalse(MarpDiscordantCoupleScreening.isEligible(form));
            checkParameter(activity, "not_started_art", true);
            assertTrue(MarpDiscordantCoupleScreening.isEligible(form));

            writeAnswer(activity, DISCORDANT_STEP, "hiv_status_on_screening", "positive");
            assertFalse(field(page, "hiv_positive_partner").has("value"));
            assertFalse(checklist.has("value"));
            writeAnswer(activity, DISCORDANT_STEP, "hiv_status_on_screening", "negative");
            assertFalse(field(page, "hiv_positive_partner").has("value"));
            writeAnswer(activity, DISCORDANT_STEP, "hiv_positive_partner", "yes");
            checkParameter(activity, "not_started_art", true);

            writeAnswer(activity, DISCORDANT_STEP, "client_knows_hiv_status", "no");
            assertFalse(field(page, "hiv_status_on_screening").has("value"));
            assertFalse(field(page, "hiv_positive_partner").has("value"));
            assertFalse(checklist.has("value"));
            answerDiscordantQuestions(activity);
            checkParameter(activity, "not_started_art", true);
            assertTrue(MarpDiscordantCoupleScreening.isEligible(form));

            writeAnswer(activity, "step3", "sexual_intercourse", "no");
            assertFalse(field(page, "client_knows_hiv_status").has("value"));
            assertFalse(field(page, "hiv_status_on_screening").has("value"));
            assertFalse(field(page, "hiv_positive_partner").has("value"));
            assertFalse(checklist.has("value"));
            assertEquals("no", field(page, "is_sdc").getString("value"));
        }
    }

    @Test
    public void noSexualHistoryClearsSkippedRiskAnswersAndKeepsIndependentGroups() throws Exception {
        for (String sex : List.of("male", "female")) {
            boolean female = "female".equals(sex);
            JSONObject form = loadScreeningForm("kvp_screening_" + sex, Locale.ENGLISH);
            KvpJsonWizardFormActivity activity = formActivity(form);
            String riskQuestion = female ? "sex_for_money" : "sexual_intercourse_with";
            String riskFlag = female ? "is_fsw" : "is_msm";
            String otherGroupsStep = female ? "step7" : "step6";
            String enrollmentStep = female ? "step9" : "step8";
            JSONObject riskPage = form.getJSONObject("step5");
            JSONObject otherGroups = form.getJSONObject(otherGroupsStep);
            JSONObject dominant = field(form.getJSONObject(enrollmentStep), "client_group");

            writeAnswer(activity, "step1", "drug_use", "yes");
            writeAnswer(activity, "step3", "sexual_intercourse", "yes");
            writeAnswer(activity, "step5", riskQuestion, female ? "yes" : "man");
            writeAnswer(activity, "step5", riskFlag, "yes");
            checkOtherGroup(activity, otherGroupsStep, TRUCK_DRIVERS, true);
            writeAnswer(activity, enrollmentStep, "client_group", female ? "agyw" : "msm");
            if (female) {
                writeAnswer(activity, "step6", "client_in_school", "no");
                activity.writeValue("step6", "harsh_condition_impact", "options", "sex_for_material", "true",
                        "", "concept", "sex_for_material", false);
                writeAnswer(activity, "step6", "is_agyw", "yes");
            }

            writeAnswer(activity, "step3", "sexual_intercourse", "no");
            assertEquals(otherGroupsStep, MarpSexualBehaviourScreening.nextStepAfterNo(activity, "step3"));
            assertEquals("no", field(form.getJSONObject("step3"), "sexual_intercourse").getString("value"));
            assertFalse(field(riskPage, riskQuestion).has("value"));
            assertFalse(field(riskPage, riskFlag).has("value"));
            assertFalse(dominant.has("value"));
            assertEquals("yes", field(form.getJSONObject("step1"), "drug_use").getString("value"));
            assertEquals(TRUCK_DRIVERS, field(otherGroups, "eligible_other_marp_groups").getString("value"));
            assertEquals(Set.of(MarpOtherGroupsScreening.Group.TRUCK_DRIVERS),
                    MarpOtherGroupsScreening.eligibleGroups(form));
            if (female) {
                JSONObject vagyw = new JSONObject(activity.currentJsonState()).getJSONObject("step6");
                assertFalse(field(vagyw, "client_in_school").has("value"));
                assertFalse(field(vagyw, "is_agyw").has("value"));
                JSONObject criterion = JsonFormUtils.getFieldJSONObject(
                        field(vagyw, "harsh_condition_impact").getJSONArray("options"), "sex_for_material");
                assertFalse(criterion.optBoolean("value"));
            }

            writeAnswer(activity, enrollmentStep, "client_group", TRUCK_DRIVERS);
            assertEquals(otherGroupsStep, MarpSexualBehaviourScreening.nextStepAfterNo(activity, "step3"));
            assertEquals(TRUCK_DRIVERS, dominant.getString("value"));
        }
    }

    @Test
    public void noneDropsOnlyTheDiscordantOptionAndRequiresReselectionWhenEligibleAgain() throws Exception {
        String enrollmentStep = "step8";
        JSONObject form = loadScreeningForm("kvp_screening_male", Locale.ENGLISH);
        KvpJsonWizardFormActivity activity = formActivity(form);
        answerDiscordantQuestions(activity);
        checkParameter(activity, "not_started_art", true);
        checkOtherGroup(activity, "step6", "disabled", true);
        writeAnswer(activity, enrollmentStep, "client_group", DISCORDANT_GROUP);

        // Native Forms unchecks the old choices before writing the exclusive choice.
        checkParameter(activity, "not_started_art", false);
        checkParameter(activity, "none", true);
        JSONObject group = field(renderEnrollment(form, enrollmentStep, Locale.ENGLISH, activity), "client_group");
        assertEquals(OTHER_GROUP, group.getString("value"));
        assertEquals(1, group.getJSONArray("options").length());
        assertEquals(OTHER_GROUP, group.getJSONArray("options").getJSONObject(0).getString("key"));
        assertEquals("no", field(form.getJSONObject(DISCORDANT_STEP), "is_sdc").getString("value"));

        writeAnswer(activity, enrollmentStep, "client_group", OTHER_GROUP);
        checkParameter(activity, "none", false);
        checkParameter(activity, "art_adherence_doubt", true);
        group = field(renderEnrollment(form, enrollmentStep, Locale.ENGLISH, activity), "client_group");
        assertEquals(OTHER_GROUP, group.getString("value"));
        assertEquals(2, group.getJSONArray("options").length());

        checkParameter(activity, "art_adherence_doubt", false);
        checkParameter(activity, "none", true);
        group = field(renderEnrollment(form, enrollmentStep, Locale.ENGLISH, activity), "client_group");
        assertEquals(OTHER_GROUP, group.getString("value"));

        checkParameter(activity, "none", false);
        checkParameter(activity, "not_started_art", true);
        group = field(renderEnrollment(form, enrollmentStep, Locale.ENGLISH, activity), "client_group");
        assertEquals(2, group.getJSONArray("options").length());
        assertEquals(OTHER_GROUP, group.getString("value"));
        writeAnswer(activity, enrollmentStep, "should_enroll", "yes");
        writeAnswer(activity, enrollmentStep, "birth_region", "Tanga");
        writeAnswer(activity, enrollmentStep, "client_group", DISCORDANT_GROUP);
        assertEnrollmentEvent(saveEnrollment(activity, enrollmentStep), DISCORDANT_GROUP);
    }

    private static void answerDiscordantQuestions(KvpJsonWizardFormActivity activity) throws Exception {
        writeAnswer(activity, "step3", "sexual_intercourse", "yes");
        writeAnswer(activity, DISCORDANT_STEP, "client_knows_hiv_status", "yes");
        writeAnswer(activity, DISCORDANT_STEP, "hiv_status_on_screening", "negative");
        writeAnswer(activity, DISCORDANT_STEP, "hiv_positive_partner", "yes");
    }

    private static void checkParameter(KvpJsonWizardFormActivity activity, String key, boolean checked)
            throws Exception {
        activity.writeValue(DISCORDANT_STEP, PARTNER_PARAMETERS, "options", key, String.valueOf(checked),
                "", "concept", key, false);
    }

    private static void checkOtherGroup(KvpJsonWizardFormActivity activity, String step, String key, boolean checked)
            throws Exception {
        activity.writeValue(step, MarpOtherGroupsScreening.CHECKLIST_FIELD, "options", key,
                String.valueOf(checked), "", "concept", key, false);
    }

    @Test
    public void otherGroupsRecomputeFromCurrentSelectionsAndKeepEarlierEligibility() throws Exception {
        for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
            for (String sex : List.of("male", "female")) {
                JSONObject form = loadScreeningForm("kvp_screening_" + sex, locale);
                String otherStep = MarpOtherGroupsScreening.checklistStep(form);
                String enrollment = "male".equals(sex) ? "step8" : "step9";
                KvpJsonWizardFormActivity activity = formActivity(form);
                checkOtherGroup(activity, otherStep, "fisherfolk", true);
                checkOtherGroup(activity, otherStep, "miners", true);
                JSONObject fact = field(form.getJSONObject(otherStep), "eligible_other_marp_groups");
                assertEquals("fisherfolk,miners", fact.getString("value"));
                JSONObject group = field(renderEnrollment(form, enrollment, locale, activity), "client_group");
                assertEquals(2, group.getJSONArray("options").length());
                assertGroupOption(group, 0, "fisherfolk", KISWAHILI.equals(locale) ? "Wavuvi" : "Fisherfolk");
                assertGroupOption(group, 1, "miners", KISWAHILI.equals(locale) ? "Wachimba madini" : "Miners");
                assertFalse(group.has("value"));
                writeAnswer(activity, enrollment, "client_group", "miners");

                checkOtherGroup(activity, otherStep, "miners", false);
                assertFalse(field(form.getJSONObject(enrollment), "client_group").has("value"));
                checkOtherGroup(activity, otherStep, "fisherfolk", false);
                checkOtherGroup(activity, otherStep, "none", true);
                assertEquals("none", fact.getString("value"));
                group = field(renderEnrollment(form, enrollment, locale, activity), "client_group");
                assertEquals(0, group.getJSONArray("options").length());
                assertFalse(group.has("value"));

                // A checked qualifying choice replaces None. Each of the three choices contributes the same group.
                checkOtherGroup(activity, otherStep, "none", false);
                for (String choice : List.of("disabled", "farmers_on_large_farms", "construction_workers")) {
                    checkOtherGroup(activity, otherStep, choice, true);
                    assertEquals(OTHER_GROUP, fact.getString("value"));
                }
                assertEnrollmentOptions(form, enrollment, locale,
                        KISWAHILI.equals(locale) ? "Usajili wa MARP" : "MARP Enrollment", OTHER_GROUP,
                        KISWAHILI.equals(locale) ? "Makundi mengine hatarishi" : "Other key and vulnerable populations",
                        OTHER_GROUP);

                writeAnswer(activity, "step1", "is_pwud", "yes");
                writeAnswer(activity, enrollment, "client_group", "pwud");
                for (String choice : List.of("disabled", "farmers_on_large_farms", "construction_workers")) {
                    checkOtherGroup(activity, otherStep, choice, false);
                }
                checkOtherGroup(activity, otherStep, "none", true);
                group = field(renderEnrollment(form, enrollment, locale, activity), "client_group");
                assertEquals(1, group.getJSONArray("options").length());
                assertEquals("pwud", group.getString("value"));
            }
        }
    }

    @Test
    public void savesTruckDriverEnrollmentWithTheChecklistAnswerInBothForms() throws Exception {
        for (String sex : List.of("male", "female")) {
            for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
                JSONObject form = loadScreeningForm("kvp_screening_" + sex, locale);
                KvpJsonWizardFormActivity activity = formActivity(form);
                String otherStep = MarpOtherGroupsScreening.checklistStep(form);
                String enrollment = "male".equals(sex) ? "step8" : "step9";
                checkOtherGroup(activity, otherStep, TRUCK_DRIVERS, true);
                JSONObject group = field(renderEnrollment(form, enrollment, locale, activity), "client_group");
                assertEquals(1, group.getJSONArray("options").length());
                assertGroupOption(group, 0, TRUCK_DRIVERS,
                        KISWAHILI.equals(locale) ? "Madereva wa masafa marefu" : "Long-distance truck drivers");
                assertEquals(TRUCK_DRIVERS, group.getString("value"));
                writeAnswer(activity, enrollment, "should_enroll", "yes");
                writeAnswer(activity, enrollment, "birth_region", "Tanga");

                JSONObject submitted = saveEnrollment(activity, enrollment);
                Event event = assertEnrollmentEvent(submitted, TRUCK_DRIVERS);
                Obs checklist = event.getObs().stream()
                        .filter(obs -> MarpOtherGroupsScreening.CHECKLIST_FIELD.equals(obs.getFieldCode()))
                        .findFirst().orElseThrow();
                assertEquals(List.of(TRUCK_DRIVERS), checklist.getValues());
            }
        }
    }

    @Test
    public void vagywQualifyingAnswersUseTheExistingGroupAndNewObservationCodesInBothLanguages() throws Exception {
        for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
            JSONObject form = loadScreeningForm("kvp_screening_female", locale);
            KvpJsonWizardFormActivity activity = adultVagywScreening(form);
            String enrollment = MarpAgywScreening.ENROLLMENT_STEP;
            JSONObject eligibility = field(form.getJSONObject(MarpAgywScreening.STEP), "is_agyw");
            assertEquals("no", eligibility.getString("value"));
            checkVagyw(activity, "not_applicable", true);
            assertEquals("no", eligibility.getString("value"));
            assertEquals(0, field(renderEnrollment(form, enrollment, locale, activity), "client_group")
                    .getJSONArray("options").length());

            checkVagyw(activity, "not_applicable", false);
            for (String choice : List.of("sex_for_material", "partner_self_injects_drugs", "sexually_abused",
                    "infected_with_sexual_disease", "unprotected_sex_last_three_months")) {
                checkVagyw(activity, choice, true);
                assertEquals("yes", eligibility.getString("value"));
                checkVagyw(activity, choice, false);
                assertEquals("no", eligibility.getString("value"));
            }

            String criterion = KISWAHILI.equals(locale)
                    ? "unprotected_sex_last_three_months" : "partner_self_injects_drugs";
            checkVagyw(activity, criterion, true);
            JSONObject group = field(renderEnrollment(form, enrollment, locale, activity), "client_group");
            assertEquals(1, group.getJSONArray("options").length());
            assertGroupOption(group, 0, MarpAgywScreening.GROUP_CODE, agywLabel(locale));
            writeAnswer(activity, enrollment, "should_enroll", "yes");
            writeAnswer(activity, enrollment, "birth_region", "Tanga");
            writeAnswer(activity, enrollment, "client_group", MarpAgywScreening.GROUP_CODE);

            JSONObject submitted = saveEnrollment(activity, enrollment);
            Event event = assertEnrollmentEvent(submitted, MarpAgywScreening.GROUP_CODE);
            Obs checklist = event.getObs().stream()
                    .filter(obs -> MarpAgywScreening.CHECKLIST_FIELD.equals(obs.getFieldCode()))
                    .findFirst().orElseThrow();
            assertEquals(List.of(criterion), checklist.getValues());

            KvpJsonWizardFormActivity reopened = formActivity(submitted);
            group = field(renderEnrollment(submitted, enrollment, locale, reopened), "client_group");
            assertEquals(MarpAgywScreening.GROUP_CODE, group.getString("value"));
            assertGroupOption(group, 0, MarpAgywScreening.GROUP_CODE, agywLabel(locale));
        }
    }

    @Test
    public void changingSchoolOrWhrClearsTheChecklistAndOnlyTheStaleAgywEnrollmentChoice() throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_female", KISWAHILI);
        KvpJsonWizardFormActivity activity = adultVagywScreening(form);
        String step = MarpAgywScreening.STEP;
        String enrollment = MarpAgywScreening.ENROLLMENT_STEP;
        String criterion = "partner_self_injects_drugs";
        JSONObject school = field(form.getJSONObject(step), "client_in_school");
        JSONObject checklist = field(form.getJSONObject(step), MarpAgywScreening.CHECKLIST_FIELD);
        JSONObject eligibility = field(form.getJSONObject(step), "is_agyw");
        JSONObject group = field(form.getJSONObject(enrollment), "client_group");

        checkOtherGroup(activity, "step7", TRUCK_DRIVERS, true);
        checkVagyw(activity, criterion, true);
        writeAnswer(activity, enrollment, "client_group", MarpAgywScreening.GROUP_CODE);
        writeAnswer(activity, step, "client_in_school", "yes");
        assertEquals("yes", school.getString("value"));
        assertFalse(checklist.has("value"));
        assertEquals("no", eligibility.getString("value"));
        assertFalse(group.has("value"));
        JSONObject rendered = field(renderEnrollment(form, enrollment, KISWAHILI, activity), "client_group");
        assertEquals(1, rendered.getJSONArray("options").length());
        assertGroupOption(rendered, 0, TRUCK_DRIVERS, "Madereva wa masafa marefu");

        writeAnswer(activity, step, "client_in_school", "no");
        assertFalse(checklist.has("value"));
        checkVagyw(activity, criterion, true);
        assertEquals("yes", eligibility.getString("value"));
        writeAnswer(activity, "step5", "sex_for_money", "yes");
        writeAnswer(activity, "step5", "main_income_source_is_sex", "yes");
        assertFalse(school.has("value"));
        assertFalse(checklist.has("value"));
        assertEquals("no", eligibility.getString("value"));
        assertEquals(TRUCK_DRIVERS, group.getString("value"));

        writeAnswer(activity, "step5", "sex_for_money", "no");
        assertFalse(school.has("value"));
        writeAnswer(activity, step, "client_in_school", "no");
        checkVagyw(activity, criterion, true);
        assertEquals("yes", eligibility.getString("value"));
    }

    @Test
    public void olderAdultsAndBothMaternityRoutesCannotKeepAgywEligibility() throws Exception {
        for (String route : List.of("age", MarpPregnancyScreening.PREGNANCY_FIELD,
                MarpPregnancyScreening.BREASTFEEDING_FIELD)) {
            JSONObject form = loadScreeningForm("kvp_screening_female", Locale.ENGLISH);
            KvpJsonWizardFormActivity activity = adultVagywScreening(form);
            String step = MarpAgywScreening.STEP;
            String enrollment = MarpAgywScreening.ENROLLMENT_STEP;
            String criterion = "unprotected_sex_last_three_months";
            JSONObject checklist = field(form.getJSONObject(step), MarpAgywScreening.CHECKLIST_FIELD);
            checkVagyw(activity, criterion, true);
            assertTrue(MarpAgywScreening.isEligible(form));
            writeAnswer(activity, enrollment, "client_group", MarpAgywScreening.GROUP_CODE);

            if ("age".equals(route)) {
                form.getJSONObject("global").put("age", 36);
                assertEquals(0, field(renderEnrollment(form, enrollment, Locale.ENGLISH, activity),
                        "client_group").getJSONArray("options").length());
            } else {
                writeAnswer(activity, MarpPregnancyScreening.INTAKE_STEP, route, "yes");
                assertEquals(MarpPregnancyScreening.HIGH_RISK_STEP, MarpPregnancyScreening.nextStep(form));
                assertFalse(checklist.has("value"));
            }
            assertFalse(MarpAgywScreening.isEligible(form));
            assertFalse(field(form.getJSONObject(enrollment), "client_group").has("value"));
        }
    }

    private static KvpJsonWizardFormActivity adultVagywScreening(JSONObject form) throws Exception {
        form.getJSONObject("global").put("age", 19);
        KvpJsonWizardFormActivity activity = formActivity(form);
        String intake = MarpPregnancyScreening.INTAKE_STEP;
        writeAnswer(activity, intake, MarpPregnancyScreening.PREGNANCY_FIELD, "no");
        writeAnswer(activity, intake, MarpPregnancyScreening.BREASTFEEDING_FIELD, "no");
        writeAnswer(activity, "step1", "is_pwud", "no");
        writeAnswer(activity, "step2", "is_pwid", "no");
        writeAnswer(activity, "step3", "sexual_intercourse", "yes");
        writeAnswer(activity, "step5", "sex_for_money", "no");
        writeAnswer(activity, "step5", "is_fsw", "no");
        writeAnswer(activity, MarpAgywScreening.STEP, "client_in_school", "no");
        return activity;
    }

    private static void checkVagyw(KvpJsonWizardFormActivity activity, String criterion, boolean checked)
            throws JSONException {
        activity.writeValue(MarpAgywScreening.STEP, MarpAgywScreening.CHECKLIST_FIELD, "options", criterion,
                String.valueOf(checked), "", "concept", criterion, false);
    }

    private static String agywLabel(Locale locale) {
        return KISWAHILI.equals(locale)
                ? "Msichana rika balehe na mwanamke kijana" : "Adolescent girls and young women";
    }

    private static void assertGroupOption(JSONObject group, int index, String key, String label) throws Exception {
        JSONObject option = group.getJSONArray("options").getJSONObject(index);
        assertEquals(key, option.getString("key"));
        assertEquals(key, option.getString("openmrs_entity_id"));
        assertEquals(label, option.getString("text"));
    }

    private static String discordantLabel(Locale locale) {
        return KISWAHILI.equals(locale) ? "Mwenza mwenye majibu kinzani" : "Discordant partner";
    }

    private static KvpJsonWizardFormActivity formActivity(JSONObject form) throws Exception {
        // Android's JSONObject.getString coerces JSONArray, unlike the test runtime's org.json.
        for (String step : List.of(DISCORDANT_STEP, "step6", "step7")) {
            JSONArray fields = form.getJSONObject(step).getJSONArray("fields");
            for (int i = 0; i < fields.length(); i++) {
                JSONObject field = fields.getJSONObject(i);
                if ("check_box".equals(field.optString("type"))) {
                    fields.put(i, new JSONObject(field.toString()) {
                        @Override
                        public String getString(String name) throws JSONException {
                            Object value = get(name);
                            return value instanceof JSONArray ? value.toString() : super.getString(name);
                        }
                    });
                }
            }
        }

        ArchTaskExecutor executor = ArchTaskExecutor.getInstance();
        executor.setDelegate(new TaskExecutor() {
            @Override
            public void executeOnDiskIO(Runnable task) {
                task.run();
            }

            @Override
            public void postToMainThread(Runnable task) {
                task.run();
            }

            @Override
            public boolean isMainThread() {
                return true;
            }
        });
        KvpJsonWizardFormActivity activity;
        try {
            activity = new KvpJsonWizardFormActivity() {
                @Override
                public void invokeRefreshLogic(String value, boolean popup, String parentKey, String childKey,
                                               String stepName, boolean forNextStep) {
                    // The test supplies calculation outputs without rendering Android views.
                }

                @Override
                public void initializeDependencyMaps() {
                    // No Android views are inflated by the mock interactor.
                }
            };
        } finally {
            executor.setDelegate(null);
        }
        // Run the real Native Forms index and write cache synchronously on the JVM.
        Field executors = JsonFormActivity.class.getDeclaredField("appExecutors");
        executors.setAccessible(true);
        executors.set(activity, new AppExecutors(Runnable::run, Runnable::run, Runnable::run));
        activity.setmJSONObject(form);
        return activity;
    }

    private static void writeAnswer(KvpJsonWizardFormActivity activity, String step, String key, String value)
            throws Exception {
        activity.writeValue(step, key, value, "", "concept", key, false);
    }

    private static JSONObject saveEnrollment(KvpJsonWizardFormActivity activity, String step) throws Exception {
        KvpScreeningJsonFormFragment fragment = mock(KvpScreeningJsonFormFragment.class);
        AtomicReference<String> serializedState = new AtomicReference<>();
        when(fragment.getJsonApi()).thenReturn(activity);
        when(fragment.getOnFieldsInvalidCallback()).thenReturn(mock(OnFieldsInvalid.class));
        when(fragment.getCurrentJsonState()).thenAnswer(call -> {
            String state = activity.currentJsonState();
            serializedState.set(state);
            return state;
        });
        KvpScreeningJsonFormFragmentPresenter presenter = new KvpScreeningJsonFormFragmentPresenter(
                fragment, mock(JsonFormInteractor.class)) {
            {
                mStepName = step;
                mStepDetails = activity.getStep(step);
            }
        };
        presenter.attachView(fragment);
        LinearLayout main = mock(LinearLayout.class);
        when(main.getTag(com.vijay.jsonwizard.R.id.skip_validation)).thenReturn(false);

        presenter.onSaveClick(main);

        verify(fragment).finishWithResult(any(Intent.class));
        return new JSONObject(serializedState.get());
    }

    @Test
    public void femaleWhrOnlyInKiswahili() throws Exception {
        assertOnlyFemaleWhr(KISWAHILI, "Usajili wa MARP");
    }

    @Test
    public void femaleWhrOnlyInEnglish() throws Exception {
        assertOnlyFemaleWhr(Locale.ENGLISH, "MARP Enrollment");
    }

    @Test
    public void highRiskEnrollmentPreselectsItsOnlyGroupAndRequiresBirthRegionInEnglish() throws Exception {
        assertHighRiskEnrollment(Locale.ENGLISH, "MARP Enrollment", "Pregnant or breastfeeding women at high risk");
    }

    @Test
    public void highRiskEnrollmentPreselectsItsOnlyGroupAndRequiresBirthRegionInKiswahili() throws Exception {
        assertHighRiskEnrollment(KISWAHILI, "Usajili wa MARP", "Wajawazito na wanaonyonyesha walio katika hatari");
    }

    @Test
    public void maleMhrOnlyInEnglish() throws Exception {
        assertOnlyMaleMhr(Locale.ENGLISH, "MARP Enrollment");
    }

    @Test
    public void maleMhrOnlyInKiswahili() throws Exception {
        assertOnlyMaleMhr(KISWAHILI, "Usajili wa MARP");
    }

    @Test
    public void restoresLocalizedMhrOptionAfterAnswersChange() throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_male", KISWAHILI);
        JSONObject enrollment = form.getJSONObject("step8");
        field(enrollment, "client_group").put("value", "msm");
        field(form.getJSONObject("step5"), "is_msm").put("value", "no");
        JSONObject group = field(renderEnrollment(form, "step8", KISWAHILI), "client_group");
        assertEquals(0, group.getJSONArray("options").length());
        assertFalse(group.has("value"));

        field(form.getJSONObject("step5"), "is_msm").put("value", "yes");
        assertEnrollmentOptions(form, "step8", KISWAHILI, "Usajili wa MARP", "msm", MHR_SW, "msm");
    }

    @Test
    public void retainsAnExistingSelectionWhenMultipleGroupsQualify() throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_male", Locale.ENGLISH);
        field(form.getJSONObject("step1"), "is_pwud").put("value", "yes");
        field(form.getJSONObject("step5"), "is_msm").put("value", "yes");
        JSONObject clientGroup = field(form.getJSONObject("step8"), "client_group");

        JSONObject rendered = field(renderEnrollment(form, "step8", Locale.ENGLISH), "client_group");
        assertEquals(2, rendered.getJSONArray("options").length());
        assertFalse(rendered.has("value"));

        clientGroup.put("value", "pwud");
        rendered = field(renderEnrollment(form, "step8", Locale.ENGLISH), "client_group");
        assertEquals(2, rendered.getJSONArray("options").length());
        assertEquals("pwud", rendered.getString("value"));
    }

    private static void assertDrugUseOutcomes(Locale locale, String title, String injectingLabel,
                                              String nonInjectingLabel) throws Exception {
        assertDrugUseOutcomes("kvp_screening_male", "step8", locale, title, injectingLabel, nonInjectingLabel);
        assertDrugUseOutcomes("kvp_screening_female", "step9", locale, title, injectingLabel, nonInjectingLabel);
    }

    private static void assertDrugUseOutcomes(String formName, String step, Locale locale, String title,
                                              String injectingLabel, String nonInjectingLabel) throws Exception {
        JSONObject form = loadScreeningForm(formName, locale);
        JSONObject recentDrugUse = field(form.getJSONObject("step1"), "is_pwud").put("value", "yes");
        JSONObject injection = field(form.getJSONObject("step2"), "is_pwid").put("value", "yes");
        JSONObject enrollment = field(form.getJSONObject(step), "client_group");
        field(form.getJSONObject(step), "should_enroll").put("value", "yes");
        field(form.getJSONObject(step), "birth_region").put("value", "Tanga");

        assertEnrollmentOptions(form, step, locale, title, "pwid", injectingLabel, "");
        enrollment.put("value", "pwid");
        assertEnrollmentOptions(form, step, locale, title, "pwid", injectingLabel, "pwid");
        assertEnrollmentEvent(form, "pwid");

        injection.put("value", "no");
        assertEnrollmentOptions(form, step, locale, title, "pwud", nonInjectingLabel, "");
        enrollment.put("value", "pwud");
        assertEnrollmentOptions(form, step, locale, title, "pwud", nonInjectingLabel, "pwud");
        assertEnrollmentEvent(form, "pwud");

        recentDrugUse.put("value", "no");
        injection.remove("value");
        JSONObject noDrugGroup = field(renderEnrollment(form, step, locale), "client_group");
        assertEquals(0, noDrugGroup.getJSONArray("options").length());
        assertFalse(noDrugGroup.has("value"));
    }

    private static Event assertEnrollmentEvent(JSONObject form, String group) throws Exception {
        assertTrue(KvpUtil.isClientToBeRegistered(form.toString()));

        Event event = JsonFormUtils.createEvent(KvpJsonFormUtils.kvpFormFields(form), form.getJSONObject("metadata"),
                new FormTag(), "synthetic-client", "KVP Registration", "KVP Registration");
        Obs groupObservation = event.getObs().stream()
                .filter(obs -> "client_group".equals(obs.getFieldCode()))
                .findFirst().orElseThrow();
        assertEquals(List.of(group), groupObservation.getValues());
        return event;
    }

    private static void assertOnlyMaleMhr(Locale locale, String title) throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_male", locale);
        assertEquals("MHR", form.getJSONObject("step5").getString("title"));
        field(form.getJSONObject("step5"), "is_msm").put("value", "yes");
        assertEnrollmentOptions(form, "step8", locale, title, "msm",
                KISWAHILI.equals(locale) ? MHR_SW : MHR_EN, "msm");
    }

    private static void assertOnlyFemaleWhr(Locale locale, String title) throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_female", locale);
        assertEquals("WHR", form.getJSONObject("step5").getString("title"));
        field(form.getJSONObject("step5"), "is_fsw").put("value", "yes");
        assertEnrollmentOptions(form, "step9", locale, title, "fsw",
                KISWAHILI.equals(locale) ? WHR_SW : WHR_EN, "fsw");
    }

    private static void assertHighRiskEnrollment(Locale locale, String title, String label) throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_female", locale);
        String step = MarpPregnancyScreening.ENROLLMENT_STEP;
        String group = MarpPregnancyScreening.GROUP_CODE;
        assertEnrollmentOptions(form, step, locale, title, group, label, group);
        JSONObject enrollment = form.getJSONObject(step);
        assertTrue(field(enrollment, "birth_region").getJSONObject("v_required").getBoolean("value"));
        assertFalse(field(enrollment, "birth_region").has("value"));
        assertTrue(field(enrollment, "prompt_for_pregnant_breastfeeding_high_risk").getString("text").contains(label));
    }

    private static void assertEnrollmentOptions(JSONObject form, String stepName, Locale locale,
                                                String title, String groupKey, String groupLabel,
                                                String selectedKey) throws Exception {
        JSONObject step = renderEnrollment(form, stepName, locale);
        assertEquals(title, step.getString("title"));
        JSONObject clientGroup = field(step, "client_group");
        assertEquals(selectedKey, clientGroup.optString("value"));
        JSONArray options = clientGroup.getJSONArray("options");
        assertEquals(options.toString(), 1, options.length());
        JSONObject option = options.getJSONObject(0);
        assertEquals(groupKey, option.getString("key"));
        assertEquals(groupKey, option.getString("openmrs_entity_id"));
        assertEquals(groupLabel, option.getString("text"));
    }

    private static JSONObject renderEnrollment(JSONObject form, String stepName, Locale locale) throws Exception {
        JsonApi jsonApi = mock(JsonApi.class);
        AppExecutors executors = new AppExecutors(Runnable::run, Runnable::run, Runnable::run);
        when(jsonApi.getAppExecutors()).thenReturn(executors);
        return renderEnrollment(form, stepName, locale, jsonApi);
    }

    private static JSONObject renderEnrollment(JSONObject form, String stepName, Locale locale, JsonApi jsonApi)
            throws Exception {
        KvpScreeningJsonFormFragment fragment = mock(KvpScreeningJsonFormFragment.class);
        JsonFormInteractor interactor = mock(JsonFormInteractor.class);
        Context context = mock(Context.class);
        Resources resources = mock(Resources.class);
        when(resources.getString(R.string.marp_mhr_option)).thenReturn(KISWAHILI.equals(locale) ? MHR_SW : MHR_EN);
        when(resources.getString(R.string.marp_whr_option)).thenReturn(KISWAHILI.equals(locale) ? WHR_SW : WHR_EN);
        when(context.getString(R.string.marp_pwid_enrollment_option))
                .thenReturn(KISWAHILI.equals(locale) ? PWID_SW : PWID_EN);
        when(context.getString(R.string.marp_pwud_enrollment_option))
                .thenReturn(KISWAHILI.equals(locale) ? PWUD_SW : PWUD_EN);
        when(context.getString(R.string.marp_discordant_partner_option)).thenReturn(discordantLabel(locale));
        when(context.getString(R.string.marp_agyw_option)).thenReturn(agywLabel(locale));
        boolean swahili = KISWAHILI.equals(locale);
        when(resources.getString(R.string.kvp_long_distance_truck_drivers))
                .thenReturn(swahili ? "Madereva wa masafa marefu" : "Long-distance truck drivers");
        when(resources.getString(R.string.kvp_fisherfolk)).thenReturn(swahili ? "Wavuvi" : "Fisherfolk");
        when(resources.getString(R.string.kvp_miners)).thenReturn(swahili ? "Wachimba madini" : "Miners");
        when(resources.getString(R.string.kvp_other_key_and_vulnerable_populations))
                .thenReturn(swahili ? "Makundi mengine hatarishi" : "Other key and vulnerable populations");
        when(context.getResources()).thenReturn(resources);
        when(fragment.getContext()).thenReturn(context);
        when(fragment.getStep(stepName)).thenReturn(form.getJSONObject(stepName));
        when(fragment.getCurrentJsonState()).thenReturn(form.toString());
        when(fragment.getJsonApi()).thenReturn(jsonApi);

        KvpScreeningJsonFormFragmentPresenter presenter = new KvpScreeningJsonFormFragmentPresenter(fragment, interactor);
        presenter.attachView(fragment);
        presenter.addFormElements(stepName);

        ArgumentCaptor<JSONObject> step = ArgumentCaptor.forClass(JSONObject.class);
        verify(interactor).fetchFormElements(eq(stepName), eq(fragment), step.capture(), isNull(), eq(false));
        return step.getValue();
    }

    private static JSONObject field(JSONObject step, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(step.getJSONArray("fields"), key);
    }

    private static JSONObject loadScreeningForm(String name, Locale locale) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("src", "main", "assets", "json.form", name + ".json"));
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            return new JSONObject(NativeFormLangUtils.getTranslatedString(new String(bytes, StandardCharsets.UTF_8)));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
