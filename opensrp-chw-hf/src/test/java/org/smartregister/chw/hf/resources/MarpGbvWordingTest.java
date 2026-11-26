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
        assertGbvWording(Locale.ENGLISH, "values", "GBV Screening", "GBV Case Reported",
                List.of("Physical violence", "Sexual violence", "Emotional violence", "None", "Not assessed"),
                "Post GBV Care Provision", List.of("Yes", "No"), "Please select an answer");
    }

    @Test
    public void showsScreeningInKiswahiliWithNewAndRestoredAnswers() throws Exception {
        assertGbvWording(new Locale("sw"), "values-sw", "Uchunguzi wa Ukatili wa kijinsia", "Ukatili aliofanyiwa",
                List.of("Ukatili wa kimwili", "Ukatili wa kingono", "Ukatili wa kihisia", "Hajafanyiwa ukatili", "Hajachunguzwa"),
                "Amepata huduma baada ya kufanyiwa ukatili", List.of("Ndio", "Hapana"), "Tafadhali chagua jibu");
    }

    private void assertGbvWording(Locale locale, String valuesDirectory, String title, String label,
                                  List<String> optionLabels, String careLabel, List<String> careOptionLabels,
                                  String requiredError) throws Exception {
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
        JSONArray fields = step.getJSONArray("fields");
        JSONObject question = fields.getJSONObject(0);
        assertEquals(label, question.getString("label"));
        assertOptions(question, List.of("physical_violence", "sexual_violence", "emotional_violence", "none", "not_assessed"),
                optionLabels);

        JSONObject careQuestion = fields.getJSONObject(1);
        assertEquals(careLabel, careQuestion.getString("label"));
        assertEquals("post_gbv_care_provision", careQuestion.getString("key"));
        assertEquals("post_gbv_care_provision", careQuestion.getString("openmrs_entity_id"));
        assertEquals(requiredError, careQuestion.getJSONObject("v_required").getString("err"));
        assertOptions(careQuestion, List.of("yes", "no"), careOptionLabels);

        VisitDetail screening = new VisitDetail();
        screening.setDetails("physical_violence");
        VisitDetail care = new VisitDetail();
        care.setDetails("yes");
        KvpJsonFormUtils.populateForm(form, Map.of(
                "gbv_screening", List.of(screening),
                "post_gbv_care_provision", List.of(care)));

        assertEquals(title, step.getString("title"));
        assertEquals("[\"physical_violence\"]", fields.getJSONObject(0).getJSONArray("value").toString());
        assertEquals(careLabel, fields.getJSONObject(1).getString("label"));
        assertEquals("yes", fields.getJSONObject(1).getString("value"));
    }

    private static void assertOptions(JSONObject question, List<String> codes, List<String> labels) throws Exception {
        JSONArray options = question.getJSONArray("options");
        assertEquals(codes.size(), options.length());
        for (int i = 0; i < codes.size(); i++) {
            JSONObject option = options.getJSONObject(i);
            assertEquals(codes.get(i), option.getString("key"));
            assertEquals(codes.get(i), option.getString("openmrs_entity_id"));
            assertEquals(labels.get(i), option.getString("text"));
        }
    }
}
