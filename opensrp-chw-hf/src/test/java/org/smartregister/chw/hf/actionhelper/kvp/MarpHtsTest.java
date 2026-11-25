package org.smartregister.chw.hf.actionhelper.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.hf.utils.MarpHts.CTC_NUMBER;
import static org.smartregister.chw.hf.utils.MarpHts.CTC_SERVICES;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.content.Context;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.YamlRuleDefinitionReader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.MarpHts;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.util.JsonFormUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

public class MarpHtsTest {
    private static final Path ASSETS = Paths.get("src/main/assets");
    private static final String RAW_ID = "02030100000123";
    private static final String CTC_ID = "02-03-0100-000123";
    private static final String REFUSED_RESULTS = "refused_results";
    private static final List<String> CTC_ANSWERS = List.of("enrolled_but_not_on_arv", "enrolled_and_on_arv");
    private static final List<String> RESULTS = List.of("negative", "positive", "inconclusive", REFUSED_RESULTS, "known_positive");
    private static final List<String> METHODS = List.of("hivst", "hiv_cv_test", "hivst_and_hiv_cv_test", "other_method");

    @Test
    public void usesRegisterLabelsAndResultOrderInBothLanguages() throws Exception {
        JSONObject english = payload(open("", Map.of(), localizedContext("values")));
        JSONObject swahili = payload(open("", Map.of(), localizedContext("values-sw")));

        assertEquals("Has the client tested for HIV", field(english, "tested_hiv").getString("label"));
        assertEquals("Amepima maambukizi ya VVU", field(swahili, "tested_hiv").getString("label"));
        assertEquals("Received HIV Self-Testing (HIVST) Kit", field(english, "kits_distributed").getString("label"));
        assertEquals("Amepewa kitepe cha JIPIME", field(swahili, "kits_distributed").getString("label"));

        assertRadio(english, "previous_hiv_testing_method", "HIV testing method", METHODS,
                List.of("HIVST", "HIV conventional test", "HIVST and HIV conventional test", "Other method"));
        assertRadio(swahili, "previous_hiv_testing_method", "Njia ya kupima VVU", METHODS,
                List.of("HIVST", "Kipimo cha kawaida cha VVU", "HIVST na kipimo cha kawaida cha VVU", "Njia nyingine"));
        assertRadio(english, "testing_site", "HIV Testing Site", List.of("community", "facility"),
                List.of("Community", "Facility"));
        assertRadio(swahili, "testing_site", "Mahali huduma ya upimaji ilipofanyika", List.of("community", "facility"),
                List.of("Jamii", "Kituoni"));
        assertRadio(english, "hiv_status", "HIV testing results", RESULTS,
                List.of("Negative", "Positive", "Inconclusive", "Refuse to receive the results", "Known positive"));
        assertRadio(swahili, "hiv_status", "Majibu baada ya kupima VVU", RESULTS,
                List.of("Hasi", "Chanya", "Majibu tata", "Amekataa kupokea majibu", "Tayari ana maambukizi"));
        assertRadio(english, CTC_SERVICES, "CTC services", CTC_ANSWERS,
                List.of("Enrolled but not initiated on ART", "Enrolled and initiated on ART"));
        assertRadio(swahili, CTC_SERVICES, "Huduma za CTC", CTC_ANSWERS,
                List.of("Ameandikishwa CTC hajaanza ART", "Ameandikishwa na ameanzishiwa ART"));
    }

    @Test
    public void positiveAndKnownPositiveRequireAnIdForBothExistingChoices() throws Exception {
        JSONObject form = form();
        assertNull(field(form, "client_type"));
        assertNull(field(form, "received_test_results"));
        assertEquals("CTC services", field(form, CTC_SERVICES).getString("label"));
        assertEquals("CTC ID #", field(form, CTC_NUMBER).getString("hint"));
        assertTrue(field(form, CTC_NUMBER).getJSONObject("v_required").getBoolean(VALUE));
        assertEquals(2, field(form, CTC_SERVICES).getJSONArray("options").length());

        Rules rules = relevanceRules();
        Facts facts = new Facts();
        for (String result : List.of("positive", "known_positive")) {
            facts.put("step1_hiv_status", result);
            facts.put("step1_ctc_services", "");
            assertTrue(relevant(rules, CTC_SERVICES, facts));
            assertFalse(relevant(rules, CTC_NUMBER, facts));
            for (String answer : CTC_ANSWERS) {
                facts.put("step1_ctc_services", answer);
                assertTrue(relevant(rules, CTC_NUMBER, facts));
            }
        }
        for (String result : List.of("negative", "inconclusive", REFUSED_RESULTS, "")) {
            facts.put("step1_hiv_status", result);
            assertFalse(relevant(rules, CTC_SERVICES, facts));
            assertFalse(relevant(rules, CTC_NUMBER, facts));
        }
    }

    @Test
    public void hivstOnlyAndNotTestedSkipSiteAndResult() throws Exception {
        Rules rules = relevanceRules();
        Facts facts = new Facts();
        for (String testedHiv : List.of("", "no", "yes")) {
            facts.put("step1_tested_hiv", testedHiv);
            assertEquals("yes".equals(testedHiv), relevant(rules, "previous_hiv_testing_method", facts));
            for (String method : List.of("", "not_tested", "hivst", "hiv_cv_test", "hivst_and_hiv_cv_test", "other_method")) {
                facts.put("step1_previous_hiv_testing_method", method);
                boolean expected = "yes".equals(testedHiv)
                        && List.of("hiv_cv_test", "hivst_and_hiv_cv_test", "other_method").contains(method);
                assertEquals(expected, relevant(rules, "testing_site", facts));
                assertEquals(expected, relevant(rules, "hiv_status", facts));
            }
        }
        assertFalse(field(form(), "kits_distributed").has("relevance"));
    }

    @Test
    public void validatesOnlyTheTwoApprovedLayoutsAndFormatsWithoutParsingAsANumber() throws Exception {
        JSONObject id = field(form(), CTC_NUMBER);
        Pattern regex = Pattern.compile(id.getJSONObject("v_regex").getString(VALUE));
        assertFalse(id.has("edit_type"));
        assertTrue(regex.matcher(RAW_ID).matches());
        assertTrue(regex.matcher(CTC_ID).matches());
        assertEquals(CTC_ID, MarpHts.formatCtcId(RAW_ID));
        assertEquals(CTC_ID, MarpHts.formatCtcId(CTC_ID));
        for (String invalid : List.of("", "0203010000012", "020301000001234", "0-203-0100-000123",
                "02-03-0100-00012a", "02-03-0100-000123 ", "02-03-0100-000123\n", " O2030100000123")) {
            assertFalse(invalid, regex.matcher(invalid).matches());
            assertEquals(invalid, MarpHts.formatCtcId(invalid));
        }
    }

    @Test
    public void savedIdAndEnrollmentRoundTripAsExistingObservations() throws Exception {
        for (String result : List.of("positive", "known_positive")) {
            for (String answer : CTC_ANSWERS) {
                BaseKvpVisitAction action = open("", Map.of());
                action.setJsonPayload(answered(result, answer, RAW_ID).toString());
                Event event = eventFrom(action);
                Obs id = observation(event, CTC_NUMBER);
                assertEquals(CTC_NUMBER, id.getFieldCode());
                assertEquals(List.of(CTC_ID), id.getValues());
                assertEquals(List.of(answer), observation(event, CTC_SERVICES).getValues());

                JSONObject reopened = payload(open(result, details(event)));
                assertEquals(CTC_ID, field(reopened, CTC_NUMBER).getString(VALUE));
                assertEquals(answer, field(reopened, CTC_SERVICES).getString(VALUE));
                assertEquals(result, field(reopened, "hiv_status").getString(VALUE));
                assertEquals("yes", reopened.getJSONObject("global").getString("editing_saved_hts"));
            }
        }
    }

    @Test
    public void resultsWithoutConfirmedPositiveDropStaleCtcValuesBeforeObservationMapping() throws Exception {
        BaseKvpVisitAction action = open("", Map.of());
        JSONObject form = answered("positive", CTC_ANSWERS.get(0), CTC_ID);
        action.setJsonPayload(form.toString());
        assertEquals(List.of(CTC_ID), observation(eventFrom(action), CTC_NUMBER).getValues());
        for (String result : List.of("negative", "inconclusive", REFUSED_RESULTS, "")) {
            field(form, "hiv_status").put(VALUE, result);
            action.setJsonPayload(form.toString());
            assertFalse(field(payload(action), CTC_SERVICES).has(VALUE));
            assertFalse(field(payload(action), CTC_NUMBER).has(VALUE));
            assertNull(observation(eventFrom(action), CTC_SERVICES));
            assertNull(observation(eventFrom(action), CTC_NUMBER));
        }
    }

    @Test
    public void refusalRoundTripsAndKeepsHtsAvailableWithoutKnownStatusRouting() throws Exception {
        BaseKvpVisitAction action = open("", Map.of());
        action.setJsonPayload(answered(REFUSED_RESULTS, CTC_ANSWERS.get(0), CTC_ID).toString());
        Event event = eventFrom(action);
        Obs result = observation(event, "hiv_status");
        assertEquals("hiv_status", result.getFieldCode());
        assertEquals(List.of(REFUSED_RESULTS), result.getValues());
        assertEquals(BaseKvpVisitAction.Status.COMPLETED, action.getActionStatus());

        JSONObject reopened = payload(open(REFUSED_RESULTS, details(event)));
        assertEquals(REFUSED_RESULTS, field(reopened, "hiv_status").getString(VALUE));
        assertFalse(field(reopened, CTC_SERVICES).has(VALUE));
        assertFalse(field(reopened, CTC_NUMBER).has(VALUE));

        Rules rules = relevanceRules();
        Facts facts = new Facts();
        facts.put("global_hiv_status", REFUSED_RESULTS);
        facts.put("global_editing_saved_hts", "no");
        facts.put("step1_hiv_status", REFUSED_RESULTS);
        assertTrue(relevant(rules, "tested_hiv", facts));
        assertFalse(relevant(rules, "enroll_prep_services", facts));
        JSONObject prepPrompt = field(reopened, "prompt_for_hiv_negative")
                .getJSONObject("relevance").getJSONObject("step1:hiv_status");
        assertEquals("equalTo(., \"negative\")", prepPrompt.getString("ex"));
        assertFalse(MarpHts.isPositiveResult(REFUSED_RESULTS));
    }

    @Test
    public void reopensNegativeAndInconclusiveRecordsUsingTheOriginalCodes() throws Exception {
        for (String result : List.of("negative", "inconclusive")) {
            BaseKvpVisitAction action = open("", Map.of());
            action.setJsonPayload(answered(result, "", "").toString());
            Event event = eventFrom(action);
            assertEquals(List.of(result), observation(event, "hiv_status").getValues());
            assertEquals(result, field(payload(open(result, details(event))), "hiv_status").getString(VALUE));
        }
    }

    @Test
    public void legacyVisitOpensWithItsRecordedResultAndPreservesRetiredObservationsOnResave() throws Exception {
        BaseKvpVisitAction action = open("", Map.of());
        action.setJsonPayload(answered("positive", CTC_ANSWERS.get(0), "").toString());
        Event legacy = eventFrom(action);
        for (String key : List.of("client_type", "received_test_results")) {
            Obs obs = new Obs();
            obs.setFormSubmissionField(key);
            obs.setFieldCode(key);
            obs.setValues(List.of("legacy-answer"));
            obs.setHumanReadableValues(List.of("legacy-answer"));
            legacy.addObs(obs);
        }

        JSONObject reopened = payload(open("positive", details(legacy)));
        assertEquals("positive", field(reopened, "hiv_status").getString(VALUE));
        assertFalse(field(reopened, CTC_NUMBER).has(VALUE));
        assertNull(field(reopened, "client_type"));
        assertNull(field(reopened, "received_test_results"));
        Facts facts = new Facts();
        facts.put("global_hiv_status", "positive");
        facts.put("global_editing_saved_hts", reopened.getJSONObject("global").getString("editing_saved_hts"));
        Rules rules = relevanceRules();
        assertTrue(relevant(rules, "tested_hiv", facts));
        facts.put("global_editing_saved_hts", "no");
        assertFalse(relevant(rules, "tested_hiv", facts));

        Event replacement = new Event();
        MarpHts.retainRemovedAnswers(legacy, replacement);
        assertEquals(2, replacement.getObs().size());
        assertEquals(List.of("legacy-answer"), observation(replacement, "client_type").getValues());
        assertEquals(List.of("legacy-answer"), observation(replacement, "received_test_results").getValues());
    }

    private static JSONObject form() throws Exception {
        return new JSONObject(new String(Files.readAllBytes(ASSETS.resolve("json.form/kvp_hts.json")),
                StandardCharsets.UTF_8));
    }

    private static JSONObject answered(String result, String ctcServices, String id) throws Exception {
        JSONObject form = form();
        field(form, "tested_hiv").put(VALUE, "yes");
        field(form, "previous_hiv_testing_method").put(VALUE, "hiv_cv_test");
        field(form, "hiv_status").put(VALUE, result);
        field(form, CTC_SERVICES).put(VALUE, ctcServices);
        field(form, CTC_NUMBER).put(VALUE, id);
        return form;
    }

    private static JSONObject field(JSONObject form, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(form.getJSONObject("step1").getJSONArray("fields"), key);
    }

    private static JSONObject payload(BaseKvpVisitAction action) throws Exception {
        return new JSONObject(action.getJsonPayload());
    }

    private static void assertRadio(JSONObject form, String key, String label, List<String> codes,
                                    List<String> texts) throws Exception {
        JSONObject field = field(form, key);
        assertEquals(label, field.getString("label"));
        JSONArray options = field.getJSONArray("options");
        assertEquals(codes.size(), options.length());
        for (int i = 0; i < codes.size(); i++) {
            JSONObject option = options.getJSONObject(i);
            assertEquals(codes.get(i), option.getString("key"));
            assertEquals(codes.get(i), option.getString("openmrs_entity_id"));
            assertEquals(texts.get(i), option.getString("text"));
        }
    }

    private static Context localizedContext(String directory) throws Exception {
        Path stringsFile = ASSETS.resolveSibling("res").resolve(directory).resolve("strings.xml");
        NodeList strings = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(stringsFile.toFile()).getElementsByTagName("string");
        Map<Integer, String> texts = new HashMap<>();
        for (int i = 0; i < strings.getLength(); i++) {
            Element element = (Element) strings.item(i);
            String name = element.getAttribute("name");
            if (name.startsWith("marp_hts_") || REFUSED_RESULTS.equals(name)) {
                texts.put(R.string.class.getField(name).getInt(null), element.getTextContent());
            }
        }
        Context context = mock(Context.class);
        when(context.getString(anyInt())).thenAnswer(invocation -> texts.get(invocation.getArgument(0)));
        return context;
    }

    private static BaseKvpVisitAction open(String lastHivStatus, Map<String, List<VisitDetail>> details) throws Exception {
        Context context = mock(Context.class);
        when(context.getString(anyInt())).thenReturn("Localized label");
        return open(lastHivStatus, details, context);
    }

    private static BaseKvpVisitAction open(String lastHivStatus, Map<String, List<VisitDetail>> details,
                                           Context context) throws Exception {
        return new BaseKvpVisitAction.Builder(context, "HTS")
                .withFormName("kvp_hts")
                .withJsonPayload(form().toString())
                .withDetails(details)
                .withHelper(new KvpHtsActionHelper(lastHivStatus))
                .build();
    }

    private static Event eventFrom(BaseKvpVisitAction action) throws Exception {
        Event event = new Event();
        for (String key : List.of("tested_hiv", "previous_hiv_testing_method", "hiv_status", CTC_SERVICES, CTC_NUMBER)) {
            JsonFormUtils.addObservation(event, field(payload(action), key));
        }
        return event;
    }

    private static Map<String, List<VisitDetail>> details(Event event) throws Exception {
        return NCUtil.eventsObsToDetails(event.getObs(), "synthetic-visit", "synthetic-client");
    }

    private static Obs observation(Event event, String key) {
        return event.getObs().stream().filter(obs -> key.equals(obs.getFormSubmissionField())).findFirst().orElse(null);
    }

    private static Rules relevanceRules() throws Exception {
        try (Reader reader = Files.newBufferedReader(ASSETS.resolve("rule/kvp_hts_relevance.yml"))) {
            return new MVELRuleFactory(new YamlRuleDefinitionReader()).createRules(reader);
        }
    }

    private static boolean relevant(Rules rules, String key, Facts facts) {
        for (Rule rule : rules) {
            if (rule.getName().equals("step1_" + key)) {
                return rule.evaluate(facts);
            }
        }
        throw new AssertionError("No relevance rule for " + key);
    }
}
