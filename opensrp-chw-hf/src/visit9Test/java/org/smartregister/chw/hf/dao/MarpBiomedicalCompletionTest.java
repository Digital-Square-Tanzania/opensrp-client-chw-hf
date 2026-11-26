package org.smartregister.chw.hf.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.BREASTFEEDING;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.NEITHER;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.PREGNANT;

import android.app.Application;

import com.google.gson.Gson;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.smartregister.chw.hf.utils.MarpPregnancyStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpVisitsUtil;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.dao.AbstractDao;
import org.smartregister.repository.AllSharedPreferences;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE, shadows = {
        MarpBiomedicalCompletionTest.MemberData.class, MarpBiomedicalCompletionTest.EventData.class,
        MarpBiomedicalCompletionTest.ProcessingSink.class
})
public class MarpBiomedicalCompletionTest {
    private static final String PATIENT = "synthetic-visit-9";
    private static final String BIOMEDICAL = Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
    private static final String STRUCTURAL = Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT;
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
        KvpLibrary.init(mock(Context.class), null, 67, 32, HfKvpDao::isFamilyPlanningExempt);
        ReflectionHelpers.setField(KvpLibrary.getInstance(), "visitRepository", visits);
    }

    @After
    public void resetLibrary() {
        ReflectionHelpers.setStaticField(KvpLibrary.class, "instance", null);
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

    private static Visit biomedical(String id, long date, String status, boolean familyPlanning, boolean tb) {
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
        add(event, "cervical_cancer_screening", "no");
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
        event.addObs(new Obs().withFormSubmissionField(field).withFieldCode(field).withValue(value));
    }

    private static void screenPregnant() {
        Event event = event("screening", EARLIER, Constants.EVENT_TYPE.KVP_REGISTRATION);
        add(event, MarpPregnancyScreening.PREGNANCY_FIELD, "yes");
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
            return Constants.EVENT_TYPE.KVP_REGISTRATION.equals(args[1]) ? screening : synced;
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
