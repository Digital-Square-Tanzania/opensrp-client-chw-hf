package org.smartregister.chw.hf.actionhelper.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;
import com.vijay.jsonwizard.utils.FormUtils;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;
import com.vijay.jsonwizard.widgets.CheckBoxFactory;

import org.jeasy.rules.api.Facts;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.repository.AllSharedPreferences;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

public class MarpHealthEducationTest {
    private static final String FORM = Constants.KVP_BEHAVIORAL_SERVICE_FORMS.KVP_HEALTH_EDUCATION;
    private static final String TOPICS = "health_education_provided";
    private static final String OTHER_TEXT = "others_health_education_provided";
    private static final String NONE = "none";
    private static final String OTHER = "other";
    private static final String SYNTHETIC_TEXT = "Education on a synthetic topic, with a follow-up";
    private static final List<String> LEGACY_TOPICS = List.of("sti", "hbv_hcv", "tb", "gbv", "pep", "vmmc",
            "cacx", "drug_disorder", "hiv_aids", OTHER);
    private static final List<String> TOPIC_ORDER = List.of("sti", "hbv_hcv", "tb", "gbv", "prep", "pep", "vmmc",
            "cacx", "drug_disorder", "hiv_aids", OTHER, NONE);

    @Test
    public void displaysTheEnglishRegisterWordingAndOrder() throws Exception {
        assertWording(Locale.ENGLISH, "values", "Health Education", "Health education",
                List.of("STI", "HBV & HCV", "TB", "GBV", "PrEP", "PEP", "VMMC", "CaCx", "Drug use disorders",
                        "HIV and AIDS", "Other (Specify)", "None"),
                "Specify other health education provided");
    }

    @Test
    public void displaysTheKiswahiliRegisterWordingAndOrder() throws Exception {
        assertWording(new Locale("sw"), "values-sw", "Elimu ya afya", "Elimu ya Afya zilizotolewa",
                List.of("Magonjwa ya Ngono", "Homa ya Ini B&C", "Kifua Kikuu", "Ukatili wa kijinsia", "PrEP",
                        "PEP", "Huduma za Tohara", "Saratani ya Shingo ya Kizazi", "Athari za dawa za kulevya",
                        "VVU na UKIMWI", "Nyinginezo; taja", "Hakuna"),
                "Taja elimu nyingine ya afya iliyotolewa");
    }

    @Test
    public void nativeChecklistRequiresAtLeastOneChoice() throws Exception {
        JSONObject topics = field(form(Locale.ENGLISH), TOPICS);
        assertEquals("check_box", topics.getString("type"));
        JSONObject required = topics.getJSONObject("v_required");
        assertTrue(required.getBoolean("value"));

        LinearLayout group = mock(LinearLayout.class);
        LinearLayout optionLayout = mock(LinearLayout.class);
        CheckBox option = mock(CheckBox.class);
        when(group.getTag(R.id.error)).thenReturn(required.getString("err"));
        when(group.isEnabled()).thenReturn(true);
        when(group.getChildCount()).thenReturn(2);
        when(group.getChildAt(1)).thenReturn(optionLayout);
        when(optionLayout.getChildAt(0)).thenReturn(option);
        assertFalse(CheckBoxFactory.isValid(group));
        when(option.isChecked()).thenReturn(true);
        assertTrue(CheckBoxFactory.isValid(group));
    }

    @Test
    public void nativePresenterClearsAllTopicsForNoneAndClearsNoneForEveryTopic() throws Exception {
        JSONObject step = form(Locale.ENGLISH).getJSONObject("step1");
        JsonFormFragment fragment = mock(JsonFormFragment.class);
        JsonApi api = mock(JsonApi.class);
        when(api.getInvalidFields()).thenReturn(new HashMap<>());
        when(fragment.getJsonApi()).thenReturn(api);
        when(fragment.getStep("step1")).thenReturn(step);
        JsonFormFragmentPresenter presenter = new JsonFormFragmentPresenter(fragment, mock(JsonFormInteractor.class)) {
            {
                mStepName = "step1";
            }
        };
        presenter.attachView(fragment);

        CheckBox none = checkedOption(NONE);
        presenter.onCheckedChanged(none, true);
        verify(fragment).unCheckAllExcept(TOPICS, NONE, none);
        for (String topic : TOPIC_ORDER.subList(0, TOPIC_ORDER.size() - 1)) {
            CheckBox option = checkedOption(topic);
            presenter.onCheckedChanged(option, true);
            verify(fragment).unCheck(TOPICS, NONE, option);
        }
    }

    @Test
    public void nativeFormShowsTheRequiredTextOnlyForOther() throws Exception {
        JSONObject form = form(Locale.ENGLISH);
        assertEquals(2, form.getJSONObject("step1").getJSONArray("fields").length());
        JSONObject text = field(form, OTHER_TEXT);
        assertTrue(text.getJSONObject("v_required").getBoolean("value"));
        JSONObject relevance = text.getJSONObject("relevance").getJSONObject("step1:" + TOPICS);

        // Use the native form engine's checkbox relevance evaluator with its per-option facts.
        Method isRelevant = JsonFormActivity.class.getDeclaredMethod("isRelevant", Facts.class, JSONObject.class);
        isRelevant.setAccessible(true);
        JsonFormActivity activity = mock(JsonFormActivity.class, CALLS_REAL_METHODS);
        Facts facts = new Facts();
        facts.put(OTHER, "false");
        facts.put(NONE, "true");
        assertEquals(false, isRelevant.invoke(activity, facts, relevance));

        facts.put(NONE, "false");
        facts.put("sti", "true");
        assertEquals(false, isRelevant.invoke(activity, facts, relevance));

        facts.put(OTHER, "true");
        assertEquals(true, isRelevant.invoke(activity, facts, relevance));
    }

    @Test
    public void actionNeedsAValidChoiceAndNonblankOtherTextWhenApplicable() throws Exception {
        BaseKvpVisitAction action = open(Map.of(), Locale.ENGLISH);
        for (List<String> invalid : List.of(List.<String>of(), List.of(NONE, "sti"), List.of(NONE, OTHER))) {
            action.setJsonPayload(answer(form(Locale.ENGLISH), invalid, SYNTHETIC_TEXT).toString());
            assertEquals(Status.PENDING, action.getActionStatus());
        }
        for (String invalid : new String[]{null, "", "   "}) {
            action.setJsonPayload(answer(form(Locale.ENGLISH), List.of(OTHER), invalid).toString());
            assertEquals(Status.PENDING, action.getActionStatus());
        }
        action.setJsonPayload(answer(form(Locale.ENGLISH), List.of(OTHER, "sti"), SYNTHETIC_TEXT).toString());
        assertEquals(Status.COMPLETED, action.getActionStatus());
        assertEquals(List.of(SYNTHETIC_TEXT), observation(event(action.getJsonPayload()), OTHER_TEXT).getValues());
    }

    @Test
    public void legacyCombinedTopicsAndOtherTextSurviveHistoryAndResaveInAnotherLanguage() throws Exception {
        JSONObject legacy = form(Locale.ENGLISH);
        JSONArray options = field(legacy, TOPICS).getJSONArray("options");
        options.remove(TOPIC_ORDER.indexOf(NONE));
        options.remove(TOPIC_ORDER.indexOf("prep"));
        options.getJSONObject(LEGACY_TOPICS.indexOf("hbv_hcv")).put("text", "HBV and HCV");
        options.getJSONObject(LEGACY_TOPICS.indexOf("drug_disorder")).put("text", "Drug use disorder");
        options.getJSONObject(LEGACY_TOPICS.indexOf(OTHER)).put("text", "Other(Please specify)");

        Event saved = event(answer(legacy, LEGACY_TOPICS, SYNTHETIC_TEXT).toString());
        assertObservations(saved, LEGACY_TOPICS, SYNTHETIC_TEXT);
        BaseKvpVisitAction restored = reopenAndAssert(saved, LEGACY_TOPICS, SYNTHETIC_TEXT);
        assertObservations(event(restored.getJsonPayload()), LEGACY_TOPICS, SYNTHETIC_TEXT);
    }

    @Test
    public void prepAndPepSaveTogetherAndRestoreAsDistinctTopicsInHistory() throws Exception {
        BaseKvpVisitAction action = open(Map.of(), Locale.ENGLISH);
        List<String> selection = List.of("prep", "pep");
        action.setJsonPayload(answer(form(Locale.ENGLISH), selection, null).toString());
        assertEquals(Status.COMPLETED, action.getActionStatus());
        Event saved = event(action.getJsonPayload());
        assertObservations(saved, selection, null);
        BaseKvpVisitAction restored = reopenAndAssert(saved, selection, null);
        assertObservations(event(restored.getJsonPayload()), selection, null);
    }

    @Test
    public void switchingFromOtherToNoneDropsStaleTextAndNoneRestoresAlone() throws Exception {
        BaseKvpVisitAction action = open(Map.of(), Locale.ENGLISH);
        action.setJsonPayload(answer(form(Locale.ENGLISH), List.of("sti", OTHER), SYNTHETIC_TEXT).toString());
        assertEquals(Status.COMPLETED, action.getActionStatus());
        JSONObject previous = new JSONObject(action.getJsonPayload());
        action.setJsonPayload(answer(previous, List.of(NONE), SYNTHETIC_TEXT).toString());
        assertEquals(Status.COMPLETED, action.getActionStatus());
        assertFalse(field(new JSONObject(action.getJsonPayload()), OTHER_TEXT).has("value"));

        Event saved = event(action.getJsonPayload());
        assertObservations(saved, List.of(NONE), null);
        BaseKvpVisitAction restored = reopenAndAssert(saved, List.of(NONE), null);
        assertObservations(event(restored.getJsonPayload()), List.of(NONE), null);
    }

    private static void assertWording(Locale locale, String resourceDirectory, String title, String question,
                                      List<String> labels, String hint) throws Exception {
        JSONObject form = form(locale);
        assertEquals("KVP Health Education", form.getString("encounter_type"));
        assertEquals(title, form.getJSONObject("step1").getString("title"));
        JSONObject topics = field(form, TOPICS);
        assertEquals(TOPICS, topics.getString("openmrs_entity_id"));
        assertEquals(question, topics.getString("label"));
        JSONArray options = topics.getJSONArray("options");
        assertEquals(TOPIC_ORDER.size(), options.length());
        for (int i = 0; i < TOPIC_ORDER.size(); i++) {
            JSONObject option = options.getJSONObject(i);
            assertEquals(TOPIC_ORDER.get(i), option.getString("key"));
            assertEquals(TOPIC_ORDER.get(i), option.getString("openmrs_entity_id"));
            assertEquals(labels.get(i), option.getString("text"));
        }
        JSONObject otherText = field(form, OTHER_TEXT);
        assertEquals(OTHER_TEXT, otherText.getString("openmrs_entity_id"));
        assertEquals(hint, otherText.getString("hint"));

        assertEquals(title, XPathFactory.newInstance().newXPath().evaluate("/resources/string[@name='" + FORM + "']",
                DocumentBuilderFactory.newInstance().newDocumentBuilder()
                        .parse(Paths.get("src/main/res", resourceDirectory, "strings.xml").toFile())));
    }

    private static CheckBox checkedOption(String key) {
        CheckBox option = mock(CheckBox.class);
        when(option.getTag(R.id.key)).thenReturn(TOPICS);
        when(option.getTag(R.id.childKey)).thenReturn(key);
        when(option.isChecked()).thenReturn(true);
        return option;
    }

    private static JSONObject answer(JSONObject form, List<String> selection, String otherText) throws JSONException {
        JSONObject topics = field(form, TOPICS).put("value", new JSONArray(selection));
        JSONArray options = topics.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            option.put("value", String.valueOf(selection.contains(option.getString("key"))));
        }
        field(form, OTHER_TEXT).put("value", otherText);
        return form;
    }

    private static BaseKvpVisitAction open(Map<String, List<VisitDetail>> details, Locale locale) throws Exception {
        JSONObject form = form(locale);
        KvpJsonFormUtils.populateForm(form, details);
        return new BaseKvpVisitAction.Builder(mock(Context.class), "Health Education")
                .withFormName(FORM)
                .withJsonPayload(form.toString())
                .withDetails(details)
                .withHelper(new KvpHealthEducationHelper())
                .build();
    }

    private static BaseKvpVisitAction reopenAndAssert(Event saved, List<String> topics, String text) throws Exception {
        BaseKvpVisitAction restored = open(NCUtil.eventsObsToDetails(saved.getObs(), "synthetic-visit", "synthetic-client"),
                new Locale("sw"));
        assertEquals(Status.COMPLETED, restored.getActionStatus());
        JSONObject form = new JSONObject(restored.getJsonPayload());
        JSONObject restoredTopics = field(form, TOPICS);
        assertEquals(new JSONArray(topics).toString(), restoredTopics.getJSONArray("value").toString());
        JSONArray options = restoredTopics.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            assertEquals(topics.contains(option.getString("key")), option.optBoolean("value"));
        }
        assertEquals(text, field(form, OTHER_TEXT).optString("value", null));
        return restored;
    }

    private static Event event(String payload) {
        KvpLibrary.init(mock(org.smartregister.Context.class), null, 67, 32);
        return KvpJsonFormUtils.processVisitJsonForm(mock(AllSharedPreferences.class), "synthetic-client",
                Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT, Map.of("Health Education", payload), Constants.TABLES.KVP_FOLLOW_UP);
    }

    private static void assertObservations(Event event, List<String> topics, String text) {
        assertEquals(Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT, event.getEventType());
        assertEquals(List.of(topics), event.getObs().stream()
                .filter(obs -> TOPICS.equals(obs.getFormSubmissionField()))
                .map(Obs::getValues).collect(Collectors.toList()));
        Obs other = observation(event, OTHER_TEXT);
        if (text == null) {
            assertNull(other);
        } else {
            assertEquals(List.of(text), other.getValues());
        }
    }

    private static Obs observation(Event event, String key) {
        return event.getObs().stream().filter(obs -> key.equals(obs.getFormSubmissionField())).findFirst().orElse(null);
    }

    private static JSONObject form(Locale locale) throws Exception {
        String json = new String(Files.readAllBytes(Paths.get("src/main/assets/json.form", FORM + ".json")),
                StandardCharsets.UTF_8);
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            return new JSONObject(NativeFormLangUtils.getTranslatedString(json));
        } finally {
            Locale.setDefault(original);
        }
    }

    private static JSONObject field(JSONObject form, String key) throws JSONException {
        return FormUtils.getFieldFromForm(form, key);
    }
}
