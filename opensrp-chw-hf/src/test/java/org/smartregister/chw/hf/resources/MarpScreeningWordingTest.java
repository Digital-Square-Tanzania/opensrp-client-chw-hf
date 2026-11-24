package org.smartregister.chw.hf.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.vijay.jsonwizard.utils.NativeFormLangUtils;

import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rule;
import org.jeasy.rules.mvel.MVELRuleFactory;
import org.jeasy.rules.support.YamlRuleDefinitionReader;
import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MarpScreeningWordingTest {
    private static final String VAGYW_NONE = "not_applicable";
    private static final List<String> VAGYW_CRITERIA = List.of(
            "sex_for_material", "partner_self_injects_drugs", "sexually_abused",
            "infected_with_sexual_disease", "unprotected_sex_last_three_months");

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

    @Test
    public void otherGroupsKeepsItsChecklistAndRoutesToEnrollmentInBothLanguages() throws IOException {
        Locale originalLocale = Locale.getDefault();
        try {
            for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("sw")}) {
                Locale.setDefault(locale);
                boolean swahili = "sw".equals(locale.getLanguage());
                List<String> keys = List.of("long_distance_truck_drivers", "fisherfolk", "miners",
                        "farmers_on_large_farms", "construction_workers", "disabled", "none");
                List<String> labels = swahili
                        ? List.of("Madereva wa masafa marefu", "Wavuvi", "Wachimba madini",
                                "Wakulima wa mashamba makubwa", "Wafanyakazi wa ujenzi", "Walemavu", "Hakuna")
                        : List.of("Long-distance truck drivers", "Fisherfolk", "Miners",
                                "Farmers on large farms", "Construction workers", "Disabled", "None");
                for (String sex : new String[]{"male", "female"}) {
                    boolean male = "male".equals(sex);
                    JsonObject form = loadTranslatedForm("kvp_screening_" + sex);
                    String otherGroupsStep = male ? "step6" : "step7";
                    String enrollmentStep = male ? "step8" : "step9";
                    assertEquals(otherGroupsStep, form.getAsJsonObject(male ? "step5" : "step6")
                            .get("next").getAsString());
                    JsonObject otherGroups = form.getAsJsonObject(otherGroupsStep);
                    assertEquals(swahili ? "Makundi mengine" : "Other Groups", otherGroups.get("title").getAsString());
                    assertEquals(enrollmentStep, otherGroups.get("next").getAsString());
                    assertEquals(swahili ? "Usajili wa MARP" : "MARP Enrollment",
                            form.getAsJsonObject(enrollmentStep).get("title").getAsString());
                    JsonArray fields = otherGroups.getAsJsonArray("fields");
                    assertEquals(2, fields.size());
                    JsonObject checklist = fields.get(0).getAsJsonObject();
                    assertEquals("other_groups_at_risk", checklist.get("key").getAsString());
                    assertEquals("check_box", checklist.get("type").getAsString());
                    assertEquals(swahili ? "Makundi mengine yaliyo katika hatari zaidi ya kupata maambukizi ya VVU"
                            : "Other groups with higher risk of acquiring HIV infection", checklist.get("label").getAsString());
                    JsonObject required = checklist.getAsJsonObject("v_required");
                    assertTrue(required.get("value").getAsBoolean());
                    assertEquals(swahili ? "Chagua angalau chaguo moja" : "Select at least one option",
                            required.get("err").getAsString());
                    assertEquals("[\"none\"]", checklist.getAsJsonArray("exclusive").toString());
                    JsonArray options = checklist.getAsJsonArray("options");
                    assertEquals(keys.size(), options.size());
                    for (int i = 0; i < keys.size(); i++) {
                        assertAnswer(options.get(i).getAsJsonObject(), keys.get(i), labels.get(i));
                    }
                    assertEquals("hidden", fields.get(1).getAsJsonObject().get("type").getAsString());
                    assertFalse(otherGroups.toString().contains("not_applicable"));
                    assertFalse(form.toString().contains("mobile_population"));
                    assertFalse(form.toString().contains("long_distance_drivers"));
                    assertFalse(form.toString().toLowerCase(Locale.ROOT).contains("prisoner"));
                    assertFalse(form.toString().toLowerCase(Locale.ROOT).contains("rumandee"));
                }
            }
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void vagywUsesTheBilingualChecklistAndDistinctObservationCodes() throws IOException {
        Locale originalLocale = Locale.getDefault();
        try {
            for (Locale locale : List.of(Locale.ENGLISH, new Locale("sw"))) {
                Locale.setDefault(locale);
                JsonObject male = loadTranslatedForm("kvp_screening_male");
                JsonObject female = loadTranslatedForm("kvp_screening_female");
                for (JsonObject form : List.of(male, female)) {
                    assertFalse(form.toString().toLowerCase(Locale.ROOT).contains("not applicable"));
                }
                assertFalse(male.toString().contains("harsh_condition_impact"));

                JsonObject page = female.getAsJsonObject("step6");
                assertEquals("step7", page.get("next").getAsString());
                assertEquals("vAGYW", page.get("title").getAsString());
                boolean sw = "sw".equals(locale.getLanguage());
                JsonObject school = page.getAsJsonArray("fields").get(0).getAsJsonObject();
                assertEquals(sw ? "Je, kwa sasa upo shule?" : "Is the client currently in school?",
                        school.get("label").getAsString());
                JsonObject checklist = page.getAsJsonArray("fields").get(1).getAsJsonObject();
                assertEquals(sw ? "Athari na mazingira hatarishi"
                                : "Impact and vulnerability of living in a risky environment",
                        checklist.get("label").getAsString());
                assertTrue(checklist.getAsJsonObject("v_required").get("value").getAsBoolean());
                assertEquals(sw ? "Chagua angalau chaguo moja" : "Select at least one option",
                        checklist.getAsJsonObject("v_required").get("err").getAsString());
                List<String> labels = sw
                        ? List.of("Ndani ya miezi 6 iliyopita, amefanya ngono ili apate vitu au fedha",
                                "Ana mahusiano ya kingono na mtu anayejidunga dawa za kulevya",
                                "Mhanga anayeendelea kufanyiwa ukatili wa kingono",
                                "Amepata magonjwa ya ngono ndani ya miezi 3",
                                "Amefanya ngono bila kinga ndani ya miezi 3")
                        : List.of("In the last 6 months, she has had sex to get things or money",
                                "She has a sexual relationship with a person who self-injects drugs",
                                "A victim who continues to be sexually abused",
                                "Had STI within 3 months", "Has had unprotected sex within 3 months");
                assertEquals("harsh_condition_impact", checklist.get("key").getAsString());
                assertEquals("check_box", checklist.get("type").getAsString());
                assertEquals("[\"not_applicable\"]", checklist.getAsJsonArray("exclusive").toString());
                JsonArray options = checklist.getAsJsonArray("options");
                assertEquals(VAGYW_CRITERIA.size() + 1, options.size());
                for (int i = 0; i < VAGYW_CRITERIA.size(); i++) {
                    JsonObject option = options.get(i).getAsJsonObject();
                    assertEquals(VAGYW_CRITERIA.get(i), option.get("key").getAsString());
                    assertEquals(VAGYW_CRITERIA.get(i), option.get("openmrs_entity_id").getAsString());
                    assertEquals(labels.get(i), option.get("text").getAsString());
                }
                assertAnswer(options.get(VAGYW_CRITERIA.size()).getAsJsonObject(), VAGYW_NONE,
                        "sw".equals(locale.getLanguage()) ? "Hakuna" : "None");
            }
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void vagywChecklistIsRelevantOnlyForTheApplicableOutOfSchoolRoute() throws Exception {
        Rule school = loadVagywRule("relevance", "step6_client_in_school");
        Rule checklist = loadVagywRule("relevance", "step6_harsh_condition_impact");
        Facts client = new Facts();
        client.put("step5_is_fsw", "no");
        client.put("step3_sexual_intercourse", "yes");
        client.put("step6_client_in_school", "");
        client.put("global_age", 19);
        assertTrue(school.evaluate(client));
        assertFalse(checklist.evaluate(client));
        client.put("step6_client_in_school", "yes");
        assertFalse(checklist.evaluate(client));
        client.put("step6_client_in_school", "no");
        for (int age : List.of(15, 19, 24)) {
            client.put("global_age", age);
            assertTrue(school.evaluate(client));
            assertTrue(checklist.evaluate(client));
        }
        for (int age : List.of(14, 25, 36)) {
            client.put("global_age", age);
            assertFalse(school.evaluate(client));
            assertFalse(checklist.evaluate(client));
        }
        client.put("global_age", 19);
        client.put("step5_is_fsw", "yes");
        assertFalse(checklist.evaluate(client));
        client.put("step5_is_fsw", "no");
        client.put("step3_sexual_intercourse", "no");
        assertFalse(checklist.evaluate(client));
    }

    @Test
    public void enrollmentFieldsFollowEligibilityInBothForms() throws Exception {
        for (String sex : List.of("male", "female")) {
            String step = "male".equals(sex) ? "step8" : "step9";
            Facts client = new Facts();
            try (Reader reader = Files.newBufferedReader(Paths.get("src", "main", "assets", "rule",
                    "kvp_screening_" + sex + "_relevance.yml"))) {
                int checked = 0;
                for (Rule rule : new MVELRuleFactory(new YamlRuleDefinitionReader()).createRules(reader)) {
                    if (List.of(step + "_birth_region", step + "_client_group").contains(rule.getName())) {
                        client.put(step + "_should_enroll", "no");
                        assertFalse(rule.evaluate(client));
                        client.put(step + "_should_enroll", "yes");
                        assertTrue(rule.evaluate(client));
                        checked++;
                    }
                }
                assertEquals(2, checked);
            }
        }
    }

    @Test
    public void enrollmentShowsOnlyTheMatchingDrugOutcomeInBothForms() throws Exception {
        for (String sex : List.of("male", "female")) {
            String step = "male".equals(sex) ? "step8" : "step9";
            try (Reader reader = Files.newBufferedReader(Paths.get("src", "main", "assets", "rule",
                    "kvp_screening_" + sex + "_relevance.yml"))) {
                int checked = 0;
                for (Rule rule : new MVELRuleFactory(new YamlRuleDefinitionReader()).createRules(reader)) {
                    boolean pwid = (step + "_prompt_for_pwid_kvp").equals(rule.getName());
                    if (pwid || (step + "_prompt_for_pwud_kvp").equals(rule.getName())) {
                        Facts client = new Facts();
                        client.put("step1_drug_use", "yes");
                        client.put("step2_drug_injection", "yes");
                        assertEquals(pwid, rule.evaluate(client));
                        client.put("step2_drug_injection", "no");
                        assertEquals(!pwid, rule.evaluate(client));
                        client.put("step2_drug_injection", "");
                        assertFalse(rule.evaluate(client));
                        client.put("step1_drug_use", "no");
                        client.put("step2_drug_injection", "yes");
                        assertFalse(rule.evaluate(client));
                        checked++;
                    }
                }
                assertEquals(2, checked);
            }
        }
    }

    private static Rule loadVagywRule(String type, String name) throws Exception {
        try (Reader reader = Files.newBufferedReader(Paths.get("src", "main", "assets", "rule",
                "kvp_screening_female_" + type + ".yml"))) {
            for (Rule rule : new MVELRuleFactory(new YamlRuleDefinitionReader()).createRules(reader)) {
                if (name.equals(rule.getName())) {
                    return rule;
                }
            }
        }
        throw new AssertionError("Missing rule " + name);
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
            assertEnrollmentForm("kvp_screening_male", "step8", 6, title, outcomePrefix);
            assertEnrollmentForm("kvp_screening_female", "step9", 7, title, outcomePrefix);
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
