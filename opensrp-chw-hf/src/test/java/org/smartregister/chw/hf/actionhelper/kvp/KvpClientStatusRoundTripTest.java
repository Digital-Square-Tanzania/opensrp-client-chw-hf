package org.smartregister.chw.hf.actionhelper.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.hf.actionhelper.kvp.KvpClientStatusActionHelper.OTHER_CATEGORY;
import static org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status.COMPLETED;
import static org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status.PENDING;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.utils.MarpVisitCategories;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KvpClientStatusRoundTripTest {
    @Test
    public void maleMhrExcludesHisGroupAndFemaleGroupsAndPrefillsPwid() throws Exception {
        JSONObject category = category(payload(open(configured("msm", "Male", 38), "[pwid,fsw,agyw,msm]", null)));
        assertNull(option(category, "msm"));
        assertNull(option(category, "fsw"));
        assertNull(option(category, "agyw"));
        assertEquals(Set.of("pwid"), selections(category));
        assertTrue(option(category, "pwid").getBoolean("value"));
    }

    @Test
    public void agywEligibilityIncludes15And24WithDominantGroupExclusion() throws Exception {
        for (int age : List.of(14, 15, 24, 25, 34)) {
            JSONObject category = category(configured("fsw", "Female", age));
            assertEquals(age >= 15 && age <= 24, option(category, "agyw") != null);
            assertNull(option(category, "fsw"));
            assertNull(option(category, "msm"));
        }
        JSONObject agywDominant = category(configured("agyw", "Female", 24));
        assertNull(option(agywDominant, "agyw"));
        assertNotNull(option(agywDominant, "fsw"));
        assertNull(option(agywDominant, "msm"));
    }

    @Test
    public void retiredAndMaternalGroupsAreAbsentAndEachRegisterGroupCanBeExcluded() throws Exception {
        for (String code : List.of("prisoner", "rumandee", "mobile_population", "other_vulnerable_population",
                "pregnant_breastfeeding_high_risk")) {
            assertNull(option(category(configured("pwud", "Female", 24)), code));
        }
        for (String code : List.of("pwud", "pwid", "msm", "fsw", "agyw", "serodiscordant_couple",
                "long_distance_truck_drivers", "fisherfolk", "miners", "other_key_and_vulnerable_populations")) {
            assertNull(option(category(configured(code, "msm".equals(code) ? "Male" : "Female", 24)), code));
        }
        assertNotNull(option(category(configured("none", "Female", 24)), "none"));
        assertNull(option(category(configured("pwud", "", 24)), "agyw"));
    }

    @Test
    public void singleScreenedGroupDefaultsToNoneAndIneligibleOnlyLeavesAnExplicitChoice() throws Exception {
        JSONObject form = configured("msm", "Male", 38);
        for (String screened : List.of("", "[]", "[none]")) {
            assertEquals(Set.of("none"), selections(category(payload(open(form, screened, null)))));
        }
        for (String screened : List.of("[prisoner]", "[prefix_pwid]", "[none,fsw]", "[agyw,msm]")) {
            assertTrue(selections(category(payload(open(form, screened, null)))).isEmpty());
        }
        assertEquals(Set.of("pwid", "pwud"), selections(category(payload(
                open(form, "[\"PWID\", \"pwud\", none, prisoner]", null)))));
    }

    @Test
    public void completingClientStatusRequiresEligibleGroupsOrExclusiveNone() throws Exception {
        BaseKvpVisitAction action = open(configured("msm", "Male", 38), "[pwid]", null);
        JSONObject form = payload(action);
        field(form, "client_status").put("value", "new_client");
        for (List<String> invalid : List.of(List.<String>of(), List.of("none", "pwid"), List.of("prisoner"),
                List.of("pwid", "fsw"), List.of("msm"))) {
            category(form).put("value", new JSONArray(invalid));
            action.setJsonPayload(form.toString());
            assertEquals(PENDING, action.getActionStatus());
        }
        for (List<String> valid : List.of(List.of("none"), List.of("pwud", "pwid"))) {
            category(form).put("value", new JSONArray(valid));
            action.setJsonPayload(form.toString());
            assertEquals(COMPLETED, action.getActionStatus());
        }
    }

    @Test
    public void saveAndReopenUseStableCodesAndIgnoreScreeningDefaultsWhenEditing() throws Exception {
        JSONObject config = configured("fsw", "Female", 24);
        BaseKvpVisitAction action = open(config, "[pwud]", null);
        JSONObject form = payload(action);
        field(form, "client_status").put("value", "new_client");
        Set<String> selected = Set.of("agyw", "fisherfolk");
        MarpVisitCategories.setSelection(category(form), selected);
        action.setJsonPayload(form.toString());
        assertEquals(COMPLETED, action.getActionStatus());

        Event event = new Event();
        JsonFormUtils.addObservation(event, field(payload(action), "client_status"));
        JsonFormUtils.addObservation(event, category(payload(action)));
        Obs obs = event.getObs().stream().filter(o -> OTHER_CATEGORY.equals(o.getFormSubmissionField()))
                .findFirst().orElseThrow();
        assertEquals(List.of("agyw", "fisherfolk"), obs.getValues());
        assertEquals(obs.getValues(), obs.getHumanReadableValues());

        BaseKvpVisitAction reopened = open(config, "[pwud]", details(event));
        JSONObject category = category(payload(reopened));
        assertEquals(COMPLETED, reopened.getActionStatus());
        assertEquals(selected, selections(category));
        assertTrue(option(category, "agyw").getBoolean("value"));
        assertFalse(option(category, "pwud").optBoolean("value"));
        assertNull(option(category, "fsw"));
    }

    @Test
    public void legacyAndIneligibleHistoryRemainsReadableAndRequiresReviewBeforeResaving() throws Exception {
        Event old = eventWithSavedCodes(List.of("pwid", "prisoner", "agyw"));
        BaseKvpVisitAction action = open(configured("msm", "Male", 38), "[pwud]", details(old));
        assertEquals(PENDING, action.getActionStatus());
        assertEquals(Set.of("pwid"), selections(category(payload(action))));
        assertEquals(List.of("pwid", "prisoner", "agyw"), old.getObs().get(1).getValues());

        action.setJsonPayload(action.getJsonPayload());
        assertEquals(COMPLETED, action.getActionStatus());
        assertEquals(List.of("pwid", "prisoner", "agyw"), old.getObs().get(1).getValues());

        BaseKvpVisitAction allRetired = open(configured("msm", "Male", 38), "[pwud]",
                details(eventWithSavedCodes(List.of("rumandee", "mobile_population"))));
        assertTrue(selections(category(payload(allRetired))).isEmpty());
        assertEquals(PENDING, allRetired.getActionStatus());
    }

    @Test
    public void editingMixedHistoricalNoneShowsEligibleGroupsForExplicitReview() throws Exception {
        Event old = eventWithSavedCodes(List.of("none", "pwid"));
        BaseKvpVisitAction action = open(configured("msm", "Male", 38), "[]", details(old));
        assertEquals(PENDING, action.getActionStatus());
        JSONObject category = category(payload(action));
        assertEquals(Set.of("pwid"), selections(category));
        assertFalse(option(category, "none").getBoolean("value"));
        assertEquals(List.of("none", "pwid"), old.getObs().get(1).getValues());
    }

    @Test
    public void editingAnEmptyCategoryDoesNotApplyScreeningDefaults() throws Exception {
        JSONObject category = category(payload(open(configured("msm", "Male", 38), "[pwud]", Map.of())));
        assertTrue(selections(category).isEmpty());
        assertFalse(option(category, "pwud").optBoolean("value"));
    }

    private static BaseKvpVisitAction open(JSONObject config, String screened, Map<String, List<VisitDetail>> details)
            throws Exception {
        KvpClientStatusActionHelper helper = new KvpClientStatusActionHelper(new MemberObject()) {
            @Override
            protected String getScreeningDefaults() {
                return screened;
            }

            @Override
            protected boolean hasPreviousClientStatus() {
                return false;
            }
        };
        return new BaseKvpVisitAction.Builder(mock(Context.class), "Client Status")
                .withFormName("kvp_client_status").withJsonPayload(config.toString())
                .withHelper(helper).withDetails(details).build();
    }

    private static Event eventWithSavedCodes(List<String> codes) throws Exception {
        JSONObject form = form();
        Event event = new Event();
        JsonFormUtils.addObservation(event, field(form, "client_status").put("value", "new_client"));
        Obs category = new Obs();
        category.setFormSubmissionField(OTHER_CATEGORY);
        category.setFieldCode(OTHER_CATEGORY);
        category.setValues(new ArrayList<>(codes));
        category.setHumanReadableValues(new ArrayList<>(codes));
        event.addObs(category);
        return event;
    }

    private static Map<String, List<VisitDetail>> details(Event event) throws JSONException {
        return NCUtil.eventsObsToDetails(event.getObs(), "synthetic-visit", "synthetic-client");
    }

    private static JSONObject configured(String dominant, String gender, int age) throws Exception {
        JSONObject form = form();
        Context context = mock(Context.class);
        when(context.getString(anyInt())).thenReturn("Localized label");
        MarpVisitCategories.configure(category(form), context, dominant, gender, age);
        return form;
    }

    private static Set<String> selections(JSONObject category) {
        return MarpVisitCategories.codes(category.optString("value"));
    }

    private static JSONObject payload(BaseKvpVisitAction action) throws Exception {
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
