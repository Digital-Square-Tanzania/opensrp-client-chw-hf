package org.smartregister.chw.hf.resources;

import android.content.Context;

import com.vijay.jsonwizard.utils.NativeFormLangUtils;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.YamlRuleDefinitionReader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.actionhelper.kvp.KvpFamilyPlanningActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpHepatitisActionHelper;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.util.JsonFormUtils;

import java.io.Reader;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class MarpBiomedicalWordingTest {
    private static final Path ASSETS = Paths.get("src/main/assets");
    private static final Locale SWAHILI = new Locale("sw");
    private static final String FAMILY_PLANNING = "family_planning_service";

    @Test
    public void rendersRegisterWordingInEnglishAndKiswahili() throws Exception {
        for (Locale locale : List.of(Locale.ENGLISH, SWAHILI)) {
            boolean swahili = locale.equals(SWAHILI);
            JSONObject condom = form("kvp_condom_provision", locale);
            assertEquals(swahili ? "Je, mteja amepewa kondomu?" : "Was the client given condoms?",
                    field(condom, "condoms_given").getString("label"));
            assertEquals(swahili ? "Chagua aina ya kondomu zilizotolewa" : "Select the type of condoms issued",
                    field(condom, "type_of_issued_condoms").getString("label"));

            String methodLabel = swahili ? "Huduma ya uzazi wa mpango aliyopewa" : "Family Planning Method Provided";
            assertRadio(form("kvp_female_family_planning_services", locale), FAMILY_PLANNING, methodLabel,
                    List.of("pill", "injectable", "implanon", "jadelle", "iucd", "tube_ligation", "traditional", "refused", "none"),
                    swahili ? List.of("Vidonge", "Sindano", "Kijiti cha miaka 3", "Kijiti cha miaka 5", "Kitanzi",
                            "TL (kufunga kizazi)", "Njia ya asili", "Amekataa", "Hakuna")
                            : List.of("Pills", "Injection", "3-year implant", "5-year implant", "IUCD",
                                    "TL", "Traditional method", "Refused", "None"));
            assertRadio(form("kvp_male_family_planning_services", locale), FAMILY_PLANNING, methodLabel,
                    List.of("vasectomy", "traditional", "refused", "none"),
                    swahili ? List.of("Kufunga uzazi", "Njia ya asili", "Amekataa", "Hakuna")
                            : List.of("Vasectomy", "Traditional method", "Refused", "None"));

            assertRadio(form("kvp_tb_investigation", locale), "tb_screening",
                    swahili ? "Uchunguzi wa kifua kikuu" : "TB Screening",
                    List.of("presumptive", "not_presumptive", "not_screened"),
                    swahili ? List.of("Ana dalili", "Hana dalili", "Hajachunguzwa")
                            : List.of("Presumptive", "Not presumptive", "Not Screened"));
            JSONObject sti = form("kvp_sti_screening", locale);
            assertRadio(sti, "sti_screening", swahili ? "Uchunguzi magonjwa ya ngono" : "STI screening",
                    List.of("positive", "negative", "screening_not_conducted"),
                    swahili ? List.of("Ana dalili", "Hana dalili", "Hajachunguzwa")
                            : List.of("Positive", "Negative", "Not Screened"));
            assertRadio(sti, "treatment_provided", swahili ? "Matibabu ya magonjwa ya ngono" : "STI treatment",
                    List.of("treated", "not_treated"), swahili ? List.of("Ametibiwa", "Hajatibiwa")
                            : List.of("Treated", "Not treated"));

            JSONObject hepatitis = form("kvp_hepatitis", locale);
            List<String> results = swahili ? List.of("Hasi", "Chanya") : List.of("Negative", "Positive");
            assertRadio(hepatitis, "hep_b_result", swahili ? "Majibu ya kipimo cha HBV" : "HBV test result",
                    List.of("negative", "positive"), results);
            assertRadio(hepatitis, "hep_c_result", swahili ? "Majibu ya kipimo cha HCV" : "HCV test result",
                    List.of("negative", "positive"), results);

            assertRadio(form("kvp_vmmc_services", locale), "vmcc_provided", swahili ? "Huduma ya tohara" : "VMMC service",
                    List.of("provided", "not_provided", "circumcised"),
                    swahili ? List.of("Amepewa", "Hajapewa", "Ametahiriwa")
                            : List.of("Received", "Not Received", "Circumcised"));

            assertRadio(form("kvp_cervical_cancer_screening", locale), "cervical_cancer_screening",
                    swahili ? "Uchunguzi wa Saratani ya shingo ya kizazi" : "Cervical cancer screening",
                    List.of("screened", "not_screened"), swahili ? List.of("Amechunguzwa", "Hajachunguzwa")
                            : List.of("Screened", "Not screened"));
        }
    }

    @Test
    public void kiswahiliStiUsesSymptomsAndHtsKeepsChanya() throws Exception {
        JSONObject sti = field(form("kvp_sti_screening", SWAHILI), "sti_screening");
        assertEquals("Ana dalili", sti.getJSONArray("options").getJSONObject(0).getString("text"));

        Path strings = Paths.get("src/main/res/values-sw/strings.xml");
        assertEquals("Chanya", XPathFactory.newInstance().newXPath().evaluate(
                "/resources/string[@name='marp_hts_positive']",
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(strings.toFile())));
    }

    @Test
    public void keepsCondomAndStiVisibilityAndValidation() throws Exception {
        JSONObject condom = form("kvp_condom_provision", SWAHILI);
        assertEquals("equalTo(., \"yes\")", field(condom, "type_of_issued_condoms").getJSONObject("relevance")
                .getJSONObject("step1:condoms_given").getString("ex"));
        for (String type : List.of("male_condoms", "female_condoms")) {
            JSONObject quantity = field(condom, "number_of_" + type + "_issued");
            assertEquals(type, quantity.getJSONObject("relevance").getJSONObject("step1:type_of_issued_condoms")
                    .getJSONArray("ex-checkbox").getJSONObject(0).getJSONArray("or").getString(0));
            assertTrue(quantity.getJSONObject("v_required").getBoolean("value"));
            assertEquals(0, quantity.getJSONObject("v_min").getInt("value"));
            assertEquals(999, quantity.getJSONObject("v_max").getInt("value"));
        }
        JSONObject sti = form("kvp_sti_screening", SWAHILI);
        JSONObject treatment = field(sti, "treatment_provided");
        assertEquals("equalTo(., \"positive\")", treatment.getJSONObject("relevance")
                .getJSONObject("step1:sti_screening").getString("ex"));
        assertTrue(treatment.getJSONObject("v_required").getBoolean("value"));
        assertEquals("equalTo(., \"not_treated\")",
                field(sti, "prompt_for_further_management").getJSONObject("relevance")
                        .getJSONObject("step1:treatment_provided").getString("ex"));
    }

    @Test
    public void keepsFamilyPlanningCondomQuestionRules() throws Exception {
        Rules rules = rules("kvp_family_planning_services_relevance.yml");
        Facts facts = new Facts();
        facts.put("global_wasCondomGiven", true);
        for (String choice : List.of("tube_ligation", "vasectomy", "refused")) {
            facts.put("step1_family_planning_service", choice);
            assertFalse(relevant(rules, "condom_provided", facts));
        }
        facts.put("step1_family_planning_service", "pill");
        assertTrue(relevant(rules, "condom_provided", facts));
        facts.put("global_wasCondomGiven", false);
        assertFalse(relevant(rules, "condom_provided", facts));
    }

    @Test
    public void hepatitisResultsStillRequireTestingAndReturnedResults() throws Exception {
        Rules rules = rules("kvp_hepatitis_relevance.yml");
        for (String test : List.of("hep_b", "hep_c")) {
            Facts facts = new Facts();
            for (String response : List.of("", "no", "yes")) {
                facts.put("step1_" + test + "_testing", response);
                assertEquals("yes".equals(response), relevant(rules, test + "_result_given", facts));
                facts.put("step1_" + test + "_result_given", response);
                assertEquals("yes".equals(response), relevant(rules, test + "_result", facts));
            }
        }
    }

    @Test
    public void reopensSavedImplanonAndHepatitisAnswersByCodeAcrossLanguages() throws Exception {
        assertReopen("kvp_female_family_planning_services", FAMILY_PLANNING, "implanon", "Kijiti cha miaka 3",
                new KvpFamilyPlanningActionHelper("no"));
        assertReopen("kvp_hepatitis", "hep_b_result", "positive", "Chanya", new KvpHepatitisActionHelper());
        assertReopen("kvp_hepatitis", "hep_c_result", "negative", "Hasi", new KvpHepatitisActionHelper());
    }

    private static void assertReopen(String formName, String fieldKey, String storedCode, String swahiliLabel,
                                     BaseKvpVisitAction.KvpVisitActionHelper helper) throws Exception {
        JSONObject english = form(formName, Locale.ENGLISH);
        JSONObject answer = field(english, fieldKey);
        answer.put("value", storedCode);
        Event event = new Event();
        JsonFormUtils.addObservation(event, answer);
        assertEquals(List.of(storedCode), event.getObs().get(0).getValues());
        Map<String, List<VisitDetail>> details = NCUtil.eventsObsToDetails(
                event.getObs(), "synthetic-visit", "synthetic-client");
        BaseKvpVisitAction action = new BaseKvpVisitAction.Builder(mock(Context.class), formName)
                .withFormName(formName)
                .withJsonPayload(form(formName, SWAHILI).toString())
                .withDetails(details)
                .withHelper(helper)
                .build();
        JSONObject reopened = field(new JSONObject(action.getJsonPayload()), fieldKey);
        assertEquals(storedCode, reopened.getString("value"));
        JSONObject selectedOption = find(reopened.getJSONArray("options"), storedCode);
        assertEquals(swahiliLabel, selectedOption.getString("text"));
    }

    private static void assertRadio(JSONObject form, String key, String label, List<String> codes, List<String> texts)
            throws Exception {
        JSONObject question = field(form, key);
        assertEquals(label, question.getString("label"));
        assertEquals("native_radio", question.getString("type"));
        assertTrue(question.getJSONObject("v_required").getBoolean("value"));

        JSONArray options = question.getJSONArray("options");
        assertEquals(codes.size(), options.length());
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            assertEquals(codes.get(i), option.getString("key"));
            String concept = key.equals("cervical_cancer_screening") && codes.get(i).equals("not_screened")
                    ? "not_provided" : codes.get(i);
            assertEquals(concept, option.getString("openmrs_entity_id"));
            assertEquals(texts.get(i), option.getString("text"));
        }
    }

    private static JSONObject form(String name, Locale locale) throws Exception {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(locale);
            byte[] bytes = Files.readAllBytes(ASSETS.resolve("json.form").resolve(name + ".json"));
            String source = new String(bytes, StandardCharsets.UTF_8);
            String translated = NativeFormLangUtils.getTranslatedString(source);
            assertFalse(translated.contains("{{"));
            return new JSONObject(translated);
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    private static JSONObject field(JSONObject form, String key) throws Exception {
        return find(form.getJSONObject("step1").getJSONArray("fields"), key);
    }

    private static JSONObject find(JSONArray items, String key) throws Exception {
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.getJSONObject(i);
            if (key.equals(item.getString("key"))) {
                return item;
            }
        }
        throw new AssertionError("Missing form key " + key);
    }

    private static Rules rules(String filename) throws Exception {
        try (Reader reader = Files.newBufferedReader(ASSETS.resolve("rule").resolve(filename))) {
            return new MVELRuleFactory(new YamlRuleDefinitionReader()).createRules(reader);
        }
    }

    private static boolean relevant(Rules rules, String fieldKey, Facts facts) {
        for (Rule rule : rules) {
            if (rule.getName().equals("step1_" + fieldKey)) {
                return rule.evaluate(facts);
            }
        }
        throw new AssertionError("Missing relevance rule for " + fieldKey);
    }
}
