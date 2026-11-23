package org.smartregister.chw.hf.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MarpScreeningRouteTest {

    private static final Path ASSETS = Paths.get("src", "main", "assets");

    @Test
    public void maleScreeningBypassesPrEPAndKeepsEnrollmentStep() throws IOException {
        assertScreeningRoute("male", 6, 7, 8, 8);
    }

    @Test
    public void femaleScreeningBypassesPrEPAndKeepsTheMaternityStepRange() throws IOException {
        assertScreeningRoute("female", 7, 8, 9, 13);
    }

    private static void assertScreeningRoute(
            String sex, int otherGroupsStep, int removedStep, int enrollmentStep, int lastStep
    ) throws IOException {
        String formName = "kvp_screening_" + sex;
        Path formPath = ASSETS.resolve("json.form").resolve(formName + ".json");
        String json = new String(Files.readAllBytes(formPath), StandardCharsets.UTF_8);
        JsonObject screening = new JsonParser().parse(json).getAsJsonObject();

        // The field collectors scan through count, including the maternity outcome steps.
        assertEquals(lastStep, screening.get("count").getAsInt());
        assertEquals("step" + enrollmentStep, screening.getAsJsonObject("step" + otherGroupsStep)
                .get("next").getAsString());
        assertEquals("{{enrollment_title}}", screening.getAsJsonObject("step" + enrollmentStep)
                .get("title").getAsString());
        assertFalse(screening.has("step" + removedStep));
        assertFalse(json.contains("prep_assessment"));
        assertFalse(json.contains("prep_qualified"));

        Path rulesPath = ASSETS.resolve("rule").resolve(formName + "_relevance.yml");
        String rules = new String(Files.readAllBytes(rulesPath), StandardCharsets.UTF_8);
        assertFalse(rules.contains("prep_assessment"));
        assertFalse(rules.contains("prep_qualified"));
    }
}
