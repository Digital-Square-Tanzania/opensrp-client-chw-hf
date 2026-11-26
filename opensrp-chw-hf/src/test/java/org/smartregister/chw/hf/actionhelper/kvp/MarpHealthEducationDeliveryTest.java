package org.smartregister.chw.hf.actionhelper.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.OTHER_TEXT;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.TOPICS;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.TOPIC_ORDER;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.answerChecklist;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.event;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.field;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.form;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.observation;
import static org.smartregister.chw.hf.actionhelper.kvp.MarpHealthEducationTest.open;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.Context;
import org.smartregister.chw.hf.interactor.KvpBehavioralServiceInteractor;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.contract.BaseKvpVisitContract;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction.Status;
import org.smartregister.chw.kvp.repository.VisitDetailsRepository;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.AppExecutors;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.chw.kvp.util.NCUtil;
import org.smartregister.chw.kvp.util.VisitUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.repository.AllSharedPreferences;
import org.smartregister.sync.helper.ECSyncHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MarpHealthEducationDeliveryTest {
    static final String MODES = "health_education_delivery_modes_";
    static final String CHANNEL = "health_education_delivery_other_";
    private static final String OTHER = "other";
    private static final Locale SWAHILI = new Locale("sw");

    @Test
    public void allTopicsHaveDeliveryQuestionsInRegisterOrderInBothLanguages() throws Exception {
        assertQuestions(Locale.ENGLISH, "Mode of Information Delivery: ", "other topic",
                List.of("Leaflets", "Health-care provider", "Television", "Other (specify)"),
                "Mention the other channel");
        assertQuestions(SWAHILI, "Aina ya chombo cha Uwasilisho: ", "elimu nyingine",
                List.of("Vipeperushi", "Mtoa huduma za Afya", "Televisheni", "Nyinginezo (taja)"),
                "Taja njia nyingine");
    }

    @Test
    public void saveRequiresModesAndNonblankOtherTextForEachSelectedTopic() throws Exception {
        BaseKvpVisitAction action = open(Map.of(), Locale.ENGLISH);
        JSONObject draft = form(Locale.ENGLISH);
        answerChecklist(field(draft, TOPICS), List.of("sti", "tb"));
        answerChecklist(field(draft, MODES + "sti"), List.of("leaflets", OTHER));
        answerChecklist(field(draft, MODES + "tb"), List.of("television", OTHER));
        field(draft, CHANNEL + "sti").put("value", "STI channel");
        field(draft, CHANNEL + "tb").put("value", "TB channel");
        assertStatus(action, draft, Status.COMPLETED);

        for (String topic : List.of("sti", "tb")) {
            JSONObject missingModes = new JSONObject(draft.toString());
            answerChecklist(field(missingModes, MODES + topic), List.of());
            assertStatus(action, missingModes, Status.PENDING);

            JSONObject blankChannel = new JSONObject(draft.toString());
            field(blankChannel, CHANNEL + topic).put("value", "   ");
            assertStatus(action, blankChannel, Status.PENDING);
        }
    }

    @Test
    public void eventAndVisitDetailsRestoreIndependentModesAndChannelTexts() throws Exception {
        BaseKvpVisitAction action = open(Map.of(), Locale.ENGLISH);
        JSONObject savedForm = form(Locale.ENGLISH);
        answerChecklist(field(savedForm, TOPICS), List.of("sti", "tb", OTHER));
        answerChecklist(field(savedForm, MODES + "sti"), List.of("leaflets", "health_care_provider", OTHER));
        answerChecklist(field(savedForm, MODES + "tb"), List.of("television", OTHER));
        answerChecklist(field(savedForm, MODES + OTHER), List.of("health_care_provider", OTHER));
        field(savedForm, OTHER_TEXT).put("value", "Synthetic topic");
        field(savedForm, CHANNEL + "sti").put("value", "Group session, STI");
        field(savedForm, CHANNEL + "tb").put("value", "Peer dialogue for TB");
        field(savedForm, CHANNEL + OTHER).put("value", "Community session");
        assertStatus(action, savedForm, Status.COMPLETED);

        Event saved = event(action.getJsonPayload());
        Map<String, List<Object>> expected = Map.of(
                MODES + "sti", List.of("leaflets", "health_care_provider", OTHER),
                MODES + "tb", List.of("television", OTHER),
                MODES + OTHER, List.of("health_care_provider", OTHER),
                CHANNEL + "sti", List.of("Group session, STI"),
                CHANNEL + "tb", List.of("Peer dialogue for TB"),
                CHANNEL + OTHER, List.of("Community session"));
        assertEquals(expected, deliveries(saved));
        assertEquals(List.of("sti", "tb", OTHER), observation(saved, TOPICS).getValues());
        assertEquals(List.of("Synthetic topic"), observation(saved, OTHER_TEXT).getValues());
        for (String key : expected.keySet()) {
            assertEquals(key, observation(saved, key).getFieldCode());
        }

        Visit visit = NCUtil.eventToVisit(saved, "synthetic-visit-16");
        for (String key : expected.keySet()) {
            assertEquals(visit.getVisitId(), visit.getVisitDetails().get(key).get(0).getVisitId());
        }
        BaseKvpVisitAction reopened = reopen(visit);
        assertEquals(Status.COMPLETED, reopened.getActionStatus());
        Event resaved = event(reopened.getJsonPayload());
        assertEquals(expected, deliveries(resaved));
        assertEquals(List.of("Synthetic topic"), observation(resaved, OTHER_TEXT).getValues());
    }

    @Test
    public void deselectionClearsOnlyItsOwnObservationsAndNoneClearsAll() throws Exception {
        BaseKvpVisitAction action = open(Map.of(), Locale.ENGLISH);
        JSONObject form = form(Locale.ENGLISH);
        answerChecklist(field(form, TOPICS), List.of("sti", "tb"));
        answerChecklist(field(form, MODES + "sti"), List.of("leaflets", OTHER));
        answerChecklist(field(form, MODES + "tb"), List.of("television", OTHER));
        field(form, CHANNEL + "sti").put("value", "STI channel");
        field(form, CHANNEL + "tb").put("value", "TB channel");

        answerChecklist(field(form, MODES + "tb"), List.of("television"));
        assertStatus(action, form, Status.COMPLETED);
        assertEquals(Map.of(MODES + "sti", List.of("leaflets", OTHER), CHANNEL + "sti", List.of("STI channel"),
                MODES + "tb", List.of("television")), deliveries(event(action.getJsonPayload())));

        answerChecklist(field(form, TOPICS), List.of("sti"));
        answerChecklist(field(form, MODES + "tb"), List.of("television", OTHER));
        assertStatus(action, form, Status.COMPLETED);
        assertEquals(Map.of(MODES + "sti", List.of("leaflets", OTHER), CHANNEL + "sti", List.of("STI channel")),
                deliveries(event(action.getJsonPayload())));

        answerChecklist(field(form, TOPICS), List.of("none"));
        assertStatus(action, form, Status.COMPLETED);
        Event none = event(action.getJsonPayload());
        assertEquals(List.of("none"), observation(none, TOPICS).getValues());
        assertTrue(deliveries(none).isEmpty());
        assertTrue(deliveries(event(reopen(NCUtil.eventToVisit(none)).getJsonPayload())).isEmpty());
    }

    @Test
    public void untouchedLegacyVisitKeepsTopicsAndTextAndEditingRequiresDelivery() throws Exception {
        JSONObject original = form(Locale.ENGLISH);
        answerChecklist(field(original, TOPICS), List.of("tb", OTHER));
        field(original, OTHER_TEXT).put("value", "Synthetic earlier topic, with detail");
        Event legacy = event(original.toString());

        BaseKvpVisitAction restored = reopen(NCUtil.eventToVisit(legacy, "legacy-visit"));
        assertEquals(Status.COMPLETED, restored.getActionStatus());
        Event untouched = event(restored.getJsonPayload());
        assertEquals(observation(legacy, TOPICS).getValues(), observation(untouched, TOPICS).getValues());
        assertEquals(observation(legacy, OTHER_TEXT).getValues(), observation(untouched, OTHER_TEXT).getValues());
        assertTrue(deliveries(untouched).isEmpty());

        JSONObject edit = new JSONObject(restored.getJsonPayload());
        assertStatus(restored, edit, Status.PENDING);
        answerChecklist(field(edit, MODES + "tb"), List.of("television"));
        assertStatus(restored, edit, Status.PENDING);
        answerChecklist(field(edit, MODES + OTHER), List.of("leaflets"));
        assertStatus(restored, edit, Status.COMPLETED);
        Event updated = event(restored.getJsonPayload());
        assertEquals(Map.of(MODES + "tb", List.of("television"), MODES + OTHER, List.of("leaflets")),
                deliveries(updated));
        assertEquals(observation(legacy, OTHER_TEXT).getValues(), observation(updated, OTHER_TEXT).getValues());
        assertTrue(deliveries(event(reopen(NCUtil.eventToVisit(legacy, "legacy-visit")).getJsonPayload())).isEmpty());
    }

    @Test
    public void behavioralSubmitAndHistoryEditReplaceDeliveryForTheSameVisit() throws Exception {
        Context context = mock(Context.class);
        when(context.allSharedPreferences()).thenReturn(mock(AllSharedPreferences.class));
        VisitRepository visits = mock(VisitRepository.class);
        VisitDetailsRepository storedDetails = mock(VisitDetailsRepository.class);
        KvpLibrary previous = ReflectionHelpers.getStaticField(KvpLibrary.class, "instance");
        ReflectionHelpers.setStaticField(KvpLibrary.class, "instance", null);
        KvpLibrary.init(context, null, 67, 32);
        KvpLibrary library = KvpLibrary.getInstance();
        ReflectionHelpers.setField(library, "visitRepository", visits);
        ReflectionHelpers.setField(library, "visitDetailsRepository", storedDetails);
        ReflectionHelpers.setField(library, "syncHelper", mock(ECSyncHelper.class));

        try {
            KvpBehavioralServiceInteractor interactor = new KvpBehavioralServiceInteractor(
                    Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT) {
                {
                    appExecutors = new AppExecutors(Runnable::run, Runnable::run, Runnable::run);
                }
            };
            BaseKvpVisitAction action = open(Map.of(), Locale.ENGLISH);
            JSONObject filled = form(Locale.ENGLISH);
            answerChecklist(field(filled, TOPICS), List.of("sti", "tb"));
            answerChecklist(field(filled, MODES + "sti"), List.of("leaflets", "health_care_provider"));
            answerChecklist(field(filled, MODES + "tb"), List.of(OTHER));
            field(filled, CHANNEL + "tb").put("value", "Synthetic TB session");
            assertStatus(action, filled, Status.COMPLETED);

            String client = "synthetic-submit";
            BaseKvpVisitContract.InteractorCallBack callback = mock(BaseKvpVisitContract.InteractorCallBack.class);
            interactor.submitVisit(false, client, Map.of(action.getTitle(), action), callback);
            verify(callback).onSubmitted(true);
            ArgumentCaptor<Visit> visitCapture = ArgumentCaptor.forClass(Visit.class);
            verify(visits).addVisit(visitCapture.capture());
            Visit saved = visitCapture.getValue();
            assertEquals(Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT, saved.getVisitType());
            Map<String, List<Object>> expected = Map.of(MODES + "sti", List.of("leaflets", "health_care_provider"),
                    MODES + "tb", List.of(OTHER), CHANNEL + "tb", List.of("Synthetic TB session"));
            assertEquals(expected, deliveries(new Gson().fromJson(saved.getPreProcessedJson(), Event.class)));

            ArgumentCaptor<VisitDetail> detailCapture = ArgumentCaptor.forClass(VisitDetail.class);
            verify(storedDetails, atLeastOnce()).addVisitDetails(detailCapture.capture());
            BaseKvpVisitAction edited = open(VisitUtils.getVisitGroups(detailCapture.getAllValues()), SWAHILI);
            assertEquals(Status.COMPLETED, edited.getActionStatus());
            Event restored = KvpJsonFormUtils.processVisitJsonForm(
                    context.allSharedPreferences(), client, saved.getVisitType(),
                    Map.of(edited.getTitle(), edited.getJsonPayload()), Constants.TABLES.KVP_FOLLOW_UP);
            assertEquals(expected, deliveries(restored));

            JSONObject none = new JSONObject(edited.getJsonPayload());
            answerChecklist(field(none, TOPICS), List.of("none"));
            assertStatus(edited, none, Status.COMPLETED);
            when(visits.getLatestVisit(client, saved.getVisitType())).thenReturn(saved);
            when(visits.getVisitByVisitId(saved.getVisitId())).thenReturn(saved);
            BaseKvpVisitContract.InteractorCallBack editCallback = mock(BaseKvpVisitContract.InteractorCallBack.class);
            interactor.submitVisit(true, client, Map.of(edited.getTitle(), edited), editCallback);
            verify(editCallback).onSubmitted(true);
            verify(storedDetails).deleteVisitDetails(saved.getVisitId());
            ArgumentCaptor<Visit> editedVisits = ArgumentCaptor.forClass(Visit.class);
            verify(visits, times(2)).addVisit(editedVisits.capture());
            Visit updated = editedVisits.getAllValues().get(1);
            assertEquals(saved.getVisitId(), updated.getVisitId());
            assertTrue(deliveries(new Gson().fromJson(updated.getPreProcessedJson(), Event.class)).isEmpty());
        } finally {
            ReflectionHelpers.setStaticField(KvpLibrary.class, "instance", previous);
        }
    }

    private static void assertQuestions(Locale locale, String prefix, String otherTopic, List<String> modeLabels,
                                        String channelHint) throws Exception {
        JSONObject form = form(locale);
        JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
        JSONArray topics = field(form, TOPICS).getJSONArray("options");
        assertEquals(24, fields.length());
        for (int i = 0; i < TOPIC_ORDER.size() - 1; i++) {
            String topic = TOPIC_ORDER.get(i);
            JSONObject modes = fields.getJSONObject(2 + 2 * i);
            JSONObject channel = fields.getJSONObject(3 + 2 * i);
            assertEquals(MODES + topic, modes.getString("key"));
            assertEquals(CHANNEL + topic, channel.getString("key"));
            assertEquals(prefix + (OTHER.equals(topic) ? otherTopic : topics.getJSONObject(i).getString("text")),
                    modes.getString("label"));
            assertTrue(modes.getJSONObject("v_required").getBoolean("value"));
            assertTrue(modes.getBoolean("combine_checkbox_option_values"));
            assertEquals(channelHint, channel.getString("hint"));
            assertTrue(channel.getJSONObject("v_required").getBoolean("value"));
            JSONArray options = modes.getJSONArray("options");
            assertEquals(modeLabels.size(), options.length());
            for (int j = 0; j < options.length(); j++) {
                assertEquals(modeLabels.get(j), options.getJSONObject(j).getString("text"));
                assertEquals(List.of("leaflets", "health_care_provider", "television", OTHER).get(j),
                        options.getJSONObject(j).getString("key"));
            }
        }
        assertFalse(field(form, MODES + "none").has("key"));
        assertFalse(field(form, OTHER_TEXT).getString("hint").equals(channelHint));
    }

    private static BaseKvpVisitAction reopen(Visit visit) throws Exception {
        return open(VisitUtils.getVisitGroups(visit.getVisitDetails().values().stream()
                .flatMap(List::stream).collect(Collectors.toList())), SWAHILI);
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
}
