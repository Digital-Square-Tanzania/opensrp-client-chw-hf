package org.smartregister.chw.hf.resources;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.MarpVisitCategories;
import org.smartregister.util.JsonFormUtils;
import org.w3c.dom.Document;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

public class MarpRiskGroupWordingTest {
    private static final Path MAIN = Paths.get("src", "main");

    @Test
    public void englishClientStatusUsesRegisterLabelsAndStableCodes() throws Exception {
        assertLabels("values", "Other MARP categories", "Please pick at least one", List.of(
                "People who use drugs through non-injecting routes", "People who inject drugs",
                "Men at High Risk (MHR)", "Most-at-risk women (WHR)", "Adolescent girls and young women",
                "Discordant partner", "Long-distance truck drivers", "Fisherfolk", "Miners",
                "Other key and vulnerable populations", "None"));
    }

    @Test
    public void kiswahiliClientStatusUsesRegisterLabelsAndStableCodes() throws Exception {
        assertLabels("values-sw", "Makundi mengine yaliyo katika hatari ya kupata maambukizi ya VVU",
                "Tafadhali chagua angalau moja", List.of(
                        "Watumiaji wa dawa za kulevya kwa njia nyingine", "Wajidunga dawa za kulevya",
                        "Mwanaume aliye katika hatari zaidi (MHR)", "Mwanamke aliye katika hatari zaidi (WHR)",
                        "Msichana rika balehe na mwanamke kijana", "Mwenza mwenye majibu kinzani",
                        "Madereva wa masafa marefu", "Wavuvi", "Wachimba madini", "Makundi mengine hatarishi", "Hakuna"));
    }

    private static void assertLabels(String folder, String question, String required, List<String> labels) throws Exception {
        Context context = mock(Context.class);
        Document strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(MAIN.resolve("res").resolve(folder).resolve("strings.xml").toFile());
        for (String name : List.of(
                "marp_other_client_category", "marp_other_category_required", "marp_other_category_none",
                "marp_pwud_enrollment_option", "marp_pwid_enrollment_option", "marp_mhr_enrollment_option",
                "marp_whr_enrollment_option", "marp_agyw_option", "marp_discordant_partner_option",
                "kvp_long_distance_truck_drivers", "kvp_fisherfolk", "kvp_miners",
                "kvp_other_key_and_vulnerable_populations")) {
            when(context.getString(R.string.class.getField(name).getInt(null))).thenReturn(string(strings, name));
        }
        byte[] json = Files.readAllBytes(MAIN.resolve("assets/json.form/kvp_client_status.json"));
        JSONObject form = new JSONObject(new String(json, StandardCharsets.UTF_8));
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        JSONObject category = JsonFormUtils.getFieldJSONObject(fields, "other_kvp_category");
        assertEquals("other_kvp_category", category.getString("openmrs_entity_id"));
        assertEquals("none", category.getJSONArray("exclusive").getString(0));
        assertEquals(1, category.getJSONArray("exclusive").length());
        assertEquals(true, category.getJSONObject("v_required").getBoolean("value"));

        List<String> codes = List.of("pwud", "pwid", "msm", "fsw", "agyw", "serodiscordant_couple",
                "long_distance_truck_drivers", "fisherfolk", "miners", "other_key_and_vulnerable_populations", "none");
        Map<String, String> displayed = new LinkedHashMap<>();
        for (String sex : List.of("male", "female")) {
            JSONObject localized = new JSONObject(category.toString());
            MarpVisitCategories.configure(localized, context, "", sex, 24);
            assertEquals(question, localized.getString("label"));
            assertEquals(required, localized.getJSONObject("v_required").getString("err"));
            JSONArray options = localized.getJSONArray("options");
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);
                String code = option.getString("key");
                assertEquals(code, option.getString("openmrs_entity_id"));
                displayed.put(code, option.getString("text"));
            }
        }
        assertEquals(codes.size(), displayed.size());
        for (int i = 0; i < codes.size(); i++) {
            assertEquals(labels.get(i), displayed.get(codes.get(i)));
        }

        // Existing profile and history readers still resolve the stored codes.
        assertEquals("MHR", string(strings, "kvp_msm"));
        assertEquals("WHR", string(strings, "kvp_fsw"));
        assertEquals("AGYW", string(strings, "kvp_agyw"));
    }

    private static String string(Document strings, String name) throws Exception {
        return XPathFactory.newInstance().newXPath()
                .evaluate("/resources/string[@name='" + name + "']", strings);
    }
}
