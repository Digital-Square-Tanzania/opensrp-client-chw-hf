package org.smartregister.chw.hf.actionhelper.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.BREASTFEEDING;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.FIELD;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.NEITHER;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.PREGNANT;

import android.content.Context;

import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.smartregister.chw.hf.utils.MarpPregnancyStatus;
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
import java.util.List;
import java.util.Map;

public class MarpPregnancyStatusTest {
    private static final String GROUP = MarpPregnancyScreening.GROUP_CODE;

    @Test
    public void fieldIsRequiredOnlyForFemaleMaternalHighRiskDominantGroup() throws Exception {
        JSONObject femaleMaternal = form(open(helper("female", GROUP, null, ""), null));
        assertTrue(field(femaleMaternal, FIELD).getJSONObject("v_required").getBoolean("value"));
        assertNull(field(form(open(helper("male", GROUP, null, ""), null)), FIELD));
        assertNull(field(form(open(helper("female", "fsw", null, ""), null)), FIELD));
        assertNull(field(form(open(helper("female", null, null, ""), null)), FIELD));
    }

    @Test
    public void firstVisitPrefillsPregnantBeforeBreastfeedingWithoutMarkingItSaved() throws Exception {
        KvpClientStatusActionHelper helper = helper("female", GROUP, screening("yes", "yes"), "");
        assertEquals(PREGNANT, field(form(open(helper, null)), FIELD).getString("value"));
        assertEquals("", helper.getPregnancyBreastfeedingStatus());
    }

    @Test
    public void firstVisitPrefillsBreastfeedingAndLeavesMissingScreeningBlank() throws Exception {
        assertEquals(BREASTFEEDING, field(form(open(helper("female", GROUP, screening("no", "yes"), ""), null)),
                FIELD).getString("value"));
        assertFalse(field(form(open(helper("female", GROUP, null, ""), null)), FIELD).has("value"));
    }

    @Test
    public void earlierMarpVisitWithoutBiomedicalStillRequiresAFreshAnswer() throws Exception {
        KvpClientStatusActionHelper helper = helper("female", GROUP, screening("yes", null), "", true);
        assertFalse(field(form(open(helper, null)), FIELD).has("value"));
    }

    @Test
    public void firstVisitOverrideIsAvailableImmediatelyAndReopensFromTheSavedObservation() throws Exception {
        KvpClientStatusActionHelper helper = helper("female", GROUP, screening("yes", null), "");
        BaseKvpVisitAction action = open(helper, null);
        JSONObject form = form(action);
        field(form, MarpEnrollmentDate.CLIENT_STATUS).put("value", "new_client");
        field(form, FIELD).put("value", NEITHER);
        action.setJsonPayload(form.toString());
        assertEquals(BaseKvpVisitAction.Status.COMPLETED, action.getActionStatus());
        assertEquals(NEITHER, helper.getPregnancyBreastfeedingStatus());

        Event saved = new Event();
        JSONObject submitted = form(action);
        JsonFormUtils.addObservation(saved, field(submitted, MarpEnrollmentDate.CLIENT_STATUS));
        JsonFormUtils.addObservation(saved, field(submitted, FIELD));
        assertEquals(NEITHER, MarpEnrollmentDate.observation(saved, FIELD));
        Map<String, List<VisitDetail>> details = NCUtil.eventsObsToDetails(saved.getObs(), "visit", "synthetic-client");
        KvpClientStatusActionHelper reopened = helper("female", GROUP, screening("yes", null), NEITHER);
        assertEquals(NEITHER, field(form(open(reopened, details)), FIELD).getString("value"));
        assertEquals(NEITHER, reopened.getPregnancyBreastfeedingStatus());
    }

    @Test
    public void laterVisitStartsBlankAndUsesLatestOnlyUntilAValidAnswerIsSaved() throws Exception {
        KvpClientStatusActionHelper helper = helper("female", GROUP, screening("yes", null), PREGNANT);
        BaseKvpVisitAction action = open(helper, null);
        JSONObject form = form(action);
        JSONObject status = field(form, FIELD);
        assertFalse(status.has("value"));
        assertEquals(PREGNANT, helper.getPregnancyBreastfeedingStatus());

        field(form, MarpEnrollmentDate.CLIENT_STATUS).put("value", "return");
        action.setJsonPayload(form.toString());
        assertEquals(BaseKvpVisitAction.Status.PENDING, action.getActionStatus());
        assertEquals(PREGNANT, helper.getPregnancyBreastfeedingStatus());

        status.put("value", BREASTFEEDING);
        action.setJsonPayload(form.toString());
        assertEquals(BaseKvpVisitAction.Status.COMPLETED, action.getActionStatus());
        assertEquals(BREASTFEEDING, helper.getPregnancyBreastfeedingStatus());
    }

    private static KvpClientStatusActionHelper helper(String gender, String group, Event screening, String latest) {
        return helper(gender, group, screening, latest, false);
    }

    private static KvpClientStatusActionHelper helper(String gender, String group, Event screening, String latest,
                                                     boolean hasEarlierMarpVisit) {
        return new KvpClientStatusActionHelper(new MemberObject(), MarpPregnancyStatus.appliesTo(gender, group)) {
            @Override
            protected String getScreeningDefaults() {
                return "";
            }

            @Override
            protected boolean hasPreviousClientStatus() {
                return !latest.isEmpty();
            }

            @Override
            protected String getScreeningPregnancyStatus() {
                return MarpPregnancyStatus.fromScreening(screening);
            }

            @Override
            protected boolean hasEarlierMarpVisit() {
                return hasEarlierMarpVisit;
            }

            @Override
            protected String getLatestPregnancyStatus() {
                return latest;
            }
        };
    }

    private static Event screening(String pregnant, String breastfeeding) {
        Event event = new Event();
        addAnswer(event, MarpPregnancyScreening.PREGNANCY_FIELD, pregnant);
        addAnswer(event, MarpPregnancyScreening.BREASTFEEDING_FIELD, breastfeeding);
        return event;
    }

    private static void addAnswer(Event event, String field, String value) {
        if (value != null) {
            Obs obs = new Obs();
            obs.setFormSubmissionField(field);
            obs.setValues(List.of(value));
            event.addObs(obs);
        }
    }

    private static BaseKvpVisitAction open(KvpClientStatusActionHelper helper, Map<String, List<VisitDetail>> details)
            throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get("src/main/assets/json.form/kvp_client_status.json"));
        return new BaseKvpVisitAction.Builder(mock(Context.class), "Client Status")
                .withFormName("kvp_client_status")
                .withJsonPayload(new String(bytes, StandardCharsets.UTF_8))
                .withHelper(helper)
                .withDetails(details)
                .build();
    }

    private static JSONObject form(BaseKvpVisitAction action) throws Exception {
        return new JSONObject(action.getJsonPayload());
    }

    private static JSONObject field(JSONObject form, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(form.getJSONObject("step1").getJSONArray("fields"), key);
    }
}
