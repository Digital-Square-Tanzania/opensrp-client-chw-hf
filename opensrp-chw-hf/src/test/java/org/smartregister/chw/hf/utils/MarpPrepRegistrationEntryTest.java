package org.smartregister.chw.hf.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.CONSENT_DECLINED;
import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.HIDDEN;
import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.PENDING;
import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.REGISTERED;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilderFactory;

@RunWith(Parameterized.class)
public class MarpPrepRegistrationEntryTest {
    private static final Path MAIN = Path.of("src/main");
    private static final int ELIGIBLE_AGE = 28;

    private final String language;
    private JSONArray htsOptions;
    private NodeList strings;

    @Parameterized.Parameters(name = "{0}")
    public static String[] languages() {
        return new String[]{"en", "sw"};
    }

    public MarpPrepRegistrationEntryTest(String language) {
        this.language = language;
    }

    @Before
    public void loadLocalizedHtsFixture() throws Exception {
        byte[] fixture = Files.readAllBytes(MAIN.resolve("assets/json.form/kvp_hts.json"));
        JSONObject form = new JSONObject(new String(fixture, StandardCharsets.UTF_8));
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        htsOptions = findByKey(fields, "hiv_status").getJSONArray("options");

        String directory = "sw".equals(language) ? "values-sw" : "values";
        Path resources = MAIN.resolve("res").resolve(directory).resolve("strings.xml");
        strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(resources.toFile()).getElementsByTagName("string");
    }

    @Test
    public void inconclusiveHidesPendingAndDeclinedRetry() throws Exception {
        String result = recordedCode("inconclusive", "marp_hts_inconclusive", "Inconclusive", "Majibu tata");
        assertHiddenAfterHts(result);
    }

    @Test
    public void refusedResultsHidePendingAndDeclinedRetry() throws Exception {
        String result = recordedCode("refused_results", "refused_results",
                "Refuse to receive the results", "Amekataa kupokea majibu");
        assertHiddenAfterHts(result);
    }

    @Test
    public void positiveAndKnownPositiveHideBothRegistrationEntries() throws Exception {
        assertHiddenAfterHts(recordedCode("positive", "marp_hts_positive", "Positive", "Chanya"));
        assertHiddenAfterHts(recordedCode("known_positive", "marp_hts_known_positive",
                "Known positive", "Tayari ana maambukizi"));
    }

    @Test
    public void positiveScreeningHidesRegistrationEvenWithNegativeHts() throws Exception {
        assertHidden(MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, negativeResult(), "positive", null));
        assertHidden(MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, null, "positive", false));
    }

    @Test
    public void noProcessedClosureHidesRegistrationEvenWithNegativeHts() throws Exception {
        assertHidden(MarpPrepRegistrationEntry.evaluate(false, ELIGIBLE_AGE, negativeResult(), null, null));
        assertHidden(MarpPrepRegistrationEntry.evaluate(false, ELIGIBLE_AGE, null, null, false));
    }

    @Test
    public void missingHtsAfterProcessedClosureAllowsRegistration() {
        MarpPrepRegistrationEntry pending = MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, null, null, null);
        MarpPrepRegistrationEntry retry = MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, "", null, false);
        assertEquals(PENDING, pending);
        assertTrue(pending.canRegister());
        assertEquals(CONSENT_DECLINED, retry);
        assertTrue(retry.canRegister());
    }

    @Test
    public void negativeAllowsRegistrationAndRetryUntilConsentYesIsRecorded() throws Exception {
        String result = negativeResult();
        MarpPrepRegistrationEntry pending = MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, result, null, null);
        MarpPrepRegistrationEntry retry = MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, result, null, false);
        assertEquals(PENDING, pending);
        assertTrue(pending.canRegister());
        assertEquals(CONSENT_DECLINED, retry);
        assertTrue(retry.canRegister());
        assertRegistered(MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, result, null, true));
    }

    @Test
    public void existingRegistrationStatusSurvivesEntryExclusions() {
        assertRegistered(MarpPrepRegistrationEntry.evaluate(false, ELIGIBLE_AGE, null, "positive", true));
        assertRegistered(MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, "inconclusive", null, true));
        assertRegistered(MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, "refused_results", null, true));
    }

    @Test
    public void preservesExistingMinimumAge() {
        assertHidden(MarpPrepRegistrationEntry.evaluate(true, 14, null, null, null));
        assertTrue(MarpPrepRegistrationEntry.evaluate(true, 15, null, null, null).canRegister());
    }

    private static void assertHiddenAfterHts(String result) {
        assertHidden(MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, result, null, null));
        assertHidden(MarpPrepRegistrationEntry.evaluate(true, ELIGIBLE_AGE, result, null, false));
    }

    private static void assertHidden(MarpPrepRegistrationEntry entry) {
        assertEquals(HIDDEN, entry);
        assertFalse(entry.canRegister());
    }

    private static void assertRegistered(MarpPrepRegistrationEntry entry) {
        assertEquals(REGISTERED, entry);
        assertFalse(entry.canRegister());
    }

    private String negativeResult() throws Exception {
        return recordedCode("negative", "marp_hts_negative", "Negative", "Hasi");
    }

    private String recordedCode(String key, String resource, String english, String swahili) throws Exception {
        assertEquals("sw".equals(language) ? swahili : english, resourceText(resource));
        String code = findByKey(htsOptions, key).getString("openmrs_entity_id");
        assertEquals(key, code);
        return code;
    }

    private String resourceText(String name) {
        for (int i = 0; i < strings.getLength(); i++) {
            Element string = (Element) strings.item(i);
            if (name.equals(string.getAttribute("name"))) {
                return string.getTextContent();
            }
        }
        throw new AssertionError("Missing resource " + name);
    }

    private static JSONObject findByKey(JSONArray fields, String key) throws JSONException {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (key.equals(field.optString("key"))) {
                return field;
            }
        }
        throw new AssertionError("Missing form field or option " + key);
    }
}
