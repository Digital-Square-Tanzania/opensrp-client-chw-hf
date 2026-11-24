package org.smartregister.chw.hf.actionhelper.kvp;

import static org.smartregister.chw.hf.actionhelper.kvp.KvpClientStatusActionHelper.OTHER_CATEGORY;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class KvpClientStatusRoundTripTest {
    @Test
    public void savesAndReopensOnlyMhr() throws Exception {
        assertRoundTrip("msm", "fsw");
    }

    @Test
    public void savesAndReopensOnlyWhr() throws Exception {
        assertRoundTrip("fsw", "msm");
    }

    @Test
    public void editingAnEmptyCategoryDoesNotRestoreScreeningDefaults() throws Exception {
        JSONObject reopened = open(form(), Map.of());
        assertFalse(option(category(reopened), "pwud").optBoolean("value"));
    }

    @Test
    public void reopensSavedLegacyGroupsWithTheirExistingLabels() throws Exception {
        for (String code : List.of("prisoner", "rumandee")) {
            JSONObject original = form();
            JSONObject savedCategory = category(original);
            option(savedCategory, code).put("value", true);
            savedCategory.put("value", new JSONArray(List.of(code)));
            Event saved = new Event();
            JsonFormUtils.addObservation(saved, savedCategory);
            Map<String, List<VisitDetail>> details = NCUtil.eventsObsToDetails(
                    saved.getObs(), "synthetic-visit", "synthetic-client");
            JSONObject reopened = category(open(form(), details));
            JSONObject group = option(reopened, code);
            assertTrue(group.getBoolean("value"));
            assertEquals("prisoner".equals(code) ? "Prisoners" : "Rumandee", group.getString("text"));
            assertEquals(code, reopened.getJSONArray("value").getString(0));
        }
    }

    private static void assertRoundTrip(String chosen, String excluded) throws Exception {
        JSONObject form = form();
        JSONObject category = category(form);
        KvpJsonFormUtils.removeOptionFromCheckboxListWithKey(category, "pwid");
        KvpJsonFormUtils.removeOptionFromCheckboxListWithKey(category, excluded);

        JSONObject filledForm = open(form, null);
        JSONObject filledCategory = category(filledForm);
        assertTrue(option(filledCategory, "pwud").optBoolean("value"));
        option(filledCategory, "pwud").put("value", false);
        option(filledCategory, chosen).put("value", true);
        filledCategory.put("value", new JSONArray(List.of(chosen)));
        field(filledForm, "client_status").put("value", "return");

        Event saved = new Event();
        JsonFormUtils.addObservation(saved, field(filledForm, "client_status"));
        JsonFormUtils.addObservation(saved, filledCategory);
        Obs categoryObs = saved.getObs().stream()
                .filter(obs -> OTHER_CATEGORY.equals(obs.getFormSubmissionField())).findFirst().orElse(null);
        assertNotNull(categoryObs);
        assertEquals(List.of(chosen), categoryObs.getValues());
        assertEquals(List.of(chosen), categoryObs.getHumanReadableValues());

        Map<String, List<VisitDetail>> details = NCUtil.eventsObsToDetails(
                saved.getObs(), "synthetic-visit", "synthetic-client");
        assertEquals(chosen, details.get(OTHER_CATEGORY).get(0).getDetails());
        JSONObject reopenedForm = open(form, details);
        assertEquals("return", field(reopenedForm, "client_status").getString("value"));
        JSONObject reopened = category(reopenedForm);
        assertTrue(option(reopened, chosen).getBoolean("value"));
        assertFalse(option(reopened, "pwud").optBoolean("value"));
        assertNull(option(reopened, "pwid"));
        assertNull(option(reopened, excluded));
        assertEquals(new JSONArray(List.of(chosen)).toString(), reopened.getJSONArray("value").toString());
    }

    private static JSONObject open(JSONObject form, Map<String, List<VisitDetail>> details) throws Exception {
        KvpClientStatusActionHelper helper = new KvpClientStatusActionHelper(new MemberObject()) {
            @Override
            protected String getScreeningDefaults() {
                return "[pwud]";
            }

            @Override
            protected boolean hasPreviousClientStatus() {
                return false;
            }
        };
        BaseKvpVisitAction action = new BaseKvpVisitAction.Builder(mock(Context.class), "Client Status")
                .withFormName("kvp_client_status")
                .withJsonPayload(form.toString())
                .withHelper(helper)
                .withDetails(details)
                .build();
        return new JSONObject(action.getJsonPayload());
    }

    private static JSONObject form() throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("src/main/assets/json.form/kvp_client_status.json"));
        return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
    }

    private static JSONObject category(JSONObject form) throws Exception {
        return field(form, OTHER_CATEGORY);
    }

    private static JSONObject field(JSONObject form, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(form.getJSONObject("step1").getJSONArray("fields"), key);
    }

    private static JSONObject option(JSONObject category, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(category.getJSONArray("options"), key);
    }
}
