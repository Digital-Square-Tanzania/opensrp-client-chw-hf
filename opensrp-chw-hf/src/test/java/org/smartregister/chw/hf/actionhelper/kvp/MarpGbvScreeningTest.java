package org.smartregister.chw.hf.actionhelper.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import androidx.arch.core.executor.ArchTaskExecutor;
import androidx.arch.core.executor.TaskExecutor;

import com.google.gson.Gson;
import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.activities.JsonFormActivity;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.interfaces.OnFieldsInvalid;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;
import com.vijay.jsonwizard.utils.FormUtils;
import com.vijay.jsonwizard.widgets.CheckBoxFactory;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.YamlRuleDefinitionReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.sync.HfClientProcessor;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.jsonmapping.Column;
import org.smartregister.util.JsonFormUtils;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MarpGbvScreeningTest {
    private static final Path ASSETS = Paths.get("src/main/assets");
    private static final String SCREENING = "gbv_screening";
    private static final String CARE = "post_gbv_care_provision";
    private static final String PHYSICAL = "physical_violence";
    private static final String SEXUAL = "sexual_violence";
    private static final String EMOTIONAL = "emotional_violence";
    private static final String NONE = "none";
    private static final String NOT_ASSESSED = "not_assessed";

    @Test
    public void nativeChecklistRejectsAnEmptySelectionAndAcceptsACheckedOption() throws Exception {
        JSONObject screening = field(form(), SCREENING);
        JSONObject required = screening.getJSONObject("v_required");
        assertEquals("check_box", screening.getString("type"));
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
    public void nativeListenerClearsAllOpposingChoicesForBothExclusiveAnswers() throws Exception {
        JSONObject step = form().getJSONObject("step1");
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

        for (String exclusive : List.of(NONE, NOT_ASSESSED)) {
            CheckBox option = checkedOption(exclusive);
            presenter.onCheckedChanged(option, true);
            verify(fragment).unCheckAllExcept(SCREENING, exclusive, option);
        }
        for (String violence : List.of(PHYSICAL, SEXUAL, EMOTIONAL)) {
            CheckBox option = checkedOption(violence);
            presenter.onCheckedChanged(option, true);
            verify(fragment).unCheck(SCREENING, NONE, option);
            verify(fragment).unCheck(SCREENING, NOT_ASSESSED, option);
        }
    }

    @Test
    public void postGbvCareIsRelevantOnlyForReportedViolence() throws Exception {
        Rule rule = careRelevance(field(form(), CARE));
        assertEquals("step1_" + CARE, rule.getName());
        for (List<String> selection : List.of(List.of(NONE), List.of(NOT_ASSESSED), List.<String>of())) {
            assertFalse(rule.evaluate(factsFor(selection)));
        }
        for (List<String> selection : List.of(List.of(PHYSICAL), List.of(SEXUAL), List.of(EMOTIONAL),
                List.of(PHYSICAL, SEXUAL))) {
            assertTrue(rule.evaluate(factsFor(selection)));
        }
    }

    @Test
    public void blankCareFailsNativeSaveValidationForEmotionalViolence() throws Exception {
        JSONObject form = form();
        JSONObject care = field(form, CARE);
        JSONObject required = care.getJSONObject("v_required");
        assertEquals("native_radio", care.getString("type"));
        assertTrue(required.getBoolean("value"));

        RadioGroup radio = mock(RadioGroup.class);
        LinearLayout canvas = mock(LinearLayout.class);
        when(radio.getTag(R.id.key)).thenReturn(CARE);
        when(radio.getTag(R.id.address)).thenReturn("step1:" + CARE);
        when(radio.getTag(R.id.error)).thenReturn(required.getString("err"));
        when(radio.isEnabled()).thenReturn(true);
        when(radio.getParent()).thenReturn(canvas);
        when(canvas.isShown()).thenReturn(careRelevance(care).evaluate(factsFor(List.of(EMOTIONAL))));

        JsonFormFragment fragment = mock(JsonFormFragment.class);
        JsonApi api = mock(JsonApi.class);
        when(fragment.getContext()).thenReturn(mock(JsonFormActivity.class));
        when(fragment.getJsonApi()).thenReturn(api);
        when(fragment.getOnFieldsInvalidCallback()).thenReturn(mock(OnFieldsInvalid.class));
        when(api.getFormDataViews()).thenReturn(List.of(radio));
        when(api.getInvalidFields()).thenReturn(new HashMap<>());
        JsonFormFragmentPresenter presenter = new JsonFormFragmentPresenter(fragment, mock(JsonFormInteractor.class)) {
            {
                mStepName = "step1";
                mStepDetails = form.optJSONObject(mStepName);
            }
        };
        presenter.attachView(fragment);

        presenter.validateAndWriteValues();
        assertFalse(presenter.isFormValid());
        assertEquals(required.getString("err"), presenter.getInvalidFields().values().iterator().next().getErrorMessage());
    }

    @Test
    public void nativeRelevanceRetainsCareUntilTheLastViolenceTypeIsRemoved() throws Exception {
        JSONObject editedForm = form();
        JSONObject care = field(editedForm, CARE);
        Rule rule = careRelevance(care);
        class CareFormActivity extends JsonFormActivity {
            @Override
            public JSONObject getObjectUsingAddress(String[] address, boolean popup) {
                return care;
            }

            void updateSelection(View view, List<String> selection) throws JSONException {
                setSelection(editedForm, selection);
                toggleViewVisibility(view, rule.evaluate(factsFor(selection)), false);
            }
        }
        TaskExecutor mainThread = mock(TaskExecutor.class);
        when(mainThread.isMainThread()).thenReturn(true);
        ArchTaskExecutor executor = ArchTaskExecutor.getInstance();
        executor.setDelegate(mainThread);
        CareFormActivity wizard;
        try {
            wizard = new CareFormActivity();
        } finally {
            executor.setDelegate(null);
        }
        RadioGroup radio = mock(RadioGroup.class);
        LinearLayout canvas = mock(LinearLayout.class);
        when(radio.getTag(R.id.address)).thenReturn("step1:" + CARE);
        when(radio.getTag(R.id.canvas_ids)).thenReturn("[1]");
        when(radio.getRootView()).thenReturn(canvas);
        when(canvas.findViewById(1)).thenReturn(canvas);

        for (List<String> hidden : List.of(List.<String>of(), List.of(NONE), List.of(NOT_ASSESSED))) {
            wizard.updateSelection(radio, List.of(PHYSICAL, EMOTIONAL));
            care.put("value", "yes");
            wizard.updateSelection(radio, List.of(EMOTIONAL));
            assertTrue(care.getBoolean("is_visible"));
            assertEquals(List.of("yes"), observation(event(editedForm), CARE).getValues());

            wizard.updateSelection(radio, hidden);
            assertFalse(care.getBoolean("is_visible"));
            assertFalse(care.has("value"));
            assertNull(observation(event(editedForm), CARE));
        }
    }

    @Test
    public void onlyValidSelectionsCompleteTheActionAndInapplicableCareIsOmitted() throws Exception {
        BaseKvpVisitAction action = open(Map.of());
        for (List<String> invalid : List.of(List.<String>of(), List.of(NONE, SEXUAL),
                List.of(NOT_ASSESSED, EMOTIONAL), List.of(NONE, NOT_ASSESSED))) {
            save(action, invalid, null);
            assertEquals(Status.PENDING, action.getActionStatus());
        }
        for (String exclusive : List.of(NONE, NOT_ASSESSED)) {
            for (String care : new String[]{null, "yes"}) {
                save(action, List.of(exclusive), care);
                assertEquals(Status.COMPLETED, action.getActionStatus());
                Event event = event(new JSONObject(action.getJsonPayload()));
                assertEquals(List.of(exclusive), observation(event, SCREENING).getValues());
                assertNull(observation(event, CARE));

                BaseKvpVisitAction restored = open(details(event));
                assertEquals(Status.COMPLETED, restored.getActionStatus());
                assertRestored(restored, List.of(exclusive));
            }
        }
    }

    @Test
    public void legacyAndNewObservationsRoundTripThroughTheVisitHistoryForm() throws Exception {
        for (String key : List.of(PHYSICAL, SEXUAL, EMOTIONAL, NONE)) {
            JSONObject legacy = form();
            field(legacy, SCREENING).put("type", "native_radio").put("value", key);
            if (!NONE.equals(key)) {
                field(legacy, CARE).put("value", "no");
            }
            Event original = event(legacy);
            BaseKvpVisitAction restored = open(details(original));
            assertEquals(Status.COMPLETED, restored.getActionStatus());
            assertRestored(restored, List.of(key));
            Event resaved = event(new JSONObject(restored.getJsonPayload()));
            assertEquals(List.of(key), observation(resaved, SCREENING).getValues());
            if (!NONE.equals(key)) {
                assertEquals(List.of("no"), observation(resaved, CARE).getValues());
            }
        }

        BaseKvpVisitAction action = open(Map.of());
        save(action, List.of(PHYSICAL, SEXUAL), "yes");
        assertEquals(Status.COMPLETED, action.getActionStatus());
        Event saved = event(new JSONObject(action.getJsonPayload()));
        assertEquals(List.of(PHYSICAL, SEXUAL), observation(saved, SCREENING).getValues());
        BaseKvpVisitAction restored = open(details(saved));
        assertRestored(restored, List.of(PHYSICAL, SEXUAL));
        assertEquals("yes", field(new JSONObject(restored.getJsonPayload()), CARE).getString("value"));
    }

    @Test
    public void facilityStructuralProjectionCanDistinguishBothOutcomesAndRetainsAllSelectedTypes() throws Exception {
        JSONObject mappings = asset("ec_client_fields.json");
        JSONObject table = find(mappings.getJSONArray("bindobjects"), "name", "ec_kvp_structural_services");
        Gson gson = new Gson();
        Column column = gson.fromJson(find(table.getJSONArray("columns"), "column_name", SCREENING).toString(), Column.class);
        HfClientProcessor processor = mock(HfClientProcessor.class, CALLS_REAL_METHODS);
        BaseKvpVisitAction action = open(Map.of());
        for (List<String> selection : List.of(List.of(NONE), List.of(NOT_ASSESSED), List.of(PHYSICAL, SEXUAL))) {
            save(action, selection, null);
            Event saved = event(new JSONObject(action.getJsonPayload()));
            ContentValues values = mock(ContentValues.class);
            org.smartregister.domain.Event synced = gson.fromJson(gson.toJson(saved), org.smartregister.domain.Event.class);
            processor.processCaseModel(synced, null, column, values);
            String projected = selection.size() == 1 ? selection.get(0) : selection.toString();
            verify(values).put(SCREENING, projected);
        }
    }

    private static Rule careRelevance(JSONObject care) throws Exception {
        String filename = care.getJSONObject("relevance").getJSONObject("rules-engine")
                .getJSONObject("ex-rules").getString("rules-file");
        try (Reader reader = Files.newBufferedReader(ASSETS.resolve("rule").resolve(filename))) {
            return new MVELRuleFactory(new YamlRuleDefinitionReader()).createRules(reader).iterator().next();
        }
    }

    private static Facts factsFor(List<String> selection) {
        Facts facts = new Facts();
        facts.put("step1_" + SCREENING, selection);
        return facts;
    }

    private static CheckBox checkedOption(String key) {
        CheckBox option = mock(CheckBox.class);
        when(option.getTag(R.id.key)).thenReturn(SCREENING);
        when(option.getTag(R.id.childKey)).thenReturn(key);
        when(option.isChecked()).thenReturn(true);
        return option;
    }

    private static void save(BaseKvpVisitAction action, List<String> selection, String care) throws Exception {
        JSONObject form = form();
        setSelection(form, selection);
        field(form, CARE).put("value", care);
        action.setJsonPayload(form.toString());
    }

    private static void setSelection(JSONObject form, List<String> selection) throws JSONException {
        JSONObject screening = field(form, SCREENING).put("value", new JSONArray(selection));
        JSONArray options = screening.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            option.put("value", selection.contains(option.getString("key")));
        }
    }

    private static BaseKvpVisitAction open(Map<String, List<VisitDetail>> details) throws Exception {
        JSONObject form = form();
        KvpJsonFormUtils.populateForm(form, details);
        return new BaseKvpVisitAction.Builder(mock(Context.class), "GBV Screening")
                .withFormName(Constants.KVP_STRUCTURAL_AND_OTHER_SERVICES_FORMS.KVP_GBV_ANALYSIS)
                .withJsonPayload(form.toString())
                .withDetails(details)
                .withHelper(new KvpGbvAnalysisActionHelper())
                .build();
    }

    private static void assertRestored(BaseKvpVisitAction action, List<String> selection) throws Exception {
        JSONObject screening = field(new JSONObject(action.getJsonPayload()), SCREENING);
        assertEquals(new JSONArray(selection).toString(), screening.getJSONArray("value").toString());
        JSONArray options = screening.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            assertEquals(selection.contains(option.getString("key")), option.optBoolean("value"));
        }
    }

    private static Event event(JSONObject form) throws JSONException {
        Event event = new Event();
        event.setEventType(Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT);
        JsonFormUtils.addObservation(event, field(form, SCREENING));
        JsonFormUtils.addObservation(event, field(form, CARE));
        return event;
    }

    private static Map<String, List<VisitDetail>> details(Event event) throws Exception {
        return NCUtil.eventsObsToDetails(event.getObs(), "synthetic-visit", "synthetic-client");
    }

    private static Obs observation(Event event, String key) {
        List<Obs> observations = event.getObs();
        return observations == null ? null : observations.stream()
                .filter(obs -> key.equals(obs.getFormSubmissionField())).findFirst().orElse(null);
    }

    private static JSONObject form() throws Exception {
        return asset("json.form/kvp_gbv_analysis.json");
    }

    private static JSONObject asset(String name) throws Exception {
        return new JSONObject(new String(Files.readAllBytes(ASSETS.resolve(name)), StandardCharsets.UTF_8));
    }

    private static JSONObject field(JSONObject form, String key) throws JSONException {
        return FormUtils.getFieldFromForm(form, key);
    }

    private static JSONObject find(JSONArray items, String property, String value) throws Exception {
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (value.equals(item.getString(property))) {
                return item;
            }
        }
        throw new AssertionError("Missing facility mapping for " + value);
    }
}
