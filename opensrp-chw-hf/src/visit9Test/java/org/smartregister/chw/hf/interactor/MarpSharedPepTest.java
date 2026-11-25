package org.smartregister.chw.hf.interactor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.hf.interactor.MarpPepVisitInteractor.BIOMEDICAL;
import static org.smartregister.chw.hf.interactor.MarpPepVisitInteractor.STRUCTURAL;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.observation;

import android.app.Application;

import com.google.gson.Gson;

import org.json.JSONArray;
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
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.dao.MarpBiomedicalCompletionTest;
import org.smartregister.chw.hf.utils.MarpPep;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.contract.BaseKvpVisitContract;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status;
import org.smartregister.chw.kvp.repository.VisitDetailsRepository;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.AppExecutors;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.util.JsonFormUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28, manifest = Config.NONE, shadows = {
        MarpBiomedicalCompletionTest.EventData.class, MarpBiomedicalCompletionTest.FormSource.class,
        MarpBiomedicalCompletionTest.CloudantStore.class, MarpSharedPepTest.MemberData.class
})
public class MarpSharedPepTest {
    private static final String CLIENT = "visit-13-synthetic";
    private static final String SCREENING = "gbv_screening";
    private static final String SEXUAL = "sexual_violence";
    private static final String PHYSICAL = "physical_violence";
    private static final Gson GSON = new Gson();

    private final VisitRepository repository = mock(VisitRepository.class);
    private final VisitDetailsRepository detailsRepository = mock(VisitDetailsRepository.class);
    private final Map<String, Visit> rows = new LinkedHashMap<>();
    private final Map<String, List<VisitDetail>> details = new LinkedHashMap<>();

    @Before
    public void setUp() {
        Context context = mock(Context.class);
        AllSharedPreferences preferences = mock(AllSharedPreferences.class);
        when(context.allSharedPreferences()).thenReturn(preferences);
        when(context.applicationContext()).thenReturn(RuntimeEnvironment.getApplication());
        CoreLibrary.init(context);
        KvpLibrary.init(context, null, 67, 32);
        ReflectionHelpers.setField(KvpLibrary.getInstance(), "visitRepository", repository);
        ReflectionHelpers.setField(KvpLibrary.getInstance(), "visitDetailsRepository", detailsRepository);

        when(repository.getVisits(eq(CLIENT), anyString())).thenAnswer(call -> visits(call.getArgument(1)));
        when(repository.getLatestVisit(eq(CLIENT), anyString())).thenAnswer(call -> latest(call.getArgument(1)));
        when(repository.getVisitByVisitId(anyString())).thenAnswer(call -> rows.get(call.getArgument(0)));
        doAnswer(call -> {
            Visit visit = call.getArgument(0);
            rows.put(visit.getVisitId(), visit);
            return null;
        }).when(repository).addVisit(any(Visit.class));
        doAnswer(call -> {
            String id = call.getArgument(0);
            rows.remove(id);
            details.remove(id);
            return null;
        }).when(repository).deleteVisit(anyString());
        when(detailsRepository.getVisits(anyString())).thenAnswer(call ->
                details.getOrDefault(call.getArgument(0), List.of()));
        doAnswer(call -> {
            VisitDetail detail = call.getArgument(0);
            details.computeIfAbsent(detail.getVisitId(), unused -> new ArrayList<>()).add(detail);
            return null;
        }).when(detailsRepository).addVisitDetails(any(VisitDetail.class));
    }

    @After
    public void resetLibrary() {
        ReflectionHelpers.setStaticField(KvpLibrary.class, "instance", null);
        ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", null);
    }

    @Test
    public void onlySavedSexualViolenceShowsStructuralPep() throws Exception {
        KvpStructuralServiceInteractor structural = open(new KvpStructuralServiceInteractor(), false);
        assertNull(pep(structural));
        for (List<String> choices : List.of(List.of(PHYSICAL), List.of("none"), List.of("not_assessed"))) {
            saveGbv(structural, choices);
            assertNull(pep(structural));
        }
        saveGbv(structural, List.of(SEXUAL));
        assertEquals(Status.PENDING, pep(structural).getActionStatus());
        saveGbv(structural, List.of(PHYSICAL, SEXUAL));
        assertEquals(Status.PENDING, pep(structural).getActionStatus());
    }

    @Test
    public void structuralFirstUsesOneBiomedicalDraftAndKeepsTheAnswerAfterDeselection() throws Exception {
        KvpStructuralServiceInteractor structural = open(new KvpStructuralServiceInteractor(), false);
        saveGbv(structural, List.of(PHYSICAL, SEXUAL));
        savePep(structural, MarpPep.YES, MarpPep.NOT_PROVIDED);
        submit(structural, false);

        Visit draft = latest(BIOMEDICAL);
        assertTrue(MarpVisitStatus.isDraft(draft));
        assertEquals("", observation(event(STRUCTURAL), MarpPep.NEED));
        KvpBioMedicalServiceInteractor biomedical = open(new KvpBioMedicalServiceInteractor(true), true);
        assertPep(biomedical, Status.COMPLETED, MarpPep.YES, MarpPep.NOT_PROVIDED);
        saveBiomedicalPrerequisites(biomedical, "negative");
        submit(biomedical, true);

        assertEquals(draft.getVisitId(), latest(BIOMEDICAL).getVisitId());
        assertFalse(MarpVisitStatus.isDraft(latest(BIOMEDICAL)));
        structural = open(new KvpStructuralServiceInteractor(), true);
        assertPep(structural, Status.COMPLETED, MarpPep.YES, MarpPep.NOT_PROVIDED);
        saveGbv(structural, List.of(PHYSICAL));
        assertNull(pep(structural));
        submit(structural, true);
        assertEquals(1, visits(BIOMEDICAL).size());
        assertPep(open(new KvpBioMedicalServiceInteractor(true), true), Status.COMPLETED, MarpPep.YES,
                MarpPep.NOT_PROVIDED);
    }

    @Test
    public void biomedicalFirstNoBecomesReviewNeededAndStructuralCanConfirmItOnce() throws Exception {
        recordPriorNo();
        Visit before = latest(BIOMEDICAL);
        KvpStructuralServiceInteractor structural = triggerReview();
        assertEquals(before.getJson(), latest(BIOMEDICAL).getJson());

        KvpBioMedicalServiceInteractor biomedical = open(new KvpBioMedicalServiceInteractor(true), true);
        assertPep(biomedical, Status.PARTIALLY_COMPLETED, MarpPep.NO, "");
        assertEquals(RuntimeEnvironment.getApplication().getString(R.string.marp_pep_review_needed),
                pep(biomedical).getSubTitle());
        submit(biomedical, true);
        assertPep(open(new KvpBioMedicalServiceInteractor(true), true), Status.PARTIALLY_COMPLETED, MarpPep.NO, "");

        structural = open(new KvpStructuralServiceInteractor(), true);
        assertPep(structural, Status.PARTIALLY_COMPLETED, MarpPep.NO, "");
        savePep(structural, MarpPep.NO, "");
        assertPep(structural, Status.COMPLETED, MarpPep.NO, "");
        submit(structural, true);
        String reviewed = MarpPep.eventDetail(event(BIOMEDICAL), MarpPep.REVIEWED_TRIGGER);
        assertEquals(MarpPep.eventDetail(event(STRUCTURAL), MarpPep.REVIEW_TRIGGER), reviewed);
        assertEquals(before.getVisitId(), latest(BIOMEDICAL).getVisitId());

        structural = open(new KvpStructuralServiceInteractor(), true);
        saveGbv(structural, List.of(SEXUAL, PHYSICAL));
        submit(structural, true);
        assertEquals(reviewed, MarpPep.eventDetail(event(STRUCTURAL), MarpPep.REVIEW_TRIGGER));
        assertPep(open(new KvpBioMedicalServiceInteractor(true), true), Status.COMPLETED, MarpPep.NO, "");
        assertPep(open(new KvpStructuralServiceInteractor(), true), Status.COMPLETED, MarpPep.NO, "");

        structural = open(new KvpStructuralServiceInteractor(), true);
        saveGbv(structural, List.of("none"));
        submit(structural, true);
        assertPep(open(new KvpBioMedicalServiceInteractor(true), true), Status.COMPLETED, MarpPep.NO, "");
        structural = open(new KvpStructuralServiceInteractor(), true);
        saveGbv(structural, List.of(SEXUAL));
        submit(structural, true);
        assertNotEquals(reviewed, MarpPep.eventDetail(event(STRUCTURAL), MarpPep.REVIEW_TRIGGER));
        assertPep(open(new KvpBioMedicalServiceInteractor(true), true), Status.PARTIALLY_COMPLETED, MarpPep.NO, "");
    }

    @Test
    public void biomedicalCanReviseAfterTriggerAndStructuralReadsTheRevision() throws Exception {
        recordPriorNo();
        triggerReview();
        KvpBioMedicalServiceInteractor biomedical = open(new KvpBioMedicalServiceInteractor(true), true);
        savePep(biomedical, MarpPep.YES, "");
        assertPep(biomedical, Status.PARTIALLY_COMPLETED, "", "");
        savePep(biomedical, MarpPep.YES, MarpPep.PROVIDED);
        submit(biomedical, true);
        KvpStructuralServiceInteractor structural = open(new KvpStructuralServiceInteractor(), true);
        assertPep(structural, Status.COMPLETED, MarpPep.YES, MarpPep.PROVIDED);
        submit(structural, true);
        assertEquals(1, visits(BIOMEDICAL).size());
        assertEquals(1, event(BIOMEDICAL).getObs().stream()
                .filter(obs -> MarpPep.NEED.equals(obs.getFormSubmissionField())).count());
        assertEquals("negative", observation(event(BIOMEDICAL), "hiv_status"));
    }

    @Test
    public void positiveOrKnownPositiveHtsHidesBothEntries() throws Exception {
        for (String result : List.of("positive", "known_positive")) {
            rows.clear();
            details.clear();
            KvpBioMedicalServiceInteractor biomedical = open(new KvpBioMedicalServiceInteractor(false), false);
            saveBiomedicalPrerequisites(biomedical, result);
            assertNull(pep(biomedical));
            submit(biomedical, false);
            KvpStructuralServiceInteractor structural = open(new KvpStructuralServiceInteractor(), false);
            saveGbv(structural, List.of(SEXUAL));
            assertNull(pep(structural));
            submit(structural, false);
            assertNull(pep(open(new KvpBioMedicalServiceInteractor(true), true)));
        }
    }

    @Test
    public void earlierUnprocessedPositiveResultStillExcludesPep() throws Exception {
        KvpBioMedicalServiceInteractor biomedical = open(new KvpBioMedicalServiceInteractor(false), false);
        saveBiomedicalPrerequisites(biomedical, "positive");
        submit(biomedical, false);
        Calendar previousDay = Calendar.getInstance();
        previousDay.add(Calendar.DATE, -1);
        latest(BIOMEDICAL).setDate(previousDay.getTime());
        KvpStructuralServiceInteractor structural = open(new KvpStructuralServiceInteractor(), false);
        saveGbv(structural, List.of(SEXUAL));
        assertNull(pep(structural));
        submit(structural, false);
        assertNull(pep(open(new KvpBioMedicalServiceInteractor(false), false)));
    }

    @Test
    public void staleCardCannotOverwriteAProviderReviewSavedFromTheOtherCard() throws Exception {
        recordPriorNo();
        KvpStructuralServiceInteractor structural = triggerReview();
        KvpBioMedicalServiceInteractor biomedical = open(new KvpBioMedicalServiceInteractor(true), true);
        savePep(structural, MarpPep.NO, "");
        submit(structural, true);
        savePep(biomedical, MarpPep.YES, MarpPep.PROVIDED);
        assertThrows(IllegalStateException.class, () -> submit(biomedical, true));
        assertPep(open(new KvpBioMedicalServiceInteractor(true), true), Status.COMPLETED, MarpPep.NO, "");
    }

    @Test
    public void separateDayDoesNotInheritReviewAndHistoryKeepsItsOwnDate() throws Exception {
        recordPriorNo();
        triggerReview();
        String oldStructuralId = latest(STRUCTURAL).getVisitId();
        String oldBiomedicalId = latest(BIOMEDICAL).getVisitId();
        Calendar previousDay = Calendar.getInstance();
        previousDay.add(Calendar.DATE, -1);
        Date date = previousDay.getTime();
        latest(STRUCTURAL).setDate(date);
        latest(BIOMEDICAL).setDate(date);

        KvpStructuralServiceInteractor history = open(new KvpStructuralServiceInteractor(), true);
        assertPep(history, Status.PARTIALLY_COMPLETED, MarpPep.NO, "");
        submit(history, true);
        assertEquals(date, rows.get(oldStructuralId).getDate());

        KvpStructuralServiceInteractor today = open(new KvpStructuralServiceInteractor(), false);
        saveGbv(today, List.of(SEXUAL));
        assertEquals(Status.PENDING, pep(today).getActionStatus());
        submit(today, false);
        assertNotEquals(oldStructuralId, latest(STRUCTURAL).getVisitId());
        assertNull(MarpPep.eventDetail(event(STRUCTURAL), MarpPep.REVIEW_TRIGGER));
        assertEquals(MarpPep.NO, observation(MarpPepVisitInteractor.readEvent(rows.get(oldBiomedicalId)),
                MarpPep.NEED));
        assertNotNull(MarpPep.eventDetail(MarpPepVisitInteractor.readEvent(rows.get(oldStructuralId)),
                MarpPep.REVIEW_TRIGGER));
    }

    @Test
    public void revisedStructuralAssessmentRetainsOtherBiomedicalObservations() throws Exception {
        recordPriorNo();
        KvpStructuralServiceInteractor structural = triggerReview();
        Visit previous = latest(BIOMEDICAL);
        savePep(structural, MarpPep.YES, MarpPep.PROVIDED);
        submit(structural, true);
        Event original = MarpPepVisitInteractor.readEvent(previous);
        Event updated = event(BIOMEDICAL);
        List<String> changedFields = List.of(MarpPep.NEED, MarpPep.SERVICE);
        assertEquals(unrelated(original, changedFields), unrelated(updated, changedFields));
        assertEquals(previous.getVisitId(), latest(BIOMEDICAL).getVisitId());
        assertEquals(previous.getDate(), latest(BIOMEDICAL).getDate());
        assertEquals(1, visits(BIOMEDICAL).size());
        assertPep(open(new KvpBioMedicalServiceInteractor(true), true), Status.COMPLETED, MarpPep.YES,
                MarpPep.PROVIDED);
    }

    private static List<String> unrelated(Event event, List<String> changedFields) {
        return event.getObs().stream().filter(obs -> !changedFields.contains(obs.getFormSubmissionField()))
                .map(GSON::toJson).sorted().collect(Collectors.toList());
    }

    private KvpStructuralServiceInteractor triggerReview() throws Exception {
        KvpStructuralServiceInteractor structural = open(new KvpStructuralServiceInteractor(), false);
        saveGbv(structural, List.of(SEXUAL));
        assertPep(structural, Status.PARTIALLY_COMPLETED, MarpPep.NO, "");
        submit(structural, false);
        return open(new KvpStructuralServiceInteractor(), true);
    }

    private void recordPriorNo() throws Exception {
        KvpBioMedicalServiceInteractor biomedical = open(new KvpBioMedicalServiceInteractor(false), false);
        saveBiomedicalPrerequisites(biomedical, "negative");
        savePep(biomedical, MarpPep.NO, "");
        submit(biomedical, false);
    }

    private static <T extends MarpPepVisitInteractor> T open(T interactor, boolean edit) {
        MemberObject member = MemberData.getKvpMember(CLIENT);
        ReflectionHelpers.setField(interactor, "memberObject", member);
        ReflectionHelpers.setField(interactor, "appExecutors",
                new AppExecutors(Runnable::run, Runnable::run, Runnable::run));
        BaseKvpVisitContract.View view = mock(BaseKvpVisitContract.View.class);
        when(view.getEditMode()).thenReturn(edit);
        when(view.getContext()).thenReturn(RuntimeEnvironment.getApplication());
        interactor.calculateActions(view, member, mock(BaseKvpVisitContract.InteractorCallBack.class));
        assertFalse(actions(interactor).isEmpty());
        return interactor;
    }

    private static void saveGbv(KvpStructuralServiceInteractor card, List<String> choices) throws Exception {
        BaseKvpVisitAction gbv = action(card, R.string.kvp_gbv_analysis);
        JSONObject form = new JSONObject(gbv.getJsonPayload());
        JSONObject screen = field(form, SCREENING).put("value", new JSONArray(choices));
        JSONArray options = screen.getJSONArray("options");
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            option.put("value", choices.contains(option.getString("key")));
        }
        field(form, "post_gbv_care_provision").put("value", "yes");
        gbv.setJsonPayload(form.toString());
        card.onGbvFormSaved();
    }

    private static void saveBiomedicalPrerequisites(KvpBioMedicalServiceInteractor card, String result)
            throws Exception {
        BaseKvpVisitAction status = action(card, R.string.kvp_client_status);
        JSONObject form = new JSONObject(status.getJsonPayload());
        field(form, "client_status").put("value", "new_client");
        status.setJsonPayload(form.toString());

        BaseKvpVisitAction hts = action(card, R.string.kvp_hts);
        form = new JSONObject(hts.getJsonPayload());
        field(form, "tested_hiv").put("value", "yes");
        field(form, "hiv_status").put("value", result);
        hts.setJsonPayload(form.toString());
    }

    private static void savePep(MarpPepVisitInteractor card, String need, String service) throws Exception {
        BaseKvpVisitAction assessment = pep(card);
        JSONObject form = card.preparePepForm(new JSONObject(assessment.getJsonPayload()));
        field(form, MarpPep.NEED).put("value", need);
        field(form, MarpPep.SERVICE).put("value", service);
        assessment.setJsonPayload(form.toString());
        card.onPepFormSaved();
    }

    private static void assertPep(MarpPepVisitInteractor card, Status status, String need, String service) {
        assertEquals(status, pep(card).getActionStatus());
        String payload = pep(card).getJsonPayload();
        assertEquals(need, MarpPep.payloadAnswer(payload, MarpPep.NEED));
        assertEquals(service, MarpPep.payloadAnswer(payload, MarpPep.SERVICE));
    }

    private static JSONObject field(JSONObject form, String name) throws Exception {
        return JsonFormUtils.getFieldJSONObject(form.getJSONObject("step1").getJSONArray("fields"), name);
    }

    private static BaseKvpVisitAction pep(MarpPepVisitInteractor card) {
        return action(card, R.string.kvp_pep_assesment);
    }

    private static BaseKvpVisitAction action(MarpPepVisitInteractor card, int label) {
        return actions(card).get(RuntimeEnvironment.getApplication().getString(label));
    }

    private static Map<String, BaseKvpVisitAction> actions(MarpPepVisitInteractor card) {
        return ReflectionHelpers.getField(card, "actionList");
    }

    private static void submit(MarpPepVisitInteractor card, boolean edit) throws Exception {
        card.saveCurrentCard(edit, CLIENT, actions(card), "");
    }

    private List<Visit> visits(String type) {
        return rows.values().stream().filter(visit -> type.equals(visit.getVisitType()))
                .collect(Collectors.toList());
    }

    private Visit latest(String type) {
        return visits(type).stream().max(Comparator.comparing(Visit::getDate)).orElse(null);
    }

    private Event event(String type) {
        return MarpPepVisitInteractor.readEvent(latest(type));
    }

    @Implements(org.smartregister.chw.kvp.dao.KvpDao.class)
    public static class MemberData {
        @Implementation
        protected static MemberObject getKvpMember(String memberId) {
            MemberObject member = mock(MemberObject.class);
            when(member.getBaseEntityId()).thenReturn(memberId);
            when(member.getGender()).thenReturn(Constants.FEMALE);
            when(member.getAge()).thenReturn(30);
            return member;
        }

        @Implementation
        protected static String getDominantKVPGroup(String memberId) {
            return "women_at_high_risk";
        }

        @Implementation
        protected static MemberObject getPrEPMember(String memberId) {
            return null;
        }
    }
}
