package org.smartregister.chw.hf.presenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.interfaces.OnFieldsInvalid;
import com.vijay.jsonwizard.utils.AppExecutors;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;
import com.vijay.jsonwizard.widgets.CheckBoxFactory;
import com.vijay.jsonwizard.widgets.NativeRadioButtonFactory;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.YamlRuleDefinitionReader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.fragment.KvpScreeningJsonFormFragment;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class MarpDiscordantCoupleScreeningTest {
    private static final String KNOWS_STATUS = "client_knows_hiv_status";
    private static final String STATUS = "hiv_status_on_screening";
    private static final String PARTNER = "hiv_positive_partner";
    private static final String PARAMETERS = "partner_hiv_care_status";
    private static final Locale KISWAHILI = new Locale("sw");
    private static final Path ASSETS = Paths.get("src", "main", "assets");

    @Test
    public void hasBilingualTextAndRequiredQuestionsOnBothForms() throws Exception {
        for (String sex : List.of("male", "female")) {
            assertCopy(loadForm(sex, Locale.ENGLISH), "Discordant Couple", List.of(
                    "Does the client know his or her HIV status?", "Client HIV status",
                    "Is the client in a sexual relationship with a partner who is HIV positive?",
                    "Check the following parameters of the partner"), List.of(
                    "Yes", "No", "Positive", "Negative", "Has not started ART",
                    "Has been on ART for less than six months", "Doubts about adherence to or use of ART",
                    "The partner has no evidence of viral suppression", "None"),
                    "Please select an answer", "Select at least one partner parameter");
            assertCopy(loadForm(sex, KISWAHILI), "Wenza wenye majibu kinzani", List.of(
                    "Je, mteja anajua hali yake ya maambukizi ya VVU?", "Hali ya maambukizi ya VVU ya mteja",
                    "Je upo kwenye mahusiano ya kingono na mwenza mwenye VVU?",
                    "Chunguza vigezo vifuatavyo vya mwenza"), List.of(
                    "Ndio", "Hapana", "Chanya", "Hasi", "Hajaanza dawa za ART",
                    "Amekuwa kwenye ART chini ya miezi sita", "Ana mashaka juu ya ufuasi au matumizi ya ART",
                    "Mwenza hana ushahidi wa kufubaza VVU", "Hakuna"),
                    "Tafadhali chagua jibu", "Chagua angalau kigezo kimoja cha mwenza");
        }
    }

    @Test
    public void routesFromSexualBehaviourThroughDiscordantCoupleToMhrOrWhr() throws Exception {
        for (String sex : List.of("male", "female")) {
            JSONObject form = loadForm(sex, Locale.ENGLISH);
            assertTrue(form.getBoolean("skip_blank_steps"));
            assertEquals("step4", form.getJSONObject("step3").getString("next"));
            assertEquals("step5", form.getJSONObject("step4").getString("next"));
            boolean male = "male".equals(sex);
            assertEquals(male ? "MHR" : "WHR", form.getJSONObject("step5").getString("title"));

            Rules rules = rules(sex, "relevance");
            Rule next = rule(rules, male ? "step5_sexual_intercourse_with" : "step5_sex_for_money");
            Rule knows = rule(rules, "step4_" + KNOWS_STATUS);
            Rule status = rule(rules, "step4_" + STATUS);
            Rule partner = rule(rules, "step4_" + PARTNER);
            Rule parameters = rule(rules, "step4_" + PARAMETERS);

            Facts answers = new Facts();
            answers.put("step3_sexual_intercourse", "no");
            answers.put("step4_" + KNOWS_STATUS, "yes");
            answers.put("step4_" + STATUS, "negative");
            answers.put("step4_" + PARTNER, "yes");
            assertFalse(knows.evaluate(answers));
            assertFalse(status.evaluate(answers));
            assertFalse(partner.evaluate(answers));
            assertFalse(parameters.evaluate(answers));
            assertFalse(next.evaluate(answers));

            answers.put("step3_sexual_intercourse", "yes");
            answers.put("step4_" + KNOWS_STATUS, "no");
            assertTrue(knows.evaluate(answers));
            assertFalse(status.evaluate(answers));
            assertFalse(partner.evaluate(answers));
            assertFalse(parameters.evaluate(answers));
            assertTrue(next.evaluate(answers));

            answers.put("step4_" + KNOWS_STATUS, "yes");
            answers.put("step4_" + STATUS, "positive");
            assertTrue(status.evaluate(answers));
            assertFalse(partner.evaluate(answers));
            assertFalse(parameters.evaluate(answers));

            answers.put("step4_" + STATUS, "negative");
            answers.put("step4_" + PARTNER, "no");
            assertTrue(partner.evaluate(answers));
            assertFalse(parameters.evaluate(answers));
            answers.put("step4_" + PARTNER, "yes");
            assertTrue(parameters.evaluate(answers));
        }
    }

    @Test
    public void enrollmentRulesUseTheDiscordantFlagAndKeepIndependentEligibility() throws Exception {
        for (String sex : List.of("male", "female")) {
            boolean male = "male".equals(sex);
            String otherGroupStep = male ? "step6" : "step7";
            String enrollmentStep = male ? "step8" : "step9";
            Rules calculation = rules(sex, "calculation");
            Rule enroll = rule(calculation, enrollmentStep + "_should_enroll");
            Rule secondaryGroups = rule(calculation, enrollmentStep + "_other_screened_client_group");
            Rule prompt = rule(rules(sex, "relevance"), enrollmentStep + "_prompt_for_sdc_kvp");
            Facts results = new Facts();
            for (String field : List.of("step1_is_pwud", "step2_is_pwid", "step4_is_sdc",
                    "step5_is_msm", "step5_is_fsw", "step6_is_agyw",
                    otherGroupStep + "_is_ovp_kvp")) {
                results.put(field, "no");
            }

            // Carried flags from older screening data cannot make the new form eligible.
            results.put(otherGroupStep + "_is_prisoner", "yes");
            results.put(otherGroupStep + "_is_rumandee", "yes");
            results.put(otherGroupStep + "_is_mobile_population", "yes");
            enroll.execute(results);
            assertEquals("no", results.get("calculation"));
            assertEquals("[]", calculateSecondaryGroups(secondaryGroups, results));
            assertFalse(prompt.evaluate(results));
            results.put("step4_is_sdc", "yes");
            enroll.execute(results);
            assertEquals("yes", results.get("calculation"));
            assertTrue(prompt.evaluate(results));
            assertTrue(calculateSecondaryGroups(secondaryGroups, results).contains("serodiscordant_couple"));

            results.put("step4_is_sdc", "no");
            results.put(otherGroupStep + "_is_ovp_kvp", "yes");
            enroll.execute(results);
            assertEquals("yes", results.get("calculation"));
            assertEquals("[other_vulnerable_population]", calculateSecondaryGroups(secondaryGroups, results));
        }
    }

    @Test
    public void emptyChecklistQueuesLocalizedFeedbackAndCorrectedNextAdvances() throws Exception {
        for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
            JSONObject form = loadForm("male", locale);
            String address = "step4:" + PARAMETERS;
            String error = findField(form, PARAMETERS).getJSONObject("v_required").getString("err");
            LinearLayout checklist = mock(LinearLayout.class);
            when(checklist.getTag(R.id.error)).thenReturn(error);
            when(checklist.getTag(R.id.is_checkbox_linear_layout)).thenReturn(true);
            when(checklist.getTag(R.id.address)).thenReturn(address);
            when(checklist.getTag(R.id.key)).thenReturn(PARAMETERS);
            when(checklist.isEnabled()).thenReturn(true);
            when(checklist.isShown()).thenReturn(true);
            when(checklist.isAttachedToWindow()).thenReturn(true);

            ConstraintLayout heading = mock(ConstraintLayout.class);
            Context context = mock(Context.class);
            when(heading.getContext()).thenReturn(context);
            when(context.getResources()).thenReturn(mock(Resources.class));
            when(checklist.getChildAt(0)).thenReturn(heading);
            when(checklist.getChildCount()).thenReturn(3);
            LinearLayout firstRow = mock(LinearLayout.class);
            LinearLayout secondRow = mock(LinearLayout.class);
            CheckBox first = mock(CheckBox.class);
            CheckBox second = mock(CheckBox.class);
            when(checklist.getChildAt(1)).thenReturn(firstRow);
            when(checklist.getChildAt(2)).thenReturn(secondRow);
            when(firstRow.getChildAt(0)).thenReturn(first);
            when(secondRow.getChildAt(0)).thenReturn(second);

            Queue<Runnable> uiTasks = new ArrayDeque<>();
            JsonApi api = mock(JsonApi.class);
            when(api.getAppExecutors()).thenReturn(new AppExecutors(Runnable::run, Runnable::run, uiTasks::add));
            when(api.getmJSONObject()).thenReturn(form);
            when(api.getFormDataView(address)).thenReturn(checklist);
            when(api.getFormDataViews()).thenReturn(List.of(checklist));
            when(api.getInvalidFields()).thenReturn(new HashMap<>());
            when(api.nextStep()).thenReturn(form.getJSONObject("step4").getString("next"));
            when(api.isNextStepRelevant()).thenReturn(true);
            KvpScreeningJsonFormFragment fragment = mock(KvpScreeningJsonFormFragment.class);
            when(fragment.getView()).thenReturn(mock(View.class));
            when(fragment.getJsonApi()).thenReturn(api);
            when(fragment.getOnFieldsInvalidCallback()).thenReturn(mock(OnFieldsInvalid.class));
            when(fragment.getCurrentJsonState()).thenReturn(form.toString());
            doCallRealMethod().when(fragment).showDiscordantChecklistError(checklist, error);
            JsonFormInteractor interactor = mock(JsonFormInteractor.class);
            KvpScreeningJsonFormFragmentPresenter presenter = new KvpScreeningJsonFormFragmentPresenter(
                    fragment, interactor) {
                {
                    mStepName = "step4";
                    mStepDetails = form.optJSONObject(mStepName);
                }
            };
            presenter.attachView(fragment);

            // Both Next controls invoke this presenter. Drain the queued UI work separately.
            assertFalse(presenter.onNextClick(mock(LinearLayout.class)));
            assertFalse(presenter.onNextClick(mock(LinearLayout.class)));
            verify(heading, never()).addView(any(View.class), any(ConstraintLayout.LayoutParams.class));
            verify(fragment, never()).scrollToView(checklist);
            verify(fragment, never()).transactThis(any());

            assertEquals(2, uiTasks.size());
            uiTasks.remove().run();
            verify(heading).addView(any(View.class), any(ConstraintLayout.LayoutParams.class));
            verify(fragment).scrollToView(checklist);

            when(first.isChecked()).thenReturn(true);
            when(second.isChecked()).thenReturn(true);
            uiTasks.remove().run();
            // The queued retry must not render or scroll after the checklist is corrected.
            verify(heading).addView(any(View.class), any(ConstraintLayout.LayoutParams.class));
            verify(fragment).scrollToView(checklist);
            assertTrue(CheckBoxFactory.isValid(checklist));

            presenter.onNextClick(mock(LinearLayout.class));
            verify(interactor).fetchFormElements(eq("step5"), eq(fragment), eq(form.getJSONObject("step5")),
                    any(), eq(false));
            verify(fragment).transactThis(any(KvpScreeningJsonFormFragment.class));
        }
    }

    private static void assertCopy(JSONObject form, String title, List<String> labels, List<String> choices,
                                   String radioError, String checklistError) throws Exception {
        JSONObject page = form.getJSONObject("step4");
        assertEquals(title, page.getString("title"));
        List<List<String>> expectedOptions = List.of(choices.subList(0, 2), choices.subList(2, 4),
                choices.subList(0, 2), choices.subList(4, choices.size()));
        List<String> keys = List.of(KNOWS_STATUS, STATUS, PARTNER, PARAMETERS);
        for (int i = 0; i < keys.size(); i++) {
            JSONObject field = page.getJSONArray("fields").getJSONObject(i);
            String key = keys.get(i);
            assertEquals(key, field.getString("key"));
            assertEquals(labels.get(i), field.getString("label"));
            JSONObject required = field.getJSONObject("v_required");
            assertTrue(required.getBoolean("value"));
            JSONArray options = field.getJSONArray("options");
            if (PARAMETERS.equals(key)) {
                assertEquals("[\"none\"]", field.getJSONArray("exclusive").toString());
                assertEquals(checklistError, required.getString("err"));
            } else {
                assertEquals(2, options.length());
                assertEquals(radioError, required.getString("err"));
                RadioGroup radio = mock(RadioGroup.class);
                when(radio.getTag(R.id.error)).thenReturn(radioError);
                when(radio.isEnabled()).thenReturn(true);
                assertFalse(NativeRadioButtonFactory.isValid(radio));
                when(radio.isEnabled()).thenReturn(false);
                assertTrue(NativeRadioButtonFactory.isValid(radio));
            }
            List<String> expected = expectedOptions.get(i);
            assertEquals(expected.size(), options.length());
            for (int j = 0; j < options.length(); j++) {
                assertEquals(expected.get(j), options.getJSONObject(j).getString("text"));
            }
        }
    }

    private static JSONObject findField(JSONObject form, String key) throws Exception {
        return org.smartregister.util.JsonFormUtils.getFieldJSONObject(
                form.getJSONObject("step4").getJSONArray("fields"), key);
    }

    private static JSONObject loadForm(String sex, Locale locale) throws Exception {
        byte[] json = Files.readAllBytes(ASSETS.resolve("json.form/kvp_screening_" + sex + ".json"));
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            return new JSONObject(NativeFormLangUtils.getTranslatedString(new String(json, StandardCharsets.UTF_8)));
        } finally {
            Locale.setDefault(previous);
        }
    }

    private static Rules rules(String sex, String type) throws Exception {
        Path path = ASSETS.resolve("rule/kvp_screening_" + sex + "_" + type + ".yml");
        try (Reader reader = Files.newBufferedReader(path)) {
            return new MVELRuleFactory(new YamlRuleDefinitionReader()).createRules(reader);
        }
    }

    private static String calculateSecondaryGroups(Rule rule, Facts answers) throws Exception {
        // RulesEngineFactory supplies fresh facts per calculation. MVEL adds the helper function to that map.
        Facts facts = new Facts();
        answers.asMap().forEach(facts::put);
        rule.execute(facts);
        return facts.get("calculation");
    }

    private static Rule rule(Rules rules, String name) {
        for (Rule rule : rules) {
            if (name.equals(rule.getName())) {
                return rule;
            }
        }
        throw new AssertionError("Missing rule " + name);
    }
}
