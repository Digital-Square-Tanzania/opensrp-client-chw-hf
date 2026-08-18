package org.smartregister.chw.hf.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HivstSourceFormSubmissionIdConfigTest {

    private static final String SOURCE_FORM_SUBMISSION_ID = "source_form_submission_id";

    @Test
    public void resultsFormCapturesSourceFormSubmissionId() throws IOException {
        JsonObject form = readAsset("json.form/hivst_results.json");
        JsonObject field = findByProperty(
                form.getAsJsonObject("step1").getAsJsonArray("fields"),
                "key",
                SOURCE_FORM_SUBMISSION_ID
        );

        assertNotNull(field);
        assertEquals("hidden", field.get("type").getAsString());
        assertEquals(SOURCE_FORM_SUBMISSION_ID, field.get("openmrs_entity_id").getAsString());
    }

    @Test
    public void hivstResultsTableMapsSourceFormSubmissionId() throws IOException {
        JsonObject config = readAsset("ec_client_fields.json");
        JsonObject table = findByProperty(config.getAsJsonArray("bindobjects"), "name", "ec_hivst_results");

        assertNotNull(table);
        JsonObject column = findByProperty(
                table.getAsJsonArray("columns"),
                "column_name",
                SOURCE_FORM_SUBMISSION_ID
        );

        assertNotNull(column);
        assertEquals("Event", column.get("type").getAsString());
        assertEquals(
                SOURCE_FORM_SUBMISSION_ID,
                column.getAsJsonObject("json_mapping").get("concept").getAsString()
        );
    }

    private static JsonObject readAsset(String relativePath) throws IOException {
        Path path = Paths.get("opensrp-chw-hf", "src", "main", "assets", relativePath);
        String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        return new JsonParser().parse(json).getAsJsonObject();
    }

    private static JsonObject findByProperty(JsonArray elements, String property, String expectedValue) {
        for (JsonElement element : elements) {
            JsonObject object = element.getAsJsonObject();
            if (object.has(property) && expectedValue.equals(object.get(property).getAsString())) {
                return object;
            }
        }
        return null;
    }
}
