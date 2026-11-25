package org.smartregister.chw.hf.resources;

import com.vijay.jsonwizard.utils.NativeFormLangUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import static org.junit.Assert.assertEquals;

public class MarpGbvWordingTest {

    @Test
    public void showsScreeningInEnglishWithNewAndRestoredAnswers() throws Exception {
        assertGbvWording(Locale.ENGLISH, "values", "GBV Screening");
    }

    @Test
    public void showsScreeningInKiswahiliWithNewAndRestoredAnswers() throws Exception {
        assertGbvWording(new Locale("sw"), "values-sw", "Uchunguzi wa Ukatili wa kijinsia");
    }

    private void assertGbvWording(Locale locale, String valuesDirectory, String title) throws Exception {
        Path strings = Paths.get("src/main/res", valuesDirectory, "strings.xml");
        assertEquals(title, XPathFactory.newInstance().newXPath().evaluate(
                "/resources/string[@name='kvp_gbv_analysis']",
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(strings.toFile())));

        String asset = new String(Files.readAllBytes(Paths.get("src/main/assets/json.form/kvp_gbv_analysis.json")),
                StandardCharsets.UTF_8);
        Locale originalLocale = Locale.getDefault();
        JSONObject form;
        try {
            Locale.setDefault(locale);
            form = new JSONObject(NativeFormLangUtils.getTranslatedString(asset));
        } finally {
            Locale.setDefault(originalLocale);
        }

        JSONObject step = form.getJSONObject("step1");
        assertEquals(title, step.getString("title"));
        assertEquals("KVP GBV Analysis", form.getString("encounter_type"));

        VisitDetail screening = new VisitDetail();
        screening.setDetails("physical_violence");
        VisitDetail care = new VisitDetail();
        care.setDetails("yes");
        KvpJsonFormUtils.populateForm(form, Map.of(
                "gbv_screening", List.of(screening),
                "post_gbv_care_provision", List.of(care)));

        assertEquals(title, step.getString("title"));
        JSONArray fields = step.getJSONArray("fields");
        assertEquals("physical_violence", fields.getJSONObject(0).getString("value"));
        assertEquals("yes", fields.getJSONObject(1).getString("value"));
    }
}
