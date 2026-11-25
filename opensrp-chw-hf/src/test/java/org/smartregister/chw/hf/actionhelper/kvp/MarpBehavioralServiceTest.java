package org.smartregister.chw.hf.actionhelper.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationDeliveryTest.CHANNEL;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationDeliveryTest.MODES;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.OTHER_TEXT;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.TOPICS;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.answerChecklist;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.event;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.field;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.form;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.observation;

import android.app.Application;
import android.content.res.Configuration;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.CoreLibrary;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.activity.KvpBehavioralServiceActivity;
import org.smartregister.chw.hf.dao.HfKvpDao;
import org.smartregister.chw.hf.interactor.KvpBehavioralServiceInteractor;
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
import org.smartregister.chw.kvp.util.KvpVisitsUtil;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.repository.BaseRepository;
import org.smartregister.sync.ClientProcessorForJava;
import org.smartregister.sync.helper.ECSyncHelper;
import org.smartregister.util.FormUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 28)
public class MarpBehavioralServiceTest {
    private static final String OTHER = "other";
    private static final Locale SWAHILI = new Locale("sw");

    @Test
    public void behavioralHealthEducationAloneEnablesSubmitAndReopensAsComplete() throws Exception {
        try (BehavioralFixture fixture = new BehavioralFixture()) {
            fixture.openCard("synthetic-new-client", null, Locale.ENGLISH);
            assertEquals(Status.PENDING, fixture.action().getActionStatus());
            assertFalse(fixture.action().isOptional());
            assertFalse(fixture.submitButton.hasOnClickListeners());

            JSONObject none = new JSONObject(fixture.action().getJsonPayload());
            answerChecklist(field(none, TOPICS), List.of("none"));
            fixture.saveForm(none);
            assertEquals(Status.COMPLETED, fixture.action().getActionStatus());
            fixture.submit(true);

            Visit saved = fixture.savedVisit();
            assertEquals(BehavioralFixture.VISIT_TYPE, saved.getVisitType());
            assertEquals(List.of("none"), observation(fixture.savedEvent(), TOPICS).getValues());
            assertNull(observation(fixture.savedEvent(), BehavioralFixture.COUNT));
            assertFalse(fixture.rows(saved).stream().anyMatch(row -> BehavioralFixture.COUNT.equals(row.getVisitKey())));
            fixture.openCard(saved.getBaseEntityId(), saved.getVisitId(), SWAHILI);
            assertEquals(Status.COMPLETED, fixture.action().getActionStatus());
            assertTrue(fixture.submitButton.hasOnClickListeners());
        }
    }

    @Test
    public void behavioralSubmitAndHistoryEditReplaceDeliveryForTheSameVisit() throws Exception {
        try (BehavioralFixture fixture = new BehavioralFixture()) {
            fixture.openCard("synthetic-current-client", null, Locale.ENGLISH);
            JSONObject filled = new JSONObject(fixture.action().getJsonPayload());
            answerChecklist(field(filled, TOPICS), List.of("sti", "tb"));
            answerChecklist(field(filled, MODES + "sti"), List.of("leaflets", "health_care_provider"));
            answerChecklist(field(filled, MODES + "tb"), List.of(OTHER));
            field(filled, CHANNEL + "tb").put("value", "Synthetic TB session");
            fixture.saveForm(filled);
            fixture.submit(true);

            Visit saved = fixture.savedVisit();
            Map<String, List<Object>> expected = Map.of(MODES + "sti", List.of("leaflets", "health_care_provider"),
                    MODES + "tb", List.of(OTHER), CHANNEL + "tb", List.of("Synthetic TB session"));
            assertEquals(expected, deliveries(fixture.savedEvent()));
            assertNull(observation(fixture.savedEvent(), BehavioralFixture.COUNT));
            fixture.openCard(saved.getBaseEntityId(), saved.getVisitId(), SWAHILI);
            assertEquals(Status.COMPLETED, fixture.action().getActionStatus());
            assertEquals(expected, deliveries(event(fixture.action().getJsonPayload())));

            JSONObject none = new JSONObject(fixture.action().getJsonPayload());
            answerChecklist(field(none, TOPICS), List.of("none"));
            fixture.saveForm(none);
            fixture.submit(true);
            verify(fixture.detailsRepository).deleteVisitDetails(saved.getVisitId());
            assertEquals(saved.getVisitId(), fixture.savedVisit().getVisitId());
            assertTrue(deliveries(fixture.savedEvent()).isEmpty());
            assertNull(observation(fixture.savedEvent(), BehavioralFixture.COUNT));
        }
    }

    @Test
    public void behavioralLegacyCountSurvivesUntouchedAndChangedEducationOnItsOwnVisit() throws Exception {
        try (BehavioralFixture fixture = new BehavioralFixture()) {
            Visit older = fixture.legacy("synthetic-client", "older-visit", "3");
            Visit original = fixture.legacy("synthetic-client", "selected-visit", "12");
            Visit otherClient = fixture.legacy("synthetic-other-client", "other-client-visit", "29");
            List<String> originalRows = fixture.countRows(original);
            String originalObservation = fixture.countObservation(original);

            fixture.openCard(original.getBaseEntityId(), original.getVisitId(), SWAHILI);
            assertEquals(Status.COMPLETED, fixture.action().getActionStatus());
            assertFalse(field(new JSONObject(fixture.action().getJsonPayload()), BehavioralFixture.COUNT).has("key"));
            assertTrue(fixture.submitButton.hasOnClickListeners());
            fixture.submit(true);
            fixture.assertRetained(original, originalObservation, originalRows);
            assertEquals(List.of("tb", OTHER), observation(fixture.savedEvent(), TOPICS).getValues());
            assertEquals(List.of("Original synthetic topic"), observation(fixture.savedEvent(), OTHER_TEXT).getValues());
            assertTrue(deliveries(fixture.savedEvent()).isEmpty());

            fixture.openCard(original.getBaseEntityId(), original.getVisitId(), Locale.ENGLISH);
            JSONObject changed = new JSONObject(fixture.action().getJsonPayload());
            answerChecklist(field(changed, TOPICS), List.of("sti"));
            assertStatus(fixture.action(), changed, Status.PENDING);
            answerChecklist(field(changed, MODES + "sti"), List.of("television"));
            fixture.saveForm(changed);
            fixture.submit(true);
            fixture.assertRetained(original, originalObservation, originalRows);
            assertEquals(List.of("sti"), observation(fixture.savedEvent(), TOPICS).getValues());
            assertEquals(Map.of(MODES + "sti", List.of("television")), deliveries(fixture.savedEvent()));
            assertNull(observation(fixture.savedEvent(), OTHER_TEXT));
            fixture.openCard(original.getBaseEntityId(), original.getVisitId(), SWAHILI);
            assertEquals(Status.COMPLETED, fixture.action().getActionStatus());

            verify(fixture.detailsRepository, never()).deleteVisitDetails(older.getVisitId());
            verify(fixture.detailsRepository, never()).deleteVisitDetails(otherClient.getVisitId());
            assertEquals(List.of("3"), observation(fixture.readEvent(older), BehavioralFixture.COUNT).getValues());
            assertEquals(List.of("29"), observation(fixture.readEvent(otherClient), BehavioralFixture.COUNT).getValues());
        }
    }

    @Test
    public void behavioralHistoryRejectsStaleOrCrossClientSaveBeforeReplacingRows() throws Exception {
        try (BehavioralFixture fixture = new BehavioralFixture()) {
            Visit selected = fixture.legacy("synthetic-client", "opened-visit", "12");
            Visit otherClient = fixture.legacy("synthetic-other-client", "other-client-visit", "29");
            fixture.openCard(selected.getBaseEntityId(), selected.getVisitId(), Locale.ENGLISH);
            fixture.submitAs(otherClient.getBaseEntityId(), false);
            Visit newer = fixture.legacy(selected.getBaseEntityId(), "newer-visit", "30");
            fixture.submit(false);
            fixture.openCard(selected.getBaseEntityId(), selected.getVisitId(), Locale.ENGLISH);
            assertEquals(Status.PENDING, fixture.action().getActionStatus());
            verify(fixture.visits, never()).addVisit(any());
            verify(fixture.detailsRepository, never()).deleteVisitDetails(anyString());
            verify(fixture.database, never()).setTransactionSuccessful();
            assertEquals(List.of("12"), observation(fixture.readEvent(selected), BehavioralFixture.COUNT).getValues());
            assertEquals(List.of("30"), observation(fixture.readEvent(newer), BehavioralFixture.COUNT).getValues());
        }
    }

    @Test
    @Config(shadows = BehavioralDatabase.CipherDatabase.class)
    public void deferredProcessorQueuesOnlyAgedValidEducationOnce() throws Exception {
        try (BehavioralDatabase store = new BehavioralDatabase();
             BehavioralFixture fixture = new BehavioralFixture(store, true)) {
            Visit delivery = fixture.submitNewEducation("synthetic-leaflets", "sti", "leaflets");
            Visit none = fixture.submitNewEducation("synthetic-none", "none", null);
            Visit young = fixture.submitNewEducation("synthetic-young", "none", null);
            Visit empty = fixture.persistEducation("synthetic-empty", List.of(), null);
            Visit mixed = fixture.persistEducation("synthetic-mixed", List.of("none", "sti"), null);
            Visit missingModes = fixture.persistEducation("synthetic-missing-delivery", List.of("sti"), null);
            Visit missingChannelText = fixture.persistEducation("synthetic-missing-other-channel", List.of("tb"), OTHER);
            Visit missingTopicText = fixture.persistEducation("synthetic-missing-other-topic", List.of(OTHER), "leaflets");
            List<Visit> incomplete = List.of(empty, mixed, missingModes, missingChannelText, missingTopicText);

            for (Visit eligible : List.of(delivery, none, young)) {
                assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBehavioralServiceStatus(eligible));
            }
            for (Visit invalid : incomplete) {
                assertEquals(KvpVisitsUtil.Pending, KvpVisitsUtil.getBehavioralServiceStatus(invalid));
                store.setAge(invalid, 3);
            }

            KvpVisitsUtil.processVisits();
            assertTrue(store.queuedEvents(BehavioralFixture.VISIT_TYPE).isEmpty());

            store.setAge(delivery, 3);
            store.setAge(none, 3);
            store.setAge(young, 1);
            KvpVisitsUtil.processVisits();
            KvpVisitsUtil.processVisits();

            List<Event> queued = store.queuedEvents(BehavioralFixture.VISIT_TYPE);
            assertEquals(2, queued.size());
            Map<String, Event> bySubmission = queued.stream()
                    .collect(Collectors.toMap(Event::getFormSubmissionId, event -> event));
            assertEquals(List.of("leaflets"), observation(bySubmission.get(delivery.getFormSubmissionId()), MODES + "sti").getValues());
            assertEquals(List.of("none"), observation(bySubmission.get(none.getFormSubmissionId()), TOPICS).getValues());
            for (Visit eligible : List.of(delivery, none)) {
                Visit processed = store.visits.getVisitByVisitId(eligible.getVisitId());
                assertTrue(processed.getProcessed());
                assertEquals("", processed.getPreProcessedJson());
                assertEquals(BaseRepository.TYPE_Unprocessed, store.queueStatus(eligible));
                Event queuedEvent = bySubmission.get(eligible.getFormSubmissionId());
                assertEquals(eligible.getBaseEntityId(), queuedEvent.getBaseEntityId());
                assertNull(observation(queuedEvent, BehavioralFixture.COUNT));
            }
            assertFalse(store.visits.getVisitByVisitId(young.getVisitId()).getProcessed());
            for (Visit invalid : incomplete) {
                assertFalse(store.visits.getVisitByVisitId(invalid.getVisitId()).getProcessed());
            }
        }
    }

    @Test
    @Config(shadows = BehavioralDatabase.CipherDatabase.class)
    public void defaultConsumerStillRequiresBothBehavioralObservations() throws Exception {
        try (BehavioralDatabase store = new BehavioralDatabase();
             BehavioralFixture fixture = new BehavioralFixture(store, false)) {
            Visit educationOnly = fixture.submitNewEducation("synthetic-default-education", "none", null);
            Visit both = fixture.legacy("synthetic-default-both", "default-legacy", "0");
            assertEquals(KvpVisitsUtil.Ongoing, KvpVisitsUtil.getBehavioralServiceStatus(educationOnly));
            assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBehavioralServiceStatus(both));
            store.setAge(educationOnly, 3);
            store.setAge(both, 3);
            KvpVisitsUtil.processVisits();
            assertFalse(store.visits.getVisitByVisitId(educationOnly.getVisitId()).getProcessed());
            assertTrue(store.visits.getVisitByVisitId(both.getVisitId()).getProcessed());
            assertEquals(List.of(both.getFormSubmissionId()), store.queuedEvents(BehavioralFixture.VISIT_TYPE)
                    .stream().map(Event::getFormSubmissionId).collect(Collectors.toList()));
        }
    }

    @Test
    @Config(shadows = BehavioralDatabase.CipherDatabase.class)
    public void replacingProcessedLegacyVisitKeepsItsCountAndQueuesItsOwnDeleteAndReplacement() throws Exception {
        try (BehavioralDatabase store = new BehavioralDatabase();
             BehavioralFixture fixture = new BehavioralFixture(store, true)) {
            Visit older = fixture.legacy("synthetic-legacy", "older-visit", "3", 8);
            Visit original = fixture.legacy("synthetic-legacy", "selected-visit", "12", 4);
            Visit otherClient = fixture.legacy("synthetic-other", "other-client-visit", "29", 4);
            String countObservation = fixture.countObservation(original);
            List<String> countRows = fixture.countRows(original);

            store.setAge(original, 3);
            KvpVisitsUtil.processVisits();
            assertTrue(store.visits.getVisitByVisitId(original.getVisitId()).getProcessed());
            assertEquals(1, store.queuedEvents(BehavioralFixture.VISIT_TYPE).size());

            fixture.openCard(original.getBaseEntityId(), original.getVisitId(), SWAHILI);
            assertEquals(Status.COMPLETED, fixture.action().getActionStatus());
            fixture.submit(true);
            fixture.assertRetained(original, countObservation, countRows);
            Visit untouched = fixture.savedVisit();
            assertNotEquals(original.getFormSubmissionId(), untouched.getFormSubmissionId());
            assertFalse(untouched.getProcessed());
            assertEquals(KvpVisitsUtil.Complete, KvpVisitsUtil.getBehavioralServiceStatus(untouched));
            store.setAge(untouched, 3);
            KvpVisitsUtil.processVisits();
            assertTrue(store.visits.getVisitByVisitId(untouched.getVisitId()).getProcessed());

            fixture.openCard(original.getBaseEntityId(), original.getVisitId(), Locale.ENGLISH);
            JSONObject changed = new JSONObject(fixture.action().getJsonPayload());
            answerChecklist(field(changed, TOPICS), List.of("sti"));
            answerChecklist(field(changed, MODES + "sti"), List.of("television"));
            fixture.saveForm(changed);
            fixture.submit(true);
            fixture.assertRetained(original, countObservation, countRows);
            Visit update = fixture.savedVisit();
            assertNotEquals(untouched.getFormSubmissionId(), update.getFormSubmissionId());
            KvpVisitsUtil.processVisits();
            assertFalse(store.visits.getVisitByVisitId(update.getVisitId()).getProcessed());
            store.setAge(update, 3);
            KvpVisitsUtil.processVisits();
            KvpVisitsUtil.processVisits();
            assertTrue(store.visits.getVisitByVisitId(update.getVisitId()).getProcessed());

            List<Event> deletions = store.queuedEvents(Constants.EVENT_TYPE.DELETE_EVENT);
            assertEquals(List.of(original.getFormSubmissionId(), untouched.getFormSubmissionId()), deletions.stream()
                    .map(event -> event.getDetails().get(Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID))
                    .collect(Collectors.toList()));
            assertEquals(original.getEventId(), deletions.get(0).getDetails().get(Constants.JSON_FORM_EXTRA.DELETE_EVENT_ID));
            assertTrue(deletions.stream().allMatch(event -> original.getBaseEntityId().equals(event.getBaseEntityId())));
            List<Event> queued = store.queuedEvents(BehavioralFixture.VISIT_TYPE);
            assertEquals(List.of(original.getFormSubmissionId(), untouched.getFormSubmissionId(), update.getFormSubmissionId()),
                    queued.stream().map(Event::getFormSubmissionId).collect(Collectors.toList()));
            assertTrue(queued.stream().allMatch(event -> countObservation.equals(BehavioralFixture.GSON.toJson(
                    observation(event, BehavioralFixture.COUNT)))));
            assertEquals(List.of("television"), observation(queued.get(2), MODES + "sti").getValues());
            assertEquals(List.of("3"), observation(fixture.readEvent(older), BehavioralFixture.COUNT).getValues());
            assertEquals(List.of("29"), observation(fixture.readEvent(otherClient), BehavioralFixture.COUNT).getValues());
        }
    }

    private static void assertStatus(BaseKvpVisitAction action, JSONObject form, Status status) {
        action.setJsonPayload(form.toString());
        assertEquals(status, action.getActionStatus());
    }

    private static Map<String, List<Object>> deliveries(Event event) {
        return event.getObs().stream()
                .filter(obs -> obs.getFormSubmissionField().startsWith(MODES) || obs.getFormSubmissionField().startsWith(CHANNEL))
                .collect(Collectors.toMap(obs -> obs.getFormSubmissionField(), obs -> new ArrayList<>(obs.getValues())));
    }

    private static final class BehavioralFixture implements AutoCloseable {
        private static final String VISIT_TYPE = Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT;
        private static final String COUNT = "iec_sbcc_materials";
        private static final Gson GSON = new Gson();

        private final BehavioralDatabase store;
        private final VisitRepository visits;
        private final VisitDetailsRepository detailsRepository;
        private final SupportSQLiteDatabase database;
        private final Map<String, Visit> latestByClient = new HashMap<>();
        private final Map<String, List<VisitDetail>> detailsByVisit = new HashMap<>();
        private final Map<String, Visit> visitsById = new HashMap<>();
        private final KvpLibrary previousLibrary = ReflectionHelpers.getStaticField(KvpLibrary.class, "instance");
        private final CoreLibrary previousCore = ReflectionHelpers.getStaticField(CoreLibrary.class, "instance");
        private final FormUtils previousForms = ReflectionHelpers.getStaticField(FormUtils.class, "instance");
        private KvpBehavioralServiceActivity card;
        private KvpBehavioralServiceInteractor interactor;
        private TextView submitButton;
        private String clientId;
        private boolean editing;
        private Visit lastSaved;

        private BehavioralFixture() {
            this(null, true);
        }

        private BehavioralFixture(BehavioralDatabase store, boolean kituoni) {
            this.store = store;
            visits = store == null ? mock(VisitRepository.class) : store.visits;
            detailsRepository = store == null ? mock(VisitDetailsRepository.class) : store.details;
            database = store == null ? mock(SupportSQLiteDatabase.class) : store.database;
            Context context = mock(Context.class);
            when(context.applicationContext()).thenReturn(ApplicationProvider.getApplicationContext());
            when(context.allSharedPreferences()).thenReturn(mock(AllSharedPreferences.class));
            ReflectionHelpers.setStaticField(KvpLibrary.class, "instance", null);
            ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", null);
            CoreLibrary.init(context);
            if (kituoni) {
                KvpLibrary.init(context, null, 67, 32, HfKvpDao::isPregnantForBiomedicalVisit,
                        HfKvpDao::isPregnantForBiomedicalVisit, KvpHealthEducationHelper::isSavedEducationComplete);
            } else {
                KvpLibrary.init(context, null, 67, 32);
            }
            KvpLibrary library = KvpLibrary.getInstance();
            ReflectionHelpers.setField(library, "visitRepository", visits);
            ReflectionHelpers.setField(library, "visitDetailsRepository", detailsRepository);
            ReflectionHelpers.setField(library, "syncHelper", store == null ? mock(ECSyncHelper.class) : store.sync);
            library.setClientProcessorForJava(mock(ClientProcessorForJava.class));

            if (store == null) {
                stubRepositories();
            }
        }

        private void stubRepositories() {
            when(visits.getLatestVisit(anyString(), eq(VISIT_TYPE)))
                    .thenAnswer(call -> latestByClient.get(call.getArgument(0)));
            when(visits.getVisitByVisitId(anyString())).thenAnswer(call -> visitsById.get(call.getArgument(0)));
            when(detailsRepository.getVisits(anyString()))
                    .thenAnswer(call -> new ArrayList<>(detailsByVisit.getOrDefault(call.getArgument(0), List.of())));
            doAnswer(call -> {
                lastSaved = call.getArgument(0);
                visitsById.put(lastSaved.getVisitId(), lastSaved);
                latestByClient.put(lastSaved.getBaseEntityId(), lastSaved);
                return null;
            }).when(visits).addVisit(any(Visit.class));
            doAnswer(call -> {
                detailsByVisit.remove(call.getArgument(0));
                return null;
            }).when(detailsRepository).deleteVisitDetails(anyString());
            doAnswer(call -> {
                VisitDetail detail = call.getArgument(0);
                detailsByVisit.computeIfAbsent(detail.getVisitId(), key -> new ArrayList<>()).add(detail);
                return null;
            }).when(detailsRepository).addVisitDetails(any(VisitDetail.class));
        }

        private void openCard(String client, String visitId, Locale locale) throws Exception {
            clientId = client;
            editing = visitId != null;
            Application application = ApplicationProvider.getApplicationContext();
            Configuration configuration = new Configuration(application.getResources().getConfiguration());
            configuration.setLocale(locale);
            android.content.Context context = application.createConfigurationContext(configuration);
            String label = context.getString(R.string.kvp_health_education);
            FormUtils forms = mock(FormUtils.class);
            when(forms.getFormJson(Constants.KVP_BEHAVIORAL_SERVICE_FORMS.KVP_HEALTH_EDUCATION)).thenReturn(form(locale));
            ReflectionHelpers.setStaticField(FormUtils.class, "instance", forms);
            interactor = new KvpBehavioralServiceInteractor(visitId) {
                {
                    appExecutors = new AppExecutors(Runnable::run, Runnable::run, Runnable::run);
                }

                @Override
                protected SupportSQLiteDatabase getWritableDatabase() {
                    return database;
                }

            };
            MemberObject member = mock(MemberObject.class);
            when(member.getBaseEntityId()).thenReturn(client);
            card = mock(KvpBehavioralServiceActivity.class);
            submitButton = new TextView(context);
            ReflectionHelpers.setField(card, "tvSubmit", submitButton);
            ReflectionHelpers.setField(card, "mAdapter", new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                @Override
                public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    throw new AssertionError("The card fixture does not inflate rows");
                }

                @Override
                public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                }

                @Override
                public int getItemCount() {
                    return 0;
                }
            });
            ReflectionHelpers.setField(card, "actionList", new LinkedHashMap<>());
            ReflectionHelpers.setField(card, "current_action", label);
            when(card.getResources()).thenReturn(context.getResources());
            when(card.getContext()).thenReturn(context);
            when(card.getEditMode()).thenReturn(editing);
            doCallRealMethod().when(card).getKvpVisitActions();
            doCallRealMethod().when(card).initializeActions(any());
            doCallRealMethod().when(card).redrawVisitUI();
            doCallRealMethod().when(card).onDialogOptionUpdated(anyString());
            BaseKvpVisitContract.InteractorCallBack callback = mock(BaseKvpVisitContract.InteractorCallBack.class);
            doAnswer(call -> {
                card.initializeActions(call.getArgument(0));
                return null;
            }).when(callback).preloadActions(any());

            interactor.calculateActions(card, member, callback);
            assertEquals(List.of(label), new ArrayList<>(card.getKvpVisitActions().keySet()));
            assertEquals(Constants.KVP_BEHAVIORAL_SERVICE_FORMS.KVP_HEALTH_EDUCATION, action().getFormName());
        }

        private BaseKvpVisitAction action() {
            return card.getKvpVisitActions().values().iterator().next();
        }

        private void saveForm(JSONObject payload) {
            card.onDialogOptionUpdated(payload.toString());
            assertTrue(submitButton.hasOnClickListeners());
        }

        private void submit(boolean success) {
            submitAs(clientId, success);
        }

        private void submitAs(String client, boolean success) {
            BaseKvpVisitContract.InteractorCallBack callback = mock(BaseKvpVisitContract.InteractorCallBack.class);
            interactor.submitVisit(editing, client, card.getKvpVisitActions(), callback);
            verify(callback).onSubmitted(success);
        }

        private Visit legacy(String client, String id, String count) throws Exception {
            return legacy(client, id, count, 0);
        }

        private Visit legacy(String client, String id, String count, int daysAgo) throws Exception {
            JSONObject savedForm = form(Locale.ENGLISH);
            answerChecklist(field(savedForm, TOPICS), List.of("tb", OTHER));
            field(savedForm, OTHER_TEXT).put("value", "Original synthetic topic");
            Event legacy = event(savedForm.toString());
            legacy.setBaseEntityId(client);
            legacy.setFormSubmissionId(id + "-original");
            legacy.setEventId("synthetic-event-" + id);
            Calendar date = Calendar.getInstance();
            date.add(Calendar.DAY_OF_YEAR, -daysAgo);
            legacy.setEventDate(date.getTime());
            legacy.addObs(new Obs("concept", "text", COUNT, "", List.of(count), List.of(count), null, COUNT));
            Visit visit = NCUtil.eventToVisit(legacy, id);
            visit.setPreProcessedJson(GSON.toJson(legacy));
            if (store != null) {
                visits.addVisit(visit);
                visit.getVisitDetails().values().forEach(rows -> rows.forEach(detailsRepository::addVisitDetails));
                return visit;
            }
            visitsById.put(id, visit);
            latestByClient.put(client, visit);
            detailsByVisit.put(id, visit.getVisitDetails().values().stream().flatMap(List::stream).collect(Collectors.toList()));
            return visit;
        }

        private Visit submitNewEducation(String client, String topic, String mode) throws Exception {
            openCard(client, null, Locale.ENGLISH);
            JSONObject payload = new JSONObject(action().getJsonPayload());
            answerChecklist(field(payload, TOPICS), List.of(topic));
            if (mode != null) {
                answerChecklist(field(payload, MODES + topic), List.of(mode));
            }
            saveForm(payload);
            submit(true);
            return savedVisit();
        }

        private Visit persistEducation(String client, List<String> topics, String mode) throws Exception {
            JSONObject payload = form(Locale.ENGLISH);
            answerChecklist(field(payload, TOPICS), topics);
            if (mode != null) {
                answerChecklist(field(payload, MODES + topics.get(0)), List.of(mode));
            }
            Event saved = event(payload.toString());
            saved.setBaseEntityId(client);
            saved.setFormSubmissionId(client + "-submission");
            Visit visit = NCUtil.eventToVisit(saved, client + "-visit");
            visit.setPreProcessedJson(GSON.toJson(saved));
            visits.addVisit(visit);
            return visit;
        }

        private List<VisitDetail> rows(Visit visit) {
            return detailsRepository.getVisits(visit.getVisitId());
        }

        private Event savedEvent() {
            return GSON.fromJson(savedVisit().getPreProcessedJson(), Event.class);
        }

        private Visit savedVisit() {
            return store == null ? lastSaved : visits.getLatestVisit(clientId, VISIT_TYPE);
        }

        private Event readEvent(Visit visit) {
            return GSON.fromJson(visits.getVisitByVisitId(visit.getVisitId()).getJson(), Event.class);
        }

        private String countObservation(Visit visit) {
            return GSON.toJson(observation(readEvent(visit), COUNT));
        }

        private List<String> countRows(Visit visit) {
            return rows(visit).stream().filter(row -> COUNT.equals(row.getVisitKey()))
                    .map(GSON::toJson).collect(Collectors.toList());
        }

        private void assertRetained(Visit original, String count, List<String> rows) {
            assertEquals(original.getVisitId(), savedVisit().getVisitId());
            assertEquals(original.getBaseEntityId(), savedVisit().getBaseEntityId());
            assertEquals(original.getDate(), savedVisit().getDate());
            assertEquals(count, countObservation(savedVisit()));
            assertEquals(count, GSON.toJson(observation(savedEvent(), COUNT)));
            assertEquals(rows, countRows(savedVisit()));
        }

        @Override
        public void close() {
            ReflectionHelpers.setStaticField(KvpLibrary.class, "instance", previousLibrary);
            ReflectionHelpers.setStaticField(CoreLibrary.class, "instance", previousCore);
            ReflectionHelpers.setStaticField(FormUtils.class, "instance", previousForms);
        }
    }

}
