package org.smartregister.chw.hf.presenter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.BREASTFEEDING_FIELD;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.HIGH_RISK_STEP;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.INTAKE_STEP;
import static org.smartregister.chw.hf.utils.MarpPregnancyScreening.PREGNANCY_FIELD;

import android.widget.RadioGroup;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.AppExecutors;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;
import com.vijay.jsonwizard.widgets.NativeRadioButtonFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

public class MarpPregnancyScreeningTest {

    @Test
    public void onlyFemaleScreeningStartsWithIntake() throws Exception {
        JSONObject female = loadForm("kvp_screening_female", Locale.ENGLISH);
        JSONObject male = loadForm("kvp_screening_male", Locale.ENGLISH);
        assertEquals(INTAKE_STEP, MarpPregnancyScreening.firstStep(female));
        assertEquals("step1", MarpPregnancyScreening.firstStep(male));
        assertEquals("drug_use", male.getJSONObject("step1").getJSONArray("fields").getJSONObject(0).getString("key"));
        assertEquals("step1", MarpPregnancyScreening.firstStep(loadForm("prep_initiation", Locale.ENGLISH)));
        assertTrue(female.getInt("count") >= 10);
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
        JSONObject form = loadForm("kvp_screening_female", Locale.ENGLISH);
        JSONObject checklist = addHighRiskFixture(form);
        checklist.getJSONArray("options").getJSONObject(0).put("value", true);
        checklist.put("value", new JSONArray().put("criterion"));
        JSONObject clientGroup = field(form, "step9", "client_group").put("value", "high_risk_group");
        field(form, INTAKE_STEP, PREGNANCY_FIELD).put("value", "no");
        field(form, INTAKE_STEP, BREASTFEEDING_FIELD).put("value", "no");
        JsonApi api = formApi(form);

        MarpPregnancyScreening.discardBranchAnswers(api, HIGH_RISK_STEP, MarpPregnancyScreening.nextStep(form));

        assertFalse(checklist.has("value"));
        assertFalse(checklist.getJSONArray("options").getJSONObject(0).has("value"));
        assertFalse(clientGroup.has("value"));
        verify(api).writeValue(HIGH_RISK_STEP, "checklist", "options", "criterion", "false",
                "", "", "", false);
    }

    @Test
    public void keepsExistingAnswersWhenTheDestinationStillApplies() throws Exception {
        JsonApi api = mock(JsonApi.class);
        MarpPregnancyScreening.discardBranchAnswers(api, HIGH_RISK_STEP, HIGH_RISK_STEP);
        verifyNoInteractions(api);
    }

    @Test
    public void blocksBothYesRoutesUntilDestinationIsAdded() throws Exception {
        JSONObject form = loadForm("kvp_screening_female", Locale.ENGLISH);
        form.remove(HIGH_RISK_STEP);
        JsonApi api = formApi(form);
        AppExecutors executors = mock(AppExecutors.class);
        when(api.getAppExecutors()).thenReturn(executors);
        when(executors.mainThread()).thenReturn(Runnable::run);
        KvpScreeningJsonFormFragmentPresenter presenter = intakePresenter(api);
        JSONObject pregnant = field(form, INTAKE_STEP, PREGNANCY_FIELD);
        JSONObject breastfeeding = field(form, INTAKE_STEP, BREASTFEEDING_FIELD);

        pregnant.put("value", "yes");
        assertTrue(presenter.executeRefreshLogicForNextStep());
        pregnant.put("value", "no");
        breastfeeding.put("value", "yes");
        assertTrue(presenter.executeRefreshLogicForNextStep());
        verify(api, never()).setNextStep("step1");
        verify(api, never()).setNextStep(HIGH_RISK_STEP);
    }

    @Test
    public void presenterPreparesDrugUseForNoNoAndHighRiskWhenAvailable() throws Exception {
        JSONObject form = loadForm("kvp_screening_female", Locale.ENGLISH);
        addHighRiskFixture(form);
        JsonApi api = formApi(form);
        when(api.isNextStepRelevant()).thenReturn(true);
        KvpScreeningJsonFormFragmentPresenter presenter = intakePresenter(api);
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

    private static KvpScreeningJsonFormFragmentPresenter intakePresenter(JsonApi api) {
        JsonFormFragment fragment = mock(JsonFormFragment.class);
        when(fragment.getJsonApi()).thenReturn(api);
        KvpScreeningJsonFormFragmentPresenter presenter = new KvpScreeningJsonFormFragmentPresenter(
                fragment, mock(JsonFormInteractor.class)) {
            {
                mStepName = INTAKE_STEP;
            }
        };
        presenter.attachView(fragment);
        return presenter;
    }

    private static JSONObject addHighRiskFixture(JSONObject form) throws Exception {
        JSONObject checklist = new JSONObject().put("key", "checklist").put("type", "check_box")
                .put("options", new JSONArray().put(new JSONObject().put("key", "criterion")));
        form.put(HIGH_RISK_STEP, new JSONObject().put("next", "step9")
                .put("fields", new JSONArray().put(checklist)));
        return checklist;
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
