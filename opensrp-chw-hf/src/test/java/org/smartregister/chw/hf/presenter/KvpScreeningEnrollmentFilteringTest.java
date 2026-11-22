package org.smartregister.chw.hf.presenter;

import android.content.Context;

import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.utils.AppExecutors;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KvpScreeningEnrollmentFilteringTest {

    private static final Locale KISWAHILI = new Locale("sw");

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

    private static void assertOnlyMalePwud(Locale locale, String title) throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_male", locale);
        field(form.getJSONObject("step1"), "is_pwud").put("value", "yes");
        assertEnrollmentOptions(form, "step8", title, "pwud", "PWUD");
    }

    private static void assertOnlyFemaleWhr(Locale locale, String title) throws Exception {
        JSONObject form = loadScreeningForm("kvp_screening_female", locale);
        field(form.getJSONObject("step5"), "is_fsw").put("value", "yes");
        assertEnrollmentOptions(form, "step9", title, "fsw", "WHR");
    }

    private static void assertEnrollmentOptions(JSONObject form, String stepName, String title,
                                                String groupKey, String groupLabel) throws Exception {
        JsonFormFragment fragment = mock(JsonFormFragment.class);
        JsonFormInteractor interactor = mock(JsonFormInteractor.class);
        JsonApi jsonApi = mock(JsonApi.class);
        AppExecutors executors = mock(AppExecutors.class);
        when(fragment.getContext()).thenReturn(mock(Context.class));
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
        assertEquals(title, step.getValue().getString("title"));
        JSONArray options = field(step.getValue(), "client_group").getJSONArray("options");
        assertEquals(options.toString(), 1, options.length());
        JSONObject option = options.getJSONObject(0);
        assertEquals(groupKey, option.getString("key"));
        assertEquals(groupKey, option.getString("openmrs_entity_id"));
        assertEquals(groupLabel, option.getString("text"));
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
