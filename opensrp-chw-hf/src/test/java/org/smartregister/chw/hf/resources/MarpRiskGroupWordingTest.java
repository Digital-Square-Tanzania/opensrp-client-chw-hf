package org.smartregister.chw.hf.resources;

import android.content.res.Resources;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.MarpGroupLabels;
import org.smartregister.util.JsonFormUtils;
import org.w3c.dom.Document;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MarpRiskGroupWordingTest {
    private static final Path MAIN = Paths.get("src", "main");

    @Test
    public void englishClientStatusAndProfileUseLegacyCodes() throws Exception {
        assertLabels("values", "A man at higher risk (MHR)", "A woman at high risk (WHR)");
    }

    @Test
    public void kiswahiliClientStatusAndProfileUseLegacyCodes() throws Exception {
        assertLabels("values-sw", "Mwanaume aliye katika hatari zaidi (MHR)",
                "Mwanamke aliye katika hatari zaidi (WHR)");
    }

    private static void assertLabels(String resourceFolder, String mhrLabel, String whrLabel) throws Exception {
        Document strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(MAIN.resolve("res").resolve(resourceFolder).resolve("strings.xml").toFile());
        Resources resources = mock(Resources.class);
        when(resources.getString(R.string.marp_mhr_option)).thenReturn(string(strings, "marp_mhr_option"));
        when(resources.getString(R.string.marp_whr_option)).thenReturn(string(strings, "marp_whr_option"));

        byte[] json = Files.readAllBytes(MAIN.resolve("assets/json.form/kvp_client_status.json"));
        JSONObject form = new JSONObject(new String(json, StandardCharsets.UTF_8));
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        JSONArray options = JsonFormUtils.getFieldJSONObject(fields, "other_kvp_category").getJSONArray("options");
        JSONObject mhr = JsonFormUtils.getFieldJSONObject(options, "msm");
        JSONObject whr = JsonFormUtils.getFieldJSONObject(options, "fsw");
        JSONObject pwud = JsonFormUtils.getFieldJSONObject(options, "pwud");
        mhr.put("value", true);
        whr.put("value", true);

        MarpGroupLabels.applyLongLabels(resources, options);

        assertEquals("KVP Client Status", form.getString("encounter_type"));
        assertOption(mhr, "msm", mhrLabel);
        assertOption(whr, "fsw", whrLabel);
        assertEquals("PWUD", pwud.getString("text"));
        // The profile resolves both dominant and other groups using "kvp_" + the stored key.
        assertEquals("MHR", string(strings, "kvp_" + mhr.getString("key")));
        assertEquals("WHR", string(strings, "kvp_" + whr.getString("key")));
    }

    private static void assertOption(JSONObject option, String key, String label) throws Exception {
        assertEquals(key, option.getString("key"));
        assertEquals(key, option.getString("openmrs_entity_id"));
        assertEquals(label, option.getString("text"));
        assertEquals(true, option.getBoolean("value"));
    }

    private static String string(Document strings, String name) throws Exception {
        return XPathFactory.newInstance().newXPath()
                .evaluate("/resources/string[@name='" + name + "']", strings);
    }
}
