package org.smartregister.chw.hf.presenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.concurrent.atomic.AtomicReference;

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
    public void sexualBehaviourHasOneLocalizedRequiredQuestionWithTheExistingAnswerCodes() throws Exception {
        for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
            boolean swahili = KISWAHILI.equals(locale);
            for (String sex : List.of("male", "female")) {
                JSONObject page = loadForm(sex, locale).getJSONObject("step3");
                assertEquals(swahili ? "Tabia ya Ngono" : "Sexual Behavior", page.getString("title"));
                JSONArray fields = page.getJSONArray("fields");
                assertEquals(1, fields.length());
                JSONObject field = fields.getJSONObject(0);
                assertEquals("sexual_intercourse", field.getString("key"));
                assertEquals("sexual_intercourse", field.getString("openmrs_entity_id"));
                assertEquals("native_radio", field.getString("type"));
                assertEquals(swahili ? "Umewahi kushiriki tendo la ngono?"
                        : "Has the client ever had sexual intercourse?", field.getString("label"));
                JSONObject required = field.getJSONObject("v_required");
                assertTrue(required.getBoolean("value"));
                String error = swahili ? "Tafadhali chagua jibu" : "Please select an answer";
                assertEquals(error, required.getString("err"));
                JSONArray options = field.getJSONArray("options");
                assertEquals(2, options.length());
                JSONObject yes = options.getJSONObject(0);
                assertEquals("yes", yes.getString("key"));
                assertEquals("yes", yes.getString("openmrs_entity_id"));
                assertEquals(swahili ? "Ndio" : "Yes", yes.getString("text"));
                JSONObject no = options.getJSONObject(1);
                assertEquals("no", no.getString("key"));
                assertEquals("no", no.getString("openmrs_entity_id"));
                assertEquals(swahili ? "Hapana" : "No", no.getString("text"));

                RadioGroup radio = mock(RadioGroup.class);
                when(radio.getTag(R.id.error)).thenReturn(error);
                when(radio.isEnabled()).thenReturn(true);
                assertFalse(NativeRadioButtonFactory.isValid(radio));
            }
        }
    }

    @Test
    public void sexualBehaviourPreparesOtherGroupsForNoAndDiscordantCoupleForYes() throws Exception {
        for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
            for (String sex : List.of("male", "female")) {
                assertSexualBehaviourDestination(sex, locale, "no", "female".equals(sex) ? "step7" : "step6");
                assertSexualBehaviourDestination(sex, locale, "yes", "step4");
            }
        }
    }

    private static void assertSexualBehaviourDestination(String sex, Locale locale, String answer, String expectedStep)
            throws Exception {
        JSONObject form = loadForm(sex, locale);
        form.getJSONObject("global").put("age", 19);
        form.getJSONObject("step3").getJSONArray("fields").getJSONObject(0).put("value", answer);
        JsonApi api = mock(JsonApi.class);
        when(api.getmJSONObject()).thenReturn(form);
        when(api.isNextStepRelevant()).thenReturn(true);
        AtomicReference<String> next = new AtomicReference<>(form.getJSONObject("step3").getString("next"));
        when(api.nextStep()).thenAnswer(call -> next.get());
        doAnswer(call -> {
            next.set(call.getArgument(0));
            return null;
        }).when(api).setNextStep(any(String.class));
        KvpScreeningJsonFormFragment fragment = mock(KvpScreeningJsonFormFragment.class);
        when(fragment.getJsonApi()).thenReturn(api);
        JsonFormInteractor interactor = mock(JsonFormInteractor.class);
        KvpScreeningJsonFormFragmentPresenter presenter = new KvpScreeningJsonFormFragmentPresenter(fragment, interactor) {
            {
                mStepName = "step3";
            }
        };
        presenter.attachView(fragment);

        assertFalse(presenter.executeRefreshLogicForNextStep());
        verify(interactor).fetchFormElements(eq(expectedStep), eq(fragment), eq(form.getJSONObject(expectedStep)),
                any(), eq(false));
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
    public void enrollmentPromptUsesTheDiscordantFlagInBothForms() throws Exception {
        for (String sex : List.of("male", "female")) {
            String enrollmentStep = "male".equals(sex) ? "step8" : "step9";
            Rule prompt = rule(rules(sex, "relevance"), enrollmentStep + "_prompt_for_sdc_kvp");
            Facts answers = new Facts();
            answers.put("step4_is_sdc", "no");
            assertFalse(prompt.evaluate(answers));
            answers.put("step4_is_sdc", "yes");
            assertTrue(prompt.evaluate(answers));
        }
    }

    @Test
    public void emptyChecklistQueuesLocalizedFeedbackAndCorrectedNextAdvances() throws Exception {
        for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
            JSONObject form = loadForm("male", locale);
            assertRequiredChecklistFeedback(form, "step4", PARAMETERS);
        }
    }

    @Test
    public void otherGroupsShowsRequiredFeedbackAndAdvancesAfterCorrection() throws Exception {
        for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
            for (String sex : List.of("male", "female")) {
                assertRequiredChecklistFeedback(loadForm(sex, locale), "male".equals(sex) ? "step6" : "step7",
                        "other_groups_at_risk");
            }
        }
    }

    @Test
    public void vagywShowsRequiredFeedbackAndAdvancesAfterCorrectionInBothLanguages() throws Exception {
        for (Locale locale : List.of(Locale.ENGLISH, KISWAHILI)) {
            assertRequiredChecklistFeedback(loadForm("female", locale), "step6",
                    "harsh_condition_impact", "not_applicable", "partner_self_injects_drugs");
        }
    }

    private static void assertRequiredChecklistFeedback(JSONObject form, String step, String key) throws Exception {
        assertRequiredChecklistFeedback(form, step, key, "none",
                PARAMETERS.equals(key) ? "not_started_art" : "fisherfolk");
    }

    private static void assertRequiredChecklistFeedback(JSONObject form, String step, String key,
                                                        String exclusive, String criterion) throws Exception {
        String address = step + ":" + key;
        String error = org.smartregister.util.JsonFormUtils.getFieldJSONObject(
                form.getJSONObject(step).getJSONArray("fields"), key).getJSONObject("v_required").getString("err");
        String next = form.getJSONObject(step).getString("next");
        LinearLayout checklist = mock(LinearLayout.class);
        when(checklist.getTag(R.id.error)).thenReturn(error);
        when(checklist.getTag(R.id.is_checkbox_linear_layout)).thenReturn(true);
        when(checklist.getTag(R.id.address)).thenReturn(address);
        when(checklist.getTag(R.id.key)).thenReturn(key);
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
        when(api.nextStep()).thenReturn(next);
        when(api.isNextStepRelevant()).thenReturn(true);
        KvpScreeningJsonFormFragment fragment = mock(KvpScreeningJsonFormFragment.class);
        when(fragment.getView()).thenReturn(mock(View.class));
        when(fragment.getJsonApi()).thenReturn(api);
        when(fragment.getStep(step)).thenReturn(form.getJSONObject(step));
        when(fragment.getOnFieldsInvalidCallback()).thenReturn(mock(OnFieldsInvalid.class));
        when(fragment.getCurrentJsonState()).thenReturn(form.toString());
        doCallRealMethod().when(fragment).showChecklistError(checklist, error);
        JsonFormInteractor interactor = mock(JsonFormInteractor.class);
        KvpScreeningJsonFormFragmentPresenter presenter = new KvpScreeningJsonFormFragmentPresenter(
                fragment, interactor) {
            {
                mStepName = step;
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

        when(first.getTag(R.id.key)).thenReturn(key);
        when(first.getTag(R.id.childKey)).thenReturn(exclusive);
        when(first.isChecked()).thenReturn(true);
        presenter.onCheckedChanged(first, true);
        verify(fragment).unCheckAllExcept(key, exclusive, first);
        verify(fragment).dismissChecklistError();
        uiTasks.remove().run();
        // The queued retry must not render or scroll after the checklist is corrected.
        verify(heading).addView(any(View.class), any(ConstraintLayout.LayoutParams.class));
        verify(fragment).scrollToView(checklist);
        assertTrue(CheckBoxFactory.isValid(checklist));

        when(second.getTag(R.id.key)).thenReturn(key);
        when(second.getTag(R.id.childKey)).thenReturn(criterion);
        when(second.isChecked()).thenReturn(true);
        presenter.onCheckedChanged(second, true);
        verify(fragment).unCheck(key, exclusive, second);

        presenter.onNextClick(mock(LinearLayout.class));
        verify(interactor).fetchFormElements(eq(next), eq(fragment), eq(form.getJSONObject(next)),
                any(), eq(false));
        verify(fragment).transactThis(any(KvpScreeningJsonFormFragment.class));

        if ("harsh_condition_impact".equals(key)) {
            // School Yes hides and disables the native checklist.
            when(checklist.isShown()).thenReturn(false);
            when(checklist.isEnabled()).thenReturn(false);
            when(first.isChecked()).thenReturn(false);
            when(second.isChecked()).thenReturn(false);
            presenter.onNextClick(mock(LinearLayout.class));
            verify(fragment, times(2)).transactThis(any(KvpScreeningJsonFormFragment.class));
            assertTrue(uiTasks.isEmpty());
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

    private static Rule rule(Rules rules, String name) {
        for (Rule rule : rules) {
            if (name.equals(rule.getName())) {
                return rule;
            }
        }
        throw new AssertionError("Missing rule " + name);
    }
}
