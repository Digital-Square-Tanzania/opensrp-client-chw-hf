package org.smartregister.chw.hf.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.smartregister.chw.hf.actionhelper.kvp.KvpClientStatusActionHelper.OTHER_CATEGORY;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.CLIENT_STATUS;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.ENROLLMENT_DATE;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.EVENT_DETAIL;

import android.content.ContentValues;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.smartregister.chw.hf.actionhelper.kvp.KvpClientStatusActionHelper;
import org.smartregister.chw.hf.sync.HfClientProcessor;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.domain.jsonmapping.Column;
import org.smartregister.util.JsonFormUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MarpClientStatusEnrollmentTest {
    private static final String ENTERED_DATE = "29-02-2016";
    private static final String ASSETS = "src/main/assets/";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT);

    @Test
    public void firstTransferInAndReturnRequireARealNonFutureDate() throws Exception {
        for (String type : List.of("transfer_in", "return")) {
            KvpClientStatusActionHelper action = action(false);
            JSONObject form = new JSONObject(action.getPreProcessed());
            assertEquals("no", form.getJSONObject("global").getString("first_visit_done"));
            field(form, CLIENT_STATUS).put("value", type);
            assertStatus(action, form, Status.PENDING);

            for (String invalid : List.of("29-02-2023", "01-01-2020 xyz", LocalDate.now().plusDays(1).format(FORMAT))) {
                field(form, ENROLLMENT_DATE).put("value", invalid);
                assertStatus(action, form, Status.PENDING);
            }
            for (String valid : List.of(ENTERED_DATE, LocalDate.now().format(FORMAT))) {
                field(form, ENROLLMENT_DATE).put("value", valid);
                assertStatus(action, form, Status.COMPLETED);
                assertNull(action.postProcess(form.toString()));
            }
        }
    }

    @Test
    public void firstNewAndSubsequentReturnDiscardHiddenDates() throws Exception {
        KvpClientStatusActionHelper first = action(false);
        JSONObject form = filledForm("new_client", ENTERED_DATE);
        assertStatus(first, form, Status.COMPLETED);
        assertFalse(field(new JSONObject(first.postProcess(form.toString())), ENROLLMENT_DATE).has("value"));

        KvpClientStatusActionHelper later = action(true);
        assertEquals("yes", new JSONObject(later.getPreProcessed()).getJSONObject("global").getString("first_visit_done"));
        form = filledForm("return", ENTERED_DATE);
        assertStatus(later, form, Status.COMPLETED);
        assertFalse(field(new JSONObject(later.postProcess(form.toString())), ENROLLMENT_DATE).has("value"));
    }

    @Test
    public void preprocessedEventSurvivesOfflineReopenAndFollowUp() throws Exception {
        JSONObject first = filledForm("transfer_in", ENTERED_DATE);
        Event firstEvent = event(first);
        MarpEnrollmentDate.recordFirstVisitDate(firstEvent);
        Visit draft = savedVisit(firstEvent, null);
        assertNull(HfKvpDao.findLocalClientStatus(List.of(draft)));
        Visit submitted = savedVisit(firstEvent, "first-submission");
        Visit later = savedVisit(event(filledForm("return", "")), "follow-up-submission");

        Event reopened = HfKvpDao.findLocalClientStatus(List.of(draft, submitted, later));
        assertEquals("transfer_in", MarpEnrollmentDate.observation(reopened, CLIENT_STATUS));
        assertEquals(ENTERED_DATE, reopened.getDetails().get(EVENT_DETAIL));
    }

    @Test
    public void sameTypeHistoryResaveRetainsDateAcrossOfflineReplacementAndProjection() throws Exception {
        for (String type : List.of("return", "transfer_in")) {
            Event original = event(filledForm(type, ENTERED_DATE));
            MarpEnrollmentDate.recordFirstVisitDate(original);
            Visit saved = savedVisit(original, "original-visit");

            JSONObject edit = filledForm(type, ENTERED_DATE);
            KvpClientStatusActionHelper action = action(true);
            Event replacement = event(new JSONObject(action.postProcess(edit.toString())));
            assertEquals("", MarpEnrollmentDate.observation(replacement, ENROLLMENT_DATE));

            MarpEnrollmentDate.retainOnResave(HfKvpDao.readSubmittedBiomedicalEvent(saved), replacement);
            Event reopened = HfKvpDao.findLocalClientStatus(List.of(savedVisit(replacement, "replacement-visit")));
            assertEquals(ENTERED_DATE, reopened.getDetails().get(EVENT_DETAIL));
            assertEquals(ENTERED_DATE, MarpEnrollmentDate.observation(reopened, ENROLLMENT_DATE));
            ContentValues values = mock(ContentValues.class);
            projectRegisterDate(reopened, values);
            verify(values).put(ENROLLMENT_DATE, ENTERED_DATE);
        }
    }

    @Test
    public void processedHistoryEventWithSavedDetailRetainsItsCanonicalDate() throws Exception {
        Event original = event(filledForm("return", ENTERED_DATE));
        MarpEnrollmentDate.recordFirstVisitDate(original);
        Visit processed = new Visit();
        processed.setProcessed(true);
        processed.setJson(new Gson().toJson(original));
        Event replacement = event(filledForm("return", ""));

        MarpEnrollmentDate.retainOnResave(HfKvpDao.readSubmittedBiomedicalEvent(processed), replacement);
        assertEquals(ENTERED_DATE, replacement.getDetails().get(EVENT_DETAIL));
    }

    @Test
    public void legacyHistoryObservationIsRetainedWithoutOverridingRegister() throws Exception {
        Event original = event(filledForm("transfer_in", ENTERED_DATE));
        Event replacement = event(filledForm("transfer_in", ""));

        MarpEnrollmentDate.retainOnResave(original, replacement);
        assertEquals(ENTERED_DATE, MarpEnrollmentDate.observation(replacement, ENROLLMENT_DATE));
        ContentValues values = mock(ContentValues.class);
        projectRegisterDate(replacement, values);
        verifyNoInteractions(values);
    }

    @Test
    public void repeatEditOfLaterReturnDoesNotAssignAnyEnrollmentDate() throws Exception {
        Event original = event(filledForm("return", ""));
        Event replacement = event(filledForm("return", ""));

        MarpEnrollmentDate.retainOnResave(original, replacement);
        assertEquals("", MarpEnrollmentDate.observation(replacement, ENROLLMENT_DATE));
        ContentValues values = mock(ContentValues.class);
        projectRegisterDate(replacement, values);
        verifyNoInteractions(values);
    }

    @Test
    public void registerMappingUsesVisitDateDetailAndIgnoresLegacyVisitAnswers() throws Exception {
        Event legacy = event(filledForm("return", ENTERED_DATE));
        ContentValues values = mock(ContentValues.class);
        projectRegisterDate(legacy, values);
        verifyNoInteractions(values);

        legacy.addDetails(EVENT_DETAIL, ENTERED_DATE);
        projectRegisterDate(legacy, values);
        verify(values).put(ENROLLMENT_DATE, ENTERED_DATE);
    }

    @Test
    public void translatedLabelsKeepExistingRadioAndConceptIdentifiers() throws Exception {
        JSONObject visit = field(form(), CLIENT_STATUS);
        assertEquals("Visit Type", visit.getString("label"));
        for (String[] option : new String[][]{
                {"new_client", "new_visit", "New"}, {"transfer_in", "transfer_in", "Transfer in"},
                {"return", "return", "Return"}}) {
            JSONObject choice = JsonFormUtils.getFieldJSONObject(visit.getJSONArray("options"), option[0]);
            assertEquals(option[1], choice.getString("openmrs_entity_id"));
            assertEquals(option[2], choice.getString("text"));
        }
        String sw = read("src/main/res/values-sw/strings.xml");
        for (String label : List.of("Hali ya mteja", "Aina ya hudhurio", "Mpya", "Amehamia", "Marudio",
                "Tarehe mteja alipoandikishwa kwenye programu")) {
            assertTrue(sw.contains(">" + label + "</string>"));
        }
    }

    private static KvpClientStatusActionHelper action(boolean hasPreviousClientStatus) throws Exception {
        KvpClientStatusActionHelper helper = new KvpClientStatusActionHelper(new MemberObject()) {
            @Override
            protected boolean hasPreviousClientStatus() {
                return hasPreviousClientStatus;
            }

            @Override
            protected String getScreeningDefaults() {
                return "[]";
            }
        };
        helper.onJsonFormLoaded(form().toString(), null, null);
        helper.getPreProcessed();
        return helper;
    }

    private static void assertStatus(KvpClientStatusActionHelper helper, JSONObject form, Status expected) {
        helper.onPayloadReceived(form.toString());
        assertEquals(expected, helper.evaluateStatusOnPayload());
    }

    private static Event event(JSONObject form) throws Exception {
        Event event = new Event();
        JsonFormUtils.addObservation(event, field(form, CLIENT_STATUS));
        JsonFormUtils.addObservation(event, field(form, ENROLLMENT_DATE));
        return event;
    }

    private static void projectRegisterDate(Event event, ContentValues values) throws Exception {
        JSONObject mappings = new JSONObject(read(ASSETS + "ec_client_fields.json"));
        JSONObject table = findBy(mappings.getJSONArray("bindobjects"), "name", "ec_kvp_register");
        JSONObject dateColumn = findBy(table.getJSONArray("columns"), "column_name", ENROLLMENT_DATE);
        Gson gson = new Gson();
        Column column = gson.fromJson(dateColumn.toString(), Column.class);
        HfClientProcessor processor = mock(HfClientProcessor.class, CALLS_REAL_METHODS);
        org.smartregister.domain.Event synced = gson.fromJson(gson.toJson(event), org.smartregister.domain.Event.class);
        processor.processCaseModel(synced, null, column, values);
    }

    private static Visit savedVisit(Event event, String submissionId) {
        Visit visit = new Visit();
        visit.setProcessed(false);
        visit.setFormSubmissionId(submissionId);
        visit.setPreProcessedJson(new Gson().toJson(event));
        return visit;
    }

    private static JSONObject filledForm(String visitType, String date) throws Exception {
        JSONObject form = form();
        field(form, CLIENT_STATUS).put("value", visitType);
        field(form, ENROLLMENT_DATE).put("value", date);
        field(form, OTHER_CATEGORY).put("value", new JSONArray(List.of("none")));
        return form;
    }

    private static JSONObject form() throws Exception {
        return new JSONObject(read(ASSETS + "json.form/kvp_client_status.json"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static JSONObject findBy(JSONArray objects, String key, String value) throws Exception {
        for (int i = 0; i < objects.length(); i++) {
            JSONObject object = objects.getJSONObject(i);
            if (value.equals(object.optString(key))) {
                return object;
            }
        }
        throw new AssertionError("Missing " + key + " " + value);
    }

    private static JSONObject field(JSONObject form, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(form.getJSONObject("step1").getJSONArray("fields"), key);
    }
}
