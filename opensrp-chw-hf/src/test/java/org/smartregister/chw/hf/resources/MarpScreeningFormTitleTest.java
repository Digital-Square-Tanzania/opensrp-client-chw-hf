package org.smartregister.chw.hf.resources;

import com.vijay.jsonwizard.utils.NativeFormLangUtils;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MarpScreeningFormTitleTest {
    private static final Path FORMS = Paths.get("src/main/assets/json.form");

    @Test
    public void resolvesScreeningTitlesInEnglish() throws Exception {
        assertTitles(Locale.ENGLISH, "TB Screening", "STI Screening");
    }

    @Test
    public void resolvesScreeningTitlesInKiswahili() throws Exception {
        assertTitles(Locale.forLanguageTag("sw"), "Uchunguzi wa TB", "Uchunguzi wa magonjwa ya ngono");
    }

    private static void assertTitles(Locale locale, String tbTitle, String stiTitle) throws Exception {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            assertEquals(tbTitle, resolveTitle("kvp_tb_investigation.json"));
            assertEquals(stiTitle, resolveTitle("kvp_sti_screening.json"));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    private static String resolveTitle(String formName) throws Exception {
        String source = new String(Files.readAllBytes(FORMS.resolve(formName)), StandardCharsets.UTF_8);
        String resolved = NativeFormLangUtils.getTranslatedString(source);
        assertFalse(resolved.contains("{{"));
        return new JSONObject(resolved).getJSONObject("step1").getString("title");
    }
}
