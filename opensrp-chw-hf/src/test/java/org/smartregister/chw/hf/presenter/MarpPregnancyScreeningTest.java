package org.smartregister.chw.hf.presenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.BREASTFEEDING_FIELD;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.CHECKLIST_FIELD;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.ENROLLMENT_STEP;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.GROUP_CODE;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.HIGH_RISK_STEP;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.INTAKE_STEP;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.NO_GROUP_STEP;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.PREGNANCY_FIELD;

import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioGroup;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;
import com.vijay.jsonwizard.widgets.CheckBoxFactory;
import com.vijay.jsonwizard.widgets.NativeRadioButtonFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.kvp.util.KvpUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.domain.tag.FormTag;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MarpPregnancyScreeningTest {

    @Test
    public void onlyFemaleScreeningStartsWithIntake() throws Exception {
        JSONObject female = loadForm("kvp_screening_female", Locale.ENGLISH);
        JSONObject male = loadForm("kvp_screening_male", Locale.ENGLISH);
        assertEquals(INTAKE_STEP, MarpPregnancyScreening.firstStep(female));
        assertEquals("step1", MarpPregnancyScreening.firstStep(male));
        assertEquals("drug_use", male.getJSONObject("step1").getJSONArray("fields").getJSONObject(0).getString("key"));
        assertEquals("step1", MarpPregnancyScreening.firstStep(loadForm("prep_initiation", Locale.ENGLISH)));
        assertEquals(13, female.getInt("count"));
        assertTrue(female.has(HIGH_RISK_STEP));
        assertFalse(male.has(HIGH_RISK_STEP));
    }

    @Test
    public void routesOnlyAfterAllApplicableAnswersAreGiven() throws Exception {
        JSONObject form = loadForm("kvp_screening_female", Locale.ENGLISH);
        JSONObject pregnant = field(form, INTAKE_STEP, PREGNANCY_FIELD);
        JSONObject breastfeeding = field(form, INTAKE_STEP, BREASTFEEDING_FIELD);

        assertEquals("", MarpPregnancyScreening.nextStep(form));
        breastfeeding.put("value", "yes");
        assertEquals("", MarpPregnancyScreening.nextStep(form));
        breastfeeding.remove("value");

        pregnant.put("value", "yes");
        assertEquals(HIGH_RISK_STEP, MarpPregnancyScreening.nextStep(form));

        pregnant.put("value", "no");
        assertEquals("", MarpPregnancyScreening.nextStep(form));
        breastfeeding.put("value", new JSONObject().put("value", "yes").put("text", "Ndio"));
        assertEquals(HIGH_RISK_STEP, MarpPregnancyScreening.nextStep(form));
        breastfeeding.put("value", "no");
        assertEquals("step1", MarpPregnancyScreening.nextStep(form));

        pregnant.put("value", "yes");
        assertEquals(HIGH_RISK_STEP, MarpPregnancyScreening.nextStep(form));
    }

    @Test
    public void translatesIntakeAndRequiredErrorsInEnglish() throws Exception {
        assertIntakeCopy(Locale.ENGLISH, "Pregnancy/Breastfeeding", "Is the client pregnant?",
                "Is the client breastfeeding?", "Yes", "No", "Please select an answer");
    }

    @Test
    public void translatesIntakeAndRequiredErrorsInKiswahili() throws Exception {
        assertIntakeCopy(new Locale("sw"), "Ujauzito/Kunyonyesha", "Je, mteja ni mjamzito?",
                "Je, mteja ananyonyesha?", "Ndio", "Hapana", "Tafadhali chagua jibu");
    }

    @Test
    public void unansweredRadioIsRequiredWhenVisibleAndIgnoredWhenHidden() {
        RadioGroup radio = mock(RadioGroup.class);
        when(radio.getTag(R.id.error)).thenReturn("Please select an answer");
        when(radio.isEnabled()).thenReturn(true);
        assertFalse(NativeRadioButtonFactory.isValid(radio));
        when(radio.isEnabled()).thenReturn(false);
        assertTrue(NativeRadioButtonFactory.isValid(radio));
    }

    @Test
    public void movingToHighRiskClearsGeneralAnswersAndUpdatesWizardCache() throws Exception {
        JSONObject form = loadForm("kvp_screening_female", Locale.ENGLISH);
        JSONObject drugUse = field(form, "step1", "drug_use").put("value", "yes");
        JSONObject pwud = field(form, "step1", "is_pwud").put("value", "yes");
        JSONObject harshCondition = field(form, "step6", "harsh_condition_impact");
        JSONObject checkedOption = harshCondition.getJSONArray("options").getJSONObject(0).put("value", true);
        String checkedKey = checkedOption.getString("key");
        harshCondition.put("value", new JSONArray().put(checkedKey));
        JSONObject clientGroup = field(form, "step9", "client_group").put("value", "pwud");
        JSONObject pregnant = field(form, INTAKE_STEP, PREGNANCY_FIELD).put("value", "yes");
        JsonApi api = formApi(form);

        MarpPregnancyScreening.discardBranchAnswers(api, "step1", MarpPregnancyScreening.nextStep(form));

        assertFalse(drugUse.has("value"));
        assertFalse(pwud.has("value"));
        assertFalse(harshCondition.has("value"));
        assertFalse(checkedOption.has("value"));
        assertFalse(clientGroup.has("value"));
        assertEquals("yes", pregnant.getString("value"));
        verify(api).writeValue("step1", "drug_use", "", "", "concept", "drug_use", false);
        verify(api).writeValue("step6", "harsh_condition_impact", "options", checkedKey, "false",
                checkedOption.optString("openmrs_entity_parent"), checkedOption.optString("openmrs_entity"),
                checkedOption.optString("openmrs_entity_id"), false);
    }

    @Test
    public void leavingHighRiskClearsThatBranchAndItsEnrollmentSelection() throws Exception {
        JSONObject form = pregnantForm(Locale.ENGLISH);
        JSONObject checklist = field(form, HIGH_RISK_STEP, CHECKLIST_FIELD);
        selection(form, "polygamous_marriage").put("value", true);
        checklist.put("value", new JSONArray().put("polygamous_marriage"));
        JSONObject clientGroup = field(form, ENROLLMENT_STEP, "client_group").put("value", GROUP_CODE);
        JSONObject region = field(form, ENROLLMENT_STEP, "birth_region").put("value", "Tanga");
        field(form, INTAKE_STEP, PREGNANCY_FIELD).put("value", "no");
        JSONObject breastfeeding = field(form, INTAKE_STEP, BREASTFEEDING_FIELD).put("value", "no");
        JsonApi api = formApi(form);

        MarpPregnancyScreening.discardBranchAnswers(api, HIGH_RISK_STEP, MarpPregnancyScreening.nextStep(form));

        assertFalse(checklist.has("value"));
        assertFalse(selection(form, "polygamous_marriage").has("value"));
        assertFalse(clientGroup.has("value"));
        assertFalse(region.has("value"));
        assertEquals("no", breastfeeding.getString("value"));
        verify(api).writeValue(HIGH_RISK_STEP, CHECKLIST_FIELD, "options", "polygamous_marriage", "false",
                "", "", "", false);
    }

    @Test
    public void keepsExistingAnswersWhenTheDestinationStillApplies() throws Exception {
        JsonApi api = mock(JsonApi.class);
        MarpPregnancyScreening.discardBranchAnswers(api, HIGH_RISK_STEP, HIGH_RISK_STEP);
        verifyNoInteractions(api);
    }

    @Test
    public void presenterBlocksIncompleteIntake() throws Exception {
        JSONObject form = loadForm("kvp_screening_female", Locale.ENGLISH);
        JsonApi api = formApi(form);
        JsonFormFragment fragment = mock(JsonFormFragment.class);
        KvpScreeningJsonFormFragmentPresenter presenter = presenter(api, fragment, INTAKE_STEP);
        JSONObject pregnant = field(form, INTAKE_STEP, PREGNANCY_FIELD);

        assertTrue(presenter.executeRefreshLogicForNextStep());
        pregnant.put("value", "no");
        assertTrue(presenter.executeRefreshLogicForNextStep());
        verify(api, never()).setNextStep("step1");
        verify(api, never()).setNextStep(HIGH_RISK_STEP);
    }

    @Test
    public void presenterPreparesDrugUseForNoNoAndTheActualHighRiskPageForYes() throws Exception {
        JSONObject form = loadForm("kvp_screening_female", Locale.ENGLISH);
        JsonApi api = formApi(form);
        when(api.isNextStepRelevant()).thenReturn(true);
        KvpScreeningJsonFormFragmentPresenter presenter = presenter(api, mock(JsonFormFragment.class), INTAKE_STEP);
        JSONObject pregnant = field(form, INTAKE_STEP, PREGNANCY_FIELD).put("value", "no");
        JSONObject breastfeeding = field(form, INTAKE_STEP, BREASTFEEDING_FIELD).put("value", "no");

        when(api.nextStep()).thenReturn("step1");
        assertFalse(presenter.executeRefreshLogicForNextStep());
        verify(api).setNextStep("step1");

        pregnant.put("value", "yes");
        breastfeeding.remove("value");
        when(api.nextStep()).thenReturn(HIGH_RISK_STEP);
        assertFalse(presenter.executeRefreshLogicForNextStep());

        pregnant.put("value", "no");
        breastfeeding.put("value", "yes");
        assertFalse(presenter.executeRefreshLogicForNextStep());
        verify(api, times(2)).setNextStep(HIGH_RISK_STEP);
    }

    @Test
    public void checklistRequiresAnAnswerAndPresenterRoutesToTheTwoEnrollmentOutcomes() throws Exception {
        JSONObject form = pregnantForm(Locale.ENGLISH);
        JSONObject checklist = field(form, HIGH_RISK_STEP, CHECKLIST_FIELD);
        LinearLayout checkboxGroup = mock(LinearLayout.class);
        LinearLayout row = mock(LinearLayout.class);
        CheckBox checkbox = mock(CheckBox.class);
        when(checkboxGroup.isEnabled()).thenReturn(true);
        when(checkboxGroup.getTag(R.id.error)).thenReturn(checklist.getJSONObject("v_required").getString("err"));
        when(checkboxGroup.getChildCount()).thenReturn(2);
        when(checkboxGroup.getChildAt(1)).thenReturn(row);
        when(row.getChildAt(0)).thenReturn(checkbox);
        assertFalse(CheckBoxFactory.isValid(checkboxGroup));
        when(checkbox.isChecked()).thenReturn(true);
        assertTrue(CheckBoxFactory.isValid(checkboxGroup));

        JsonApi api = formApi(form);
        when(api.isNextStepRelevant()).thenReturn(true);
        KvpScreeningJsonFormFragmentPresenter presenter = presenter(api, mock(JsonFormFragment.class), HIGH_RISK_STEP);
        assertTrue(presenter.executeRefreshLogicForNextStep());
        verify(api, never()).setNextStep(ENROLLMENT_STEP);
        selection(form, "polygamous_marriage").put("value", true);
        when(api.nextStep()).thenReturn(ENROLLMENT_STEP);
        assertFalse(presenter.executeRefreshLogicForNextStep());
        verify(api).setNextStep(ENROLLMENT_STEP);

        selection(form, "polygamous_marriage").put("value", false);
        selection(form, "none").put("value", true);
        when(api.nextStep()).thenReturn(NO_GROUP_STEP);
        assertFalse(presenter.executeRefreshLogicForNextStep());
        verify(api).setNextStep(NO_GROUP_STEP);
    }

    @Test
    public void nativeCheckboxListenerClearsTheOpposingSelectionsInBothDirections() throws Exception {
        JSONObject form = pregnantForm(Locale.ENGLISH);
        JsonFormFragment fragment = mock(JsonFormFragment.class);
        when(fragment.getStep(HIGH_RISK_STEP)).thenReturn(form.getJSONObject(HIGH_RISK_STEP));
        KvpScreeningJsonFormFragmentPresenter presenter = presenter(formApi(form), fragment, HIGH_RISK_STEP);
        CheckBox none = checkBox("none");
        CheckBox criterion = checkBox("polygamous_marriage");

        presenter.onCheckedChanged(none, true);
        verify(fragment).unCheckAllExcept(CHECKLIST_FIELD, "none", none);
        presenter.onCheckedChanged(criterion, true);
        verify(fragment).unCheck(CHECKLIST_FIELD, "none", criterion);
    }

    @Test
    public void pregnancyEnrollmentPersistsOnlyApplicableIntakeInTheKvpEvent() throws Exception {
        JSONObject form = pregnantForm(Locale.ENGLISH);
        field(form, INTAKE_STEP, BREASTFEEDING_FIELD).put("value", "yes");
        assertEnrollmentEvent(form, "yes", null);
    }

    @Test
    public void breastfeedingEnrollmentPersistsBothApplicableIntakeAnswersInTheKvpEvent() throws Exception {
        JSONObject form = pregnantForm(new Locale("sw"));
        field(form, INTAKE_STEP, PREGNANCY_FIELD).put("value", "no");
        field(form, INTAKE_STEP, BREASTFEEDING_FIELD).put("value", "yes");
        assertEnrollmentEvent(form, "no", "yes");
    }

    @Test
    public void nonePreventsEnrollmentEvenAfterABirthRegionAndGroupWereSelected() throws Exception {
        JSONObject form = pregnantForm(Locale.ENGLISH);
        selection(form, "none").put("value", true);
        field(form, ENROLLMENT_STEP, "birth_region").put("value", "Tanga");
        field(form, ENROLLMENT_STEP, "client_group").put("value", GROUP_CODE);
        assertFalse(MarpPregnancyScreening.prepareEnrollmentResult(form, ENROLLMENT_STEP));
        assertTrue(MarpPregnancyScreening.prepareEnrollmentResult(form, NO_GROUP_STEP));
        assertFalse(KvpUtil.isClientToBeRegistered(form.toString()));
        JSONArray fields = KvpJsonFormUtils.kvpFormFields(form);
        assertNull(JsonFormUtils.getFieldJSONObject(fields, "birth_region"));
        assertNull(JsonFormUtils.getFieldJSONObject(fields, "client_group"));
    }

    @Test
    public void highRiskChecklistAndNoGroupOutcomeUseBothTranslationBundles() throws Exception {
        assertHighRiskCopy(Locale.ENGLISH, "Pregnant/Breastfeeding High Risk",
                "HIV-negative pregnant and breastfeeding women at increased risk of acquiring HIV",
                "Is in a polygamous marriage", "None",
                "The client does not fall in any of the MARP groups and won't be enrolled to MARP friendly services");
        assertHighRiskCopy(new Locale("sw"), "Mjamzito/Anayenyonyesha mwenye hatari kubwa",
                "Wajawazito na wanaonyonyesha wasio na VVU walio katika hatari kubwa ya kupata VVU",
                "Yuko kwenye ndoa ya wake wengi", "Hakuna",
                "Mteja hayuko kwenye kundi lolote la MARP na hatasajiliwa kwenye huduma rafiki za MARP");
    }

    private static KvpScreeningJsonFormFragmentPresenter presenter(JsonApi api, JsonFormFragment fragment,
                                                                  String step) {
        when(fragment.getJsonApi()).thenReturn(api);
        KvpScreeningJsonFormFragmentPresenter presenter = new KvpScreeningJsonFormFragmentPresenter(
                fragment, mock(JsonFormInteractor.class)) {
            {
                mStepName = step;
            }
        };
        presenter.attachView(fragment);
        return presenter;
    }

    private static CheckBox checkBox(String key) {
        CheckBox option = mock(CheckBox.class);
        when(option.getTag(R.id.key)).thenReturn(CHECKLIST_FIELD);
        when(option.getTag(R.id.childKey)).thenReturn(key);
        when(option.isChecked()).thenReturn(true);
        return option;
    }

    private static JSONObject pregnantForm(Locale locale) throws Exception {
        JSONObject form = loadForm("kvp_screening_female", locale);
        field(form, INTAKE_STEP, PREGNANCY_FIELD).put("value", "yes");
        return form;
    }

    private static JSONObject selection(JSONObject form, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(field(form, HIGH_RISK_STEP, CHECKLIST_FIELD).getJSONArray("options"), key);
    }

    private static void assertEnrollmentEvent(JSONObject form, String pregnancy, String breastfeeding) throws Exception {
        selection(form, "polygamous_marriage").put("value", true);
        field(form, ENROLLMENT_STEP, "client_group").put("value", GROUP_CODE);
        assertFalse(MarpPregnancyScreening.prepareEnrollmentResult(form, ENROLLMENT_STEP));
        field(form, ENROLLMENT_STEP, "birth_region").put("value", "Tanga");
        assertTrue(MarpPregnancyScreening.prepareEnrollmentResult(form, ENROLLMENT_STEP));
        assertTrue(KvpUtil.isClientToBeRegistered(form.toString()));

        JSONArray fields = KvpJsonFormUtils.kvpFormFields(form);
        Event event = JsonFormUtils.createEvent(fields, form.getJSONObject("metadata"), new FormTag(),
                "synthetic-client", "KVP Registration", "KVP Registration");
        assertObservation(event, "birth_region", "Tanga");
        assertObservation(event, "client_group", GROUP_CODE);
        assertObservation(event, PREGNANCY_FIELD, pregnancy);
        assertObservation(event, BREASTFEEDING_FIELD, breastfeeding);
        assertObservation(event, CHECKLIST_FIELD, null);
    }

    private static void assertObservation(Event event, String field, String expected) {
        List<Obs> matches = new ArrayList<>();
        for (Obs obs : event.getObs()) {
            if (field.equals(obs.getFieldCode())) {
                matches.add(obs);
            }
        }
        assertEquals(field, expected == null ? 0 : 1, matches.size());
        if (expected != null) {
            assertEquals(List.of(expected), matches.get(0).getValues());
        }
    }

    private static void assertHighRiskCopy(Locale locale, String title, String label, String criterion,
                                           String none, String noGroupMessage) throws Exception {
        JSONObject form = pregnantForm(locale);
        assertEquals(title, form.getJSONObject(HIGH_RISK_STEP).getString("title"));
        JSONObject checklist = field(form, HIGH_RISK_STEP, CHECKLIST_FIELD);
        assertEquals(label, checklist.getString("label"));
        assertTrue(checklist.getJSONObject("v_required").getBoolean("value"));
        assertEquals(new JSONArray().put("none").toString(), checklist.getJSONArray("exclusive").toString());
        JSONArray options = checklist.getJSONArray("options");
        assertEquals(15, options.length());
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            assertTrue(keys.add(option.getString("key")));
            assertFalse(option.getString("text").contains("{{"));
        }
        assertEquals(criterion, selection(form, "polygamous_marriage").getString("text"));
        assertEquals(none, selection(form, "none").getString("text"));
        assertEquals(noGroupMessage, field(form, NO_GROUP_STEP, "prompt_no_group").getString("text"));
        assertEquals(1, form.getJSONObject(NO_GROUP_STEP).getJSONArray("fields").length());
    }

    private static void assertIntakeCopy(Locale locale, String title, String pregnancy, String breastfeeding,
                                         String yes, String no, String error) throws Exception {
        JSONObject form = loadForm("kvp_screening_female", locale);
        assertEquals(title, form.getJSONObject(INTAKE_STEP).getString("title"));
        JSONObject pregnantField = field(form, INTAKE_STEP, PREGNANCY_FIELD);
        JSONObject breastfeedingField = field(form, INTAKE_STEP, BREASTFEEDING_FIELD);
        assertRequiredRadio(pregnantField, pregnancy, yes, no, error);
        assertRequiredRadio(breastfeedingField, breastfeeding, yes, no, error);
        assertFalse(pregnantField.has("relevance"));
        JSONObject relevance = breastfeedingField.getJSONObject("relevance")
                .getJSONObject(INTAKE_STEP + ":" + PREGNANCY_FIELD);
        assertEquals("equalTo(., \"no\")", relevance.getString("ex"));
    }

    private static void assertRequiredRadio(JSONObject field, String label, String yes, String no,
                                            String error) throws Exception {
        assertEquals("native_radio", field.getString("type"));
        assertEquals(label, field.getString("label"));
        assertTrue(field.getJSONObject("v_required").getBoolean("value"));
        assertEquals(error, field.getJSONObject("v_required").getString("err"));
        assertFalse(field.has("value"));
        JSONArray options = field.getJSONArray("options");
        assertEquals(2, options.length());
        assertEquals("yes", options.getJSONObject(0).getString("key"));
        assertEquals(yes, options.getJSONObject(0).getString("text"));
        assertEquals("no", options.getJSONObject(1).getString("key"));
        assertEquals(no, options.getJSONObject(1).getString("text"));
    }

    private static JsonApi formApi(JSONObject form) {
        JsonApi api = mock(JsonApi.class);
        when(api.getmJSONObject()).thenReturn(form);
        return api;
    }

    private static JSONObject field(JSONObject form, String step, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(form.getJSONObject(step).getJSONArray("fields"), key);
    }

    private static JSONObject loadForm(String name, Locale locale) throws Exception {
        byte[] json = Files.readAllBytes(Paths.get("src", "main", "assets", "json.form", name + ".json"));
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            return new JSONObject(NativeFormLangUtils.getTranslatedString(new String(json, StandardCharsets.UTF_8)));
        } finally {
            Locale.setDefault(previous);
        }
    }
}
