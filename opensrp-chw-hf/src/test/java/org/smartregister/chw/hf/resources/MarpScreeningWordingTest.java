package org.smartregister.chw.hf.resources;

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
        byte[] bytes = Files.readAllBytes(Paths.get("src", "main", "assets",
                "json.form", formName + ".json"));
        String form = new String(bytes, StandardCharsets.UTF_8);
        String translatedForm = NativeFormLangUtils.getTranslatedString(form);
        JsonObject json = new JsonParser().parse(translatedForm).getAsJsonObject();

        assertFalse(translatedForm.contains("{{"));
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
}
