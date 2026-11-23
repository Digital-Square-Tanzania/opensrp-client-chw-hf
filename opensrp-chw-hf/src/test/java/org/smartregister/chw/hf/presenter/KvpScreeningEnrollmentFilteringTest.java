package org.smartregister.chw.hf.presenter;

import android.content.Context;
import android.content.res.Resources;

import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.AppExecutors;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.mockito.ArgumentCaptor;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KvpScreeningEnrollmentFilteringTest {

    private static final Locale KISWAHILI = new Locale("sw");
    private static final String MHR_EN = "A man at higher risk (MHR)";
    private static final String MHR_SW = "Mwanaume aliye katika hatari zaidi (MHR)";
    private static final String WHR_EN = "A woman at high risk (WHR)";
    private static final String WHR_SW = "Mwanamke aliye katika hatari zaidi (WHR)";

    @Test
    public void malePwudOnlyInKiswahili() throws Exception {
        assertOnlyMalePwud(KISWAHILI, "Usajili wa MARP");
    }

    @Test
    public void malePwudOnlyInEnglish() throws Exception {
        assertOnlyMalePwud(Locale.ENGLISH, "MARP Enrollment");
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
        field(form.getJSONObject("step4"), "is_msm").put("value", "no");
        JSONObject group = field(renderEnrollment(form, "step8", KISWAHILI), "client_group");
        assertEquals(0, group.getJSONArray("options").length());
        assertFalse(group.has("value"));

        field(form.getJSONObject("step4"), "is_msm").put("value", "yes");
        assertEnrollmentOptions(form, "step8", KISWAHILI, "Usajili wa MARP", "msm", MHR_SW, "msm");
    }

    @Test
    public void retainsAnExistingSelectionWhenMultipleGroupsQualify() throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_male", Locale.ENGLISH);
        field(form.getJSONObject("step1"), "is_pwud").put("value", "yes");
        field(form.getJSONObject("step4"), "is_msm").put("value", "yes");
        JSONObject clientGroup = field(form.getJSONObject("step8"), "client_group");

        JSONObject rendered = field(renderEnrollment(form, "step8", Locale.ENGLISH), "client_group");
        assertEquals(2, rendered.getJSONArray("options").length());
        assertFalse(rendered.has("value"));

        clientGroup.put("value", "pwud");
        rendered = field(renderEnrollment(form, "step8", Locale.ENGLISH), "client_group");
        assertEquals(2, rendered.getJSONArray("options").length());
        assertEquals("pwud", rendered.getString("value"));
    }

    private static void assertOnlyMalePwud(Locale locale, String title) throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_male", locale);
        field(form.getJSONObject("step1"), "is_pwud").put("value", "yes");
        assertEnrollmentOptions(form, "step8", locale, title, "pwud", "PWUD", "");
    }

    private static void assertOnlyMaleMhr(Locale locale, String title) throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_male", locale);
        assertEquals("MHR", form.getJSONObject("step4").getString("title"));
        field(form.getJSONObject("step4"), "is_msm").put("value", "yes");
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
        JsonFormFragment fragment = mock(JsonFormFragment.class);
        JsonFormInteractor interactor = mock(JsonFormInteractor.class);
        JsonApi jsonApi = mock(JsonApi.class);
        AppExecutors executors = mock(AppExecutors.class);
        Context context = mock(Context.class);
        Resources resources = mock(Resources.class);
        when(resources.getString(R.string.marp_mhr_option)).thenReturn(KISWAHILI.equals(locale) ? MHR_SW : MHR_EN);
        when(resources.getString(R.string.marp_whr_option)).thenReturn(KISWAHILI.equals(locale) ? WHR_SW : WHR_EN);
        when(context.getResources()).thenReturn(resources);
        when(fragment.getContext()).thenReturn(context);
        when(fragment.getStep(stepName)).thenReturn(form.getJSONObject(stepName));
        when(fragment.getCurrentJsonState()).thenReturn(form.toString());
        when(fragment.getJsonApi()).thenReturn(jsonApi);
        when(jsonApi.getAppExecutors()).thenReturn(executors);
        when(executors.diskIO()).thenReturn(Runnable::run);
        when(executors.mainThread()).thenReturn(Runnable::run);

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
