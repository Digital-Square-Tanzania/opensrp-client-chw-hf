package org.smartregister.chw.hf.interactor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.hf.actionhelper.kvp.KvpClientStatusActionHelper.OTHER_CATEGORY;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.CLIENT_STATUS;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.ENROLLMENT_DATE;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.EVENT_DETAIL;
import static org.smartregister.chw.hf.utils.MarpHts.CTC_NUMBER;
import static org.smartregister.chw.hf.utils.MarpHts.CTC_SERVICES;
import static org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status.COMPLETED;
import static org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status.PENDING;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.actionhelper.kvp.KvpClientStatusActionHelper;
import org.smartregister.chw.hf.actionhelper.kvp.KvpHtsActionHelper;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.hf.utils.MarpPregnancyStatus;
import org.smartregister.chw.hf.utils.MarpVisitCategories;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MarpVisitTypeHistoryTest {
    @Test
    public void firstNewTypeSurvivesKnownPositiveAndRefusalHistorySaves() throws Exception {
        BaseKvpVisitAction clientStatus = startVisit("new_client", "");
        Event initial = toEvent(clientStatus);
        assertEquals("new_visit", MarpEnrollmentDate.observation(initial, CLIENT_STATUS));

        clientStatus = openClientStatus("new_visit", details(initial));
        assertEquals(COMPLETED, clientStatus.getActionStatus());
        BaseKvpVisitAction hts = openHts(details(initial));
        JSONObject htsForm = payload(hts);
        field(htsForm, "tested_hiv").put(VALUE, "yes");
        field(htsForm, "previous_hiv_testing_method").put(VALUE, "hiv_cv_test");
        field(htsForm, "testing_site").put(VALUE, "facility");
        field(htsForm, "hiv_status").put(VALUE, "known_positive");
        field(htsForm, CTC_SERVICES).put(VALUE, "enrolled_and_on_arv");
        field(htsForm, CTC_NUMBER).put(VALUE, "02-03-0100-000123");
        hts.setJsonPayload(htsForm.toString());

        Event knownPositive = toEvent(clientStatus, hts);
        assertEquals("new_visit", MarpEnrollmentDate.observation(knownPositive, CLIENT_STATUS));
        assertEquals("known_positive", MarpEnrollmentDate.observation(knownPositive, "hiv_status"));
        assertEquals("02-03-0100-000123", MarpEnrollmentDate.observation(knownPositive, CTC_NUMBER));

        clientStatus = openClientStatus("new_visit", details(knownPositive));
        assertEquals(COMPLETED, clientStatus.getActionStatus());
        hts = openHts(details(knownPositive));
        htsForm = payload(hts);
        field(htsForm, "hiv_status").put(VALUE, "refused_results");
        hts.setJsonPayload(htsForm.toString());
        assertEquals(COMPLETED, hts.getActionStatus());

        Event refusal = toEvent(clientStatus, hts);
        assertEquals("new_visit", MarpEnrollmentDate.observation(refusal, CLIENT_STATUS));
        clientStatus = openClientStatus("new_visit", details(refusal));
        JSONObject statusForm = payload(clientStatus);
        assertEquals(COMPLETED, clientStatus.getActionStatus());
        assertEquals("new_client", field(statusForm, CLIENT_STATUS).getString(VALUE));
        assertNotNull(visitTypeOption(statusForm, "new_client"));
        assertEquals(MarpPregnancyStatus.NEITHER, field(statusForm, MarpPregnancyStatus.FIELD).getString(VALUE));
        assertEquals(Set.of("none"), MarpVisitCategories.codes(field(statusForm, OTHER_CATEGORY).optString(VALUE)));

        JSONObject reopenedHts = payload(openHts(details(refusal)));
        assertEquals("refused_results", field(reopenedHts, "hiv_status").getString(VALUE));
        assertFalse(field(reopenedHts, CTC_SERVICES).has(VALUE));
        assertFalse(field(reopenedHts, CTC_NUMBER).has(VALUE));
        assertEquals("", MarpEnrollmentDate.observation(refusal, CTC_SERVICES));
        assertEquals("", MarpEnrollmentDate.observation(refusal, CTC_NUMBER));
    }

    @Test
    public void historyResavesRetainFirstReturnDateAndKeepNewHidden() throws Exception {
        String date = "03-04-2020";
        Event saved = toEvent(startVisit("return", date));
        MarpEnrollmentDate.recordFirstVisitDate(saved);
        for (int edit = 0; edit < 2; edit++) {
            // The programme date is retained on the event even if absent from the editable payload.
            Event savedAnswers = new Event();
            saved.getObs().stream().filter(obs -> !ENROLLMENT_DATE.equals(obs.getFormSubmissionField()))
                    .forEach(savedAnswers::addObs);
            BaseKvpVisitAction reopened = openClientStatus("return", details(savedAnswers));
            assertEquals(COMPLETED, reopened.getActionStatus());
            assertEquals("return", field(payload(reopened), CLIENT_STATUS).getString(VALUE));
            assertNull(visitTypeOption(payload(reopened), "new_client"));

            Event replacement = toEvent(reopened);
            MarpEnrollmentDate.retainOnResave(saved, replacement);
            assertEquals(date, replacement.getDetails().get(EVENT_DETAIL));
            assertEquals(date, MarpEnrollmentDate.observation(replacement, ENROLLMENT_DATE));
            saved = replacement;
        }
    }

    @Test
    public void firstEncounterOffersNewAndFollowUpStillRequiresAnAllowedSelection() throws Exception {
        BaseKvpVisitAction first = openClientStatus("", null);
        JSONObject firstForm = payload(first);
        for (String type : List.of("new_client", "transfer_in", "return")) {
            assertNotNull(visitTypeOption(firstForm, type));
        }
        field(firstForm, MarpPregnancyStatus.FIELD).put(VALUE, MarpPregnancyStatus.NEITHER);
        field(firstForm, CLIENT_STATUS).put(VALUE, "return");
        first.setJsonPayload(firstForm.toString());
        assertEquals(PENDING, first.getActionStatus());

        BaseKvpVisitAction followUp = openClientStatus("new_visit", null);
        JSONObject form = payload(followUp);
        assertNull(visitTypeOption(form, "new_client"));
        assertNotNull(visitTypeOption(form, "transfer_in"));
        assertNotNull(visitTypeOption(form, "return"));
        field(form, MarpPregnancyStatus.FIELD).put(VALUE, MarpPregnancyStatus.NEITHER);
        followUp.setJsonPayload(form.toString());
        assertEquals(PENDING, followUp.getActionStatus());
        field(form, CLIENT_STATUS).put(VALUE, "return");
        followUp.setJsonPayload(form.toString());
        assertEquals(COMPLETED, followUp.getActionStatus());
    }

    private static BaseKvpVisitAction startVisit(String type, String date) throws Exception {
        BaseKvpVisitAction action = openClientStatus("", null);
        JSONObject form = payload(action);
        field(form, CLIENT_STATUS).put(VALUE, type);
        field(form, MarpPregnancyStatus.FIELD).put(VALUE, MarpPregnancyStatus.NEITHER);
        field(form, ENROLLMENT_DATE).put(VALUE, date);
        action.setJsonPayload(form.toString());
        assertEquals(COMPLETED, action.getActionStatus());
        return action;
    }

    private static BaseKvpVisitAction openClientStatus(String previousStatus, Map<String, List<VisitDetail>> details)
            throws Exception {
        Context context = context();
        JSONObject form = form("kvp_client_status");
        MarpVisitCategories.configure(field(form, OTHER_CATEGORY), context,
                "pregnant_breastfeeding_high_risk", "Female", 30);
        KvpBioMedicalServiceInteractor.filterVisitTypeOptions(field(form, CLIENT_STATUS), previousStatus, details);

        KvpClientStatusActionHelper helper = new KvpClientStatusActionHelper(new MemberObject(), true) {
            @Override
            protected boolean hasPreviousClientStatus() {
                return StringUtils.isNotBlank(previousStatus);
            }

            @Override
            protected boolean hasEarlierMarpVisit() {
                return false;
            }

            @Override
            protected String getScreeningPregnancyStatus() {
                return MarpPregnancyStatus.PREGNANT;
            }

            @Override
            protected String getScreeningDefaults() {
                return "[]";
            }
        };
        return new BaseKvpVisitAction.Builder(context, "Client Status").withOptional(false)
                .withDetails(details).withFormName("kvp_client_status").withJsonPayload(form.toString())
                .withHelper(helper).build();
    }

    private static BaseKvpVisitAction openHts(Map<String, List<VisitDetail>> details) throws Exception {
        return new BaseKvpVisitAction.Builder(context(), "HTS").withDetails(details)
                .withFormName("kvp_hts").withJsonPayload(form("kvp_hts").toString())
                .withHelper(new KvpHtsActionHelper("")).build();
    }

    private static Context context() {
        Context context = mock(Context.class);
        when(context.getString(anyInt())).thenReturn("Localized label");
        return context;
    }

    private static Event toEvent(BaseKvpVisitAction... actions) throws Exception {
        Event event = new Event();
        for (BaseKvpVisitAction action : actions) {
            JSONArray fields = fields(payload(action));
            for (int i = 0; i < fields.length(); i++) {
                JsonFormUtils.addObservation(event, fields.getJSONObject(i));
            }
        }
        return event;
    }

    private static Map<String, List<VisitDetail>> details(Event event) throws Exception {
        return NCUtil.eventsObsToDetails(event.getObs(), "synthetic-visit", "synthetic-client");
    }

    private static JSONObject form(String name) throws Exception {
        byte[] data = Files.readAllBytes(Paths.get("src/main/assets/json.form", name + ".json"));
        return new JSONObject(new String(data, StandardCharsets.UTF_8));
    }

    private static JSONObject payload(BaseKvpVisitAction action) throws Exception {
        return new JSONObject(action.getJsonPayload());
    }

    private static JSONArray fields(JSONObject form) throws Exception {
        return form.getJSONObject("step1").getJSONArray("fields");
    }

    private static JSONObject field(JSONObject form, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(fields(form), key);
    }

    private static JSONObject visitTypeOption(JSONObject form, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(field(form, CLIENT_STATUS).getJSONArray("options"), key);
    }
}
