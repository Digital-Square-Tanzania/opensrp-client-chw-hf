package org.smartregister.chw.hf.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MarpScreeningWordingTest {

    @Test
    public void rendersEveryEnrollmentOutcomeInEnglish() throws IOException {
        assertEnrollmentWording(Locale.ENGLISH, "MARP Enrollment", "The client");
    }

    @Test
    public void rendersEveryEnrollmentOutcomeInKiswahili() throws IOException {
        assertEnrollmentWording(new Locale("sw"), "Usajili wa MARP", "Mteja");
    }

    @Test
    public void rendersDrugUseQuestionsInEnglish() throws IOException {
        assertDrugUseWording(Locale.ENGLISH, "Drug Use",
                "Has the client used drugs in the past 12 months?",
                "Has the client ever injected him or herself with drugs?", "Yes", "No");
    }

    @Test
    public void rendersDrugUseQuestionsInKiswahili() throws IOException {
        assertDrugUseWording(new Locale("sw"), "Matumizi ya dawa za kulevya",
                "Je, mteja ametumia dawa za kulevya ndani ya miezi 12 iliyopita?",
                "Umewahi kujidunga sindano za dawa ya kulevya?", "Ndio", "Hapana");
    }

    private static void assertDrugUseWording(Locale locale, String title, String recentUse,
                                             String injecting, String yes, String no) throws IOException {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            for (String sex : new String[]{"male", "female"}) {
                JsonObject form = loadTranslatedForm("kvp_screening_" + sex);
                assertTrue(form.get("skip_blank_steps").getAsBoolean());
                assertDrugQuestion(form, "step1", "step2", title, "drug_use", recentUse, yes, no);
                JsonObject injection = assertDrugQuestion(form, "step2", "step3", title, "drug_injection",
                        injecting, yes, no);
                String ruleFile = injection.getAsJsonObject("relevance").getAsJsonObject("rules-engine")
                        .getAsJsonObject("ex-rules").get("rules-file").getAsString();
                assertEquals("kvp_screening_" + sex + "_relevance.yml", ruleFile);
            }
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    private static JsonObject assertDrugQuestion(JsonObject form, String step, String next, String title,
                                                 String key, String label, String yes, String no) {
        JsonObject page = form.getAsJsonObject(step);
        assertEquals(title, page.get("title").getAsString());
        assertEquals(next, page.get("next").getAsString());
        JsonObject field = page.getAsJsonArray("fields").get(0).getAsJsonObject();
        assertEquals(key, field.get("key").getAsString());
        assertEquals(key, field.get("openmrs_entity_id").getAsString());
        assertEquals(label, field.get("label").getAsString());
        assertTrue(field.getAsJsonObject("v_required").get("value").getAsBoolean());
        JsonArray options = field.getAsJsonArray("options");
        assertEquals(2, options.size());
        assertAnswer(options.get(0).getAsJsonObject(), "yes", yes);
        assertAnswer(options.get(1).getAsJsonObject(), "no", no);
        return field;
    }

    private static void assertAnswer(JsonObject option, String key, String label) {
        assertEquals(key, option.get("key").getAsString());
        assertEquals(key, option.get("openmrs_entity_id").getAsString());
        assertEquals(label, option.get("text").getAsString());
    }

    private static void assertEnrollmentWording(Locale locale, String title, String outcomePrefix) throws IOException {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            assertEnrollmentForm("kvp_screening_male", "step8", 9, title, outcomePrefix);
            assertEnrollmentForm("kvp_screening_female", "step9", 10, title, outcomePrefix);
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    private static void assertEnrollmentForm(String formName, String step, int expectedOutcomes,
                                             String title, String outcomePrefix) throws IOException {
        JsonObject json = loadTranslatedForm(formName);
        assertEquals("KVP Registration", json.get("encounter_type").getAsString());
        JsonObject enrollment = json.getAsJsonObject(step);
        assertEquals(title, enrollment.get("title").getAsString());

        int outcomes = 0;
        for (JsonElement element : enrollment.getAsJsonArray("fields")) {
            JsonObject field = element.getAsJsonObject();
            if ("toaster_notes".equals(field.get("type").getAsString())) {
                String text = field.get("text").getAsString();
                assertTrue(text, text.startsWith(outcomePrefix));
                assertTrue(text, text.contains("MARP"));
                assertFalse(text, text.contains("KVP"));
                outcomes++;
            }
        }
        assertEquals(expectedOutcomes, outcomes);
    }

    private static JsonObject loadTranslatedForm(String name) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get("src", "main", "assets", "json.form", name + ".json"));
        String translated = NativeFormLangUtils.getTranslatedString(new String(bytes, StandardCharsets.UTF_8));
        assertFalse(translated.contains("{{"));
        return new JsonParser().parse(translated).getAsJsonObject();
    }
}
