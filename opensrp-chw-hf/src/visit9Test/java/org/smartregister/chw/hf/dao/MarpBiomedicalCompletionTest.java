package org.smartregister.chw.hf.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;
import static org.smartregister.chw.hf.actionhelper.kvp.KvpClientStatusActionHelper.OTHER_CATEGORY;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.observation;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.BREASTFEEDING;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.NEITHER;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.PREGNANT;

import android.app.Application;
import android.os.Looper;

import com.google.gson.Gson;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.interactor.KvpBioMedicalServiceInteractor;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.smartregister.chw.hf.utils.MarpPregnancyStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.contract.BaseKvpVisitContract;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.AppExecutors;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpVisitsUtil;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.dao.AbstractDao;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.sync.CloudantDataHandler;
import org.smartregister.util.JsonFormUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE, shadows = {
        MarpBiomedicalCompletionTest.MemberData.class, MarpBiomedicalCompletionTest.EventData.class,
        MarpBiomedicalCompletionTest.ProcessingSink.class, MarpBiomedicalCompletionTest.FormSource.class,
        MarpBiomedicalCompletionTest.CloudantStore.class
})
public class MarpBiomedicalCompletionTest {
    private static final String PATIENT = "synthetic-visit-9";
    private static final String BIOMEDICAL = Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
    private static final String STRUCTURAL = Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT;
    private static final String CERVICAL = "cervical_cancer_screening";
    private static final long NOW = System.currentTimeMillis();
    private static final long VISIT_DATE = NOW - TimeUnit.DAYS.toMillis(4);
    private static final long EARLIER = VISIT_DATE - TimeUnit.DAYS.toMillis(1);
    private static final long LATER = VISIT_DATE + TimeUnit.DAYS.toMillis(1);
    private static final Gson GSON = new Gson();

    private final VisitRepository visits = mock(VisitRepository.class);

    @Before
    public void setUp() {
        MemberData.gender = Constants.FEMALE;
        MemberData.group = MarpPregnancyScreening.GROUP_CODE;
        EventData.screening = List.of();
        EventData.synced = List.of();
        ProcessingSink.events.clear();
        ProcessingSink.starts = 0;
        Context appContext = mock(Context.class);
        CoreLibrary.init(appContext);
        KvpLibrary.init(appContext, null, 67, 32,
                HfKvpDao::isPregnantForBiomedicalVisit, HfKvpDao::isPregnantForBiomedicalVisit);
        ReflectionHelpers.setField(KvpLibrary.getInstance(), "visitRepository", visits);
    }

    @After
    public void resetLibrary() {
        ReflectionHelpers.setStaticField(KvpLibrary.class, "instance", null);
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", null);
    }

    @Test
    public void savedPregnantCompletesWithoutFamilyPlanning() {
        Visit current = biomedical("saved", VISIT_DATE, PREGNANT, false, true);
        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(current));
    }

    @Test
    public void eitherSavedRestoreAnswerWinsOverEarlierPregnancy() {
        when(visits.getVisits(PATIENT, BIOMEDICAL))
                .thenReturn(List.of(biomedical("prior", EARLIER, PREGNANT, false, true)));
        screenPregnant();

        for (String status : List.of(BREASTFEEDING, NEITHER)) {
            assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(
                    biomedical("restored", VISIT_DATE, status, false, true)));
            assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(
                    biomedical("answered", VISIT_DATE, status, true, true)));
        }
    }

    @Test
    public void usesEarlierSubmittedStatusFromLocalOrRestoredHistory() {
        Visit current = biomedical("current", VISIT_DATE, "", false, true);
        Visit earlier = biomedical("prior", EARLIER, PREGNANT, false, true);
        Visit later = biomedical("later", LATER, NEITHER, false, true);
        when(visits.getVisits(PATIENT, BIOMEDICAL)).thenReturn(List.of(earlier, current, later));
        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(current));

        when(visits.getVisits(PATIENT, BIOMEDICAL)).thenReturn(List.of(current));
        EventData.synced = List.of(row(later.getJson()), row(earlier.getJson()));
        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(current));

        screenPregnant();
        EventData.synced = List.of(row(biomedical("prior-neither", EARLIER, NEITHER, false, true).getJson()));
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(current));
    }

    @Test
    public void screeningFallbackAppliesOnlyOnTheFirstVisit() {
        Visit current = biomedical("first", VISIT_DATE, "", false, true);
        screenPregnant();
        when(visits.getVisits(PATIENT, BIOMEDICAL)).thenReturn(List.of(current));
        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(current));

        Visit earlier = new Visit();
        earlier.setDate(new Date(EARLIER));
        earlier.setFormSubmissionId("prior-structural");
        when(visits.getVisits(PATIENT, STRUCTURAL)).thenReturn(List.of(earlier));
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(current));
    }

    @Test
    public void otherClientsAndDefaultLibraryStillRequireFamilyPlanning() {
        Visit current = biomedical("current", VISIT_DATE, PREGNANT, false, true);
        MemberData.group = "women_at_high_risk";
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(current));
        MemberData.gender = Constants.MALE;
        MemberData.group = MarpPregnancyScreening.GROUP_CODE;
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(current));

        resetLibrary();
        KvpLibrary.init(mock(Context.class), null, 67, 32);
        MemberData.gender = Constants.FEMALE;
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(current));
    }

    @Test
    public void processingUsesCompletionAndKeepsTheExistingAgeGate() throws Exception {
        Visit pregnant = biomedical("complete-pregnant", VISIT_DATE, PREGNANT, false, true);
        Visit missingTb = biomedical("missing-tb", VISIT_DATE, PREGNANT, false, false);
        Visit recent = biomedical("too-recent", NOW, PREGNANT, false, true);
        Visit noFamilyPlanning = biomedical("not-pregnant", VISIT_DATE, NEITHER, false, true);
        Visit comparison = biomedical("comparison", VISIT_DATE, NEITHER, true, true);
        when(visits.getAllUnSynced()).thenReturn(List.of(pregnant, missingTb, recent, noFamilyPlanning, comparison));

        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(missingTb));
        KvpVisitsUtil.processVisits();

        assertEquals(2, ProcessingSink.events.size());
        assertEquals(1, ProcessingSink.starts);
        Event processed = ProcessingSink.events.get(0);
        assertEquals(pregnant.getFormSubmissionId(), processed.getFormSubmissionId());
        assertTrue(processed.getObs().stream().anyMatch(obs -> "hiv_status".equals(obs.getFieldCode())
                && obs.getValues().equals(List.of("negative"))));
        assertFalse(processed.getObs().stream().anyMatch(obs -> "family_planning_service".equals(obs.getFieldCode())));
        assertEquals(comparison.getFormSubmissionId(), ProcessingSink.events.get(1).getFormSubmissionId());
        verify(visits).completeProcessing(pregnant.getVisitId());
        verify(visits).completeProcessing(comparison.getVisitId());
    }

    @Test
    public void cervicalScreeningCanBeAbsentForPregnancyWithoutExemptingOtherServices() {
        Visit pregnant = biomedical("pregnant", VISIT_DATE, PREGNANT, false, true, null);
        Visit missingTb = biomedical("missing-tb", VISIT_DATE, PREGNANT, false, false, null);

        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(pregnant));
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(missingTb));
    }

    @Test
    public void cervicalScreeningCompletionUsesEarlierStatusAsOfTheVisit() {
        Visit current = biomedical("current", VISIT_DATE, "", true, true, null);
        Visit earlier = biomedical("earlier", EARLIER, PREGNANT, false, true, null);
        Visit later = biomedical("later", LATER, NEITHER, true, true);
        when(visits.getVisits(PATIENT, BIOMEDICAL)).thenReturn(List.of(earlier, current, later));
        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(current));

        when(visits.getVisits(PATIENT, BIOMEDICAL)).thenReturn(List.of(current));
        EventData.synced = List.of(row(later.getJson()), row(earlier.getJson()));
        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(current));

        screenPregnant();
        Visit breastfeeding = biomedical("earlier-breastfeeding", EARLIER, BREASTFEEDING, true, true);
        EventData.synced = List.of(row(later.getJson()), row(breastfeeding.getJson()));
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(current));
    }

    @Test
    public void cervicalScreeningCompletionOnlyFallsBackToPregnantScreeningOnTheFirstVisit() {
        Visit current = biomedical("first", VISIT_DATE, "", true, true, null);
        screenPregnant();
        when(visits.getVisits(PATIENT, BIOMEDICAL)).thenReturn(List.of(current));
        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(current));

        Visit earlier = new Visit();
        earlier.setDate(new Date(EARLIER));
        earlier.setFormSubmissionId("earlier-structural");
        when(visits.getVisits(PATIENT, STRUCTURAL)).thenReturn(List.of(earlier));
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(current));
    }

    @Test
    public void cervicalScreeningRemainsRequiredAfterEitherSavedNonpregnantAnswer() {
        Visit earlier = biomedical("previous-pregnancy", EARLIER, PREGNANT, false, true, null);
        when(visits.getVisits(PATIENT, BIOMEDICAL)).thenReturn(List.of(earlier));
        screenPregnant();
        for (String status : List.of(NEITHER, BREASTFEEDING)) {
            assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(
                    biomedical("unanswered", VISIT_DATE, status, true, true, null)));
            assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(
                    biomedical("answered", VISIT_DATE, status, true, true, "not_screened")));
        }
    }

    @Test
    public void cervicalScreeningIsRequiredForWhrAndHasNoCompletionCheckForMaleClients() {
        Visit unanswered = biomedical("unanswered", VISIT_DATE, "", true, true, null);
        MemberData.group = "women_at_high_risk";
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(unanswered));
        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(
                biomedical("answered", VISIT_DATE, "", true, true, "screened")));

        MemberData.gender = Constants.MALE;
        assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBioMedicalStatus(unanswered));
    }

    @Test
    public void cervicalScreeningDefaultsToRequiredForExistingKvpConsumers() {
        Visit current = biomedical("current", VISIT_DATE, PREGNANT, false, true, null);
        resetLibrary();
        KvpLibrary.init(mock(Context.class), null, 67, 32);
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(current));

        resetLibrary();
        KvpLibrary.init(mock(Context.class), null, 67, 32, (visit, gender) -> true);
        assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBioMedicalStatus(current));
    }

    @Test
    public void cervicalScreeningProcessingPreservesRecordedAnswersAndOmitsTheExemptField() throws Exception {
        Visit pregnant = biomedical("pregnant", VISIT_DATE, PREGNANT, false, true, null);
        Visit pregnantWithHistory = biomedical("pregnant-with-history", VISIT_DATE, PREGNANT, false, true, "screened");
        Visit breastfeeding = biomedical("breastfeeding", VISIT_DATE, BREASTFEEDING, true, true, "not_screened");
        Visit missingCervical = biomedical("missing-cervical", VISIT_DATE, NEITHER, true, true, null);
        Visit missingTb = biomedical("missing-tb", VISIT_DATE, PREGNANT, false, false, null);
        Visit tooRecent = biomedical("too-recent", NOW, PREGNANT, false, true, null);
        when(visits.getAllUnSynced()).thenReturn(
                List.of(pregnant, pregnantWithHistory, breastfeeding, missingCervical, missingTb, tooRecent));

        KvpVisitsUtil.processVisits();

        assertEquals(3, ProcessingSink.events.size());
        Event processedPregnant = ProcessingSink.events.get(0);
        assertEquals(pregnant.getFormSubmissionId(), processedPregnant.getFormSubmissionId());
        assertFalse(processedPregnant.getObs().stream().anyMatch(obs -> CERVICAL.equals(obs.getFormSubmissionField())));
        assertEquals(pregnantWithHistory.getFormSubmissionId(), ProcessingSink.events.get(1).getFormSubmissionId());
        assertEquals("screened", observation(ProcessingSink.events.get(1), CERVICAL));
        assertEquals(breastfeeding.getFormSubmissionId(), ProcessingSink.events.get(2).getFormSubmissionId());
        assertEquals("not_screened", observation(ProcessingSink.events.get(2), CERVICAL));
        verify(visits).completeProcessing(pregnant.getVisitId());
        verify(visits).completeProcessing(pregnantWithHistory.getVisitId());
        verify(visits).completeProcessing(breastfeeding.getVisitId());
        verify(visits, never()).completeProcessing(missingCervical.getVisitId());
        verify(visits, never()).completeProcessing(missingTb.getVisitId());
        verify(visits, never()).completeProcessing(tooRecent.getVisitId());
    }

    @Test
    public void cervicalScreeningFollowsSexAndVisitNinePregnancyPrecedence() {
        screen("yes");
        assertFalse(new Checklist(null).showsCervicalScreening());

        screen("no");
        assertTrue(new Checklist(null).showsCervicalScreening());

        when(visits.getVisits(PATIENT, BIOMEDICAL))
                .thenReturn(List.of(biomedical("prior", EARLIER, PREGNANT, false, true)));
        assertFalse(new Checklist(null).showsCervicalScreening());
        screen("yes");
        when(visits.getVisits(PATIENT, BIOMEDICAL))
                .thenReturn(List.of(biomedical("prior", EARLIER, NEITHER, true, true)));
        assertTrue(new Checklist(null).showsCervicalScreening());

        MemberData.group = "women_at_high_risk";
        assertTrue(new Checklist(null).showsCervicalScreening());
        MemberData.gender = Constants.MALE;
        assertFalse(new Checklist(null).showsCervicalScreening());
    }

    @Test
    public void cervicalScreeningRefreshesAfterStatusSaveAndRestoresTheEditedAnswer() throws Exception {
        screen("yes");
        Checklist checklist = new Checklist(null);
        assertFalse(checklist.showsCervicalScreening());
        checklist.saveClientStatus(BREASTFEEDING);
        BaseKvpVisitAction screening = checklist.cervicalScreening();
        JSONObject payload = new JSONObject(screening.getJsonPayload());
        field(payload, CERVICAL).put("value", "screened");
        screening.setJsonPayload(payload.toString());

        checklist.saveClientStatus(PREGNANT);
        assertFalse(checklist.showsCervicalScreening());
        checklist.saveClientStatus(NEITHER);
        assertSame(screening, checklist.cervicalScreening());
        assertEquals(BaseKvpVisitAction.Status.COMPLETED, screening.getActionStatus());
        assertEquals("screened", field(new JSONObject(screening.getJsonPayload()), CERVICAL).getString("value"));
    }

    @Test
    public void cervicalScreeningHistoryKeepsRecordedAnswerWhenHiddenAndReopensItWhenRestored() throws Exception {
        Event original = event("original", EARLIER, BIOMEDICAL);
        add(original, "client_status", "new_client");
        add(original, MarpPregnancyStatus.FIELD, BREASTFEEDING);
        add(original, OTHER_CATEGORY, "none");
        add(original, CERVICAL, "not_screened");
        Checklist history = new Checklist(NCUtil.eventsObsToDetails(original.getObs(), "original", PATIENT));
        assertEquals("not_screened", field(new JSONObject(history.cervicalScreening().getJsonPayload()),
                CERVICAL).getString("value"));

        history.saveClientStatus(PREGNANT);
        assertFalse(history.showsCervicalScreening());
        Event replacement = event("replacement", VISIT_DATE, BIOMEDICAL);
        add(replacement, MarpPregnancyStatus.FIELD, PREGNANT);
        add(replacement, "client_status", "new_client");
        add(replacement, OTHER_CATEGORY, "none");
        ReflectionHelpers.setField(history, "replacedBiomedicalEvent", original);
        history.prepareEvent(replacement);
        assertEquals("not_screened", observation(replacement, CERVICAL));

        Checklist reopened = new Checklist(NCUtil.eventsObsToDetails(replacement.getObs(), "replacement", PATIENT));
        assertFalse(reopened.showsCervicalScreening());
        reopened.saveClientStatus(NEITHER);
        assertEquals("not_screened", field(new JSONObject(reopened.cervicalScreening().getJsonPayload()),
                CERVICAL).getString("value"));
    }

    private static JSONObject field(JSONObject form, String key) throws Exception {
        return JsonFormUtils.getFieldJSONObject(form.getJSONObject("step1").getJSONArray("fields"), key);
    }

    private static class Checklist extends KvpBioMedicalServiceInteractor {
        private final BaseKvpVisitContract.InteractorCallBack callback = mock(BaseKvpVisitContract.InteractorCallBack.class);

        Checklist(Map<String, List<VisitDetail>> savedDetails) {
            super(savedDetails != null);
            memberObject = KvpDao.getKvpMember(PATIENT);
            context = RuntimeEnvironment.getApplication();
            appExecutors = new AppExecutors(Runnable::run, Runnable::run, Runnable::run);
            details = savedDetails;
            Timber.Tree checklistErrors = new Timber.Tree() {
                @Override
                protected void log(int priority, String tag, String message, Throwable error) {
                    if (error != null) {
                        throw new AssertionError("Checklist initialization failed", error);
                    }
                }
            };
            Timber.plant(checklistErrors);
            try {
                populateActionList(callback);
            } finally {
                Timber.uproot(checklistErrors);
            }
            assertTrue(actionList.containsKey(context.getString(R.string.kvp_client_status)));
        }

        boolean showsCervicalScreening() {
            return cervicalScreening() != null;
        }

        BaseKvpVisitAction cervicalScreening() {
            return actionList.get(context.getString(R.string.kvp_cervical_screening));
        }

        void saveClientStatus(String status) throws Exception {
            BaseKvpVisitAction action = actionList.get(context.getString(R.string.kvp_client_status));
            JSONObject form = new JSONObject(action.getJsonPayload());
            field(form, "client_status").put("value", "new_client");
            field(form, OTHER_CATEGORY).put("value", "none");
            field(form, MarpPregnancyStatus.FIELD).put("value", status);
            clearInvocations(callback);
            action.setJsonPayload(form.toString());
            shadowOf(Looper.getMainLooper()).idle();
            assertEquals(BaseKvpVisitAction.Status.COMPLETED, action.getActionStatus());
            verify(callback, atLeastOnce()).preloadActions(actionList);
        }

        @Override
        protected void prepareEvent(Event event) {
            super.prepareEvent(event);
        }
    }

    private static Visit biomedical(String id, long date, String status, boolean familyPlanning, boolean tb) {
        return biomedical(id, date, status, familyPlanning, tb, "not_screened");
    }

    private static Visit biomedical(String id, long date, String status, boolean familyPlanning, boolean tb,
                                    String cervicalAnswer) {
        Event event = event(id, date, BIOMEDICAL);
        add(event, "client_status", "new_client");
        if (!status.isEmpty()) {
            add(event, MarpPregnancyStatus.FIELD, status);
        }
        add(event, "condoms_given", "no");
        add(event, "tested_hiv", "yes");
        add(event, "hiv_status", "negative");
        add(event, "hep_b_testing", "no");
        add(event, "hep_c_testing", "no");
        if (cervicalAnswer != null) {
            add(event, CERVICAL, cervicalAnswer);
        }
        add(event, "vmcc_provided", "no");
        if (tb) {
            add(event, "tb_screening", "no");
        }
        if (familyPlanning) {
            add(event, "family_planning_service", "no");
        }

        Visit visit = new Visit();
        visit.setVisitId(id);
        visit.setBaseEntityId(PATIENT);
        visit.setDate(new Date(date));
        visit.setUpdatedAt(new Date(date));
        visit.setVisitType(BIOMEDICAL);
        visit.setProcessed(false);
        visit.setFormSubmissionId(id);
        visit.setJson(GSON.toJson(event));
        visit.setPreProcessedJson(visit.getJson());
        return visit;
    }

    private static Event event(String id, long date, String type) {
        Event event = new Event();
        event.setFormSubmissionId(id);
        event.setEventDate(new Date(date));
        event.setEventType(type);
        event.setBaseEntityId(PATIENT);
        return event;
    }

    private static void add(Event event, String field, String value) {
        event.addObs(new Obs().withFormSubmissionField(field).withFieldCode(field).withValue(value)
                .withHumanReadableValues(List.of()));
    }

    private static void screenPregnant() {
        screen("yes");
    }

    private static void screen(String pregnant) {
        Event event = event("screening", EARLIER, Constants.EVENT_TYPE.KVP_REGISTRATION);
        add(event, MarpPregnancyScreening.PREGNANCY_FIELD, pregnant);
        add(event, MarpPregnancyScreening.BREASTFEEDING_FIELD, "yes");
        EventData.screening = List.of(row(GSON.toJson(event)));
    }

    private static Map<String, Object> row(String json) {
        return Map.of("json", json);
    }

    @Implements(KvpDao.class)
    public static class MemberData {
        static String gender;
        static String group;

        @Implementation
        protected static MemberObject getKvpMember(String baseEntityId) {
            MemberObject member = mock(MemberObject.class);
            when(member.getGender()).thenReturn(gender);
            when(member.getBaseEntityId()).thenReturn(PATIENT);
            when(member.getAge()).thenReturn(30);
            return member;
        }

        @Implementation
        protected static String getDominantKVPGroup(String baseEntityId) {
            return group;
        }

        @Implementation
        protected static String getKvpVmmcServices(String baseEntityId) {
            return "";
        }
    }

    @Implements(AbstractDao.class)
    public static class EventData {
        static List<Map<String, Object>> screening;
        static List<Map<String, Object>> synced;

        @Implementation
        protected static List<Map<String, Object>> readData(String sql, String[] args) {
            return args.length < 2 ? List.of()
                    : Constants.EVENT_TYPE.KVP_REGISTRATION.equals(args[1]) ? screening : synced;
        }

        @Implementation
        protected static <T> List<T> readData(String sql, AbstractDao.DataMap<T> mapper) {
            return List.of();
        }
    }

    @Implements(FormUtils.class)
    public static class FormSource {
        @Implementation
        protected static org.smartregister.util.FormUtils getFormUtils() throws Exception {
            return org.smartregister.util.FormUtils.getInstance(RuntimeEnvironment.getApplication());
        }
    }

    @Implements(CloudantDataHandler.class)
    public static class CloudantStore {
        @Implementation
        protected static CloudantDataHandler getInstance(android.content.Context context) {
            return null;
        }
    }

    @Implements(NCUtil.class)
    public static class ProcessingSink {
        static final List<Event> events = new ArrayList<>();
        static int starts;

        @Implementation
        protected static void addEvent(AllSharedPreferences preferences, Event event) {
            events.add(event);
        }

        @Implementation
        protected static void startClientProcessing() {
            starts++;
        }
    }
}
