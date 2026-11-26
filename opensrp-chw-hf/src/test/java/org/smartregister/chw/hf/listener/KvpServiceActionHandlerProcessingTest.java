package org.smartregister.chw.hf.listener;

import org.junit.Test;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome;

import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KvpServiceActionHandlerProcessingTest {
    private static final String CLIENT = "synthetic-marp-client";
    private static final String OTHER = Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT;
    private static final String OTHER_CARD = "other-card";
    private static final String SUBMISSION = "other-submission";
    private static final String COMPLETE_OBSERVATIONS =
            "[{\"fieldCode\":\"other_services_referrals_provided\",\"values\":[\"yes\"]}]";

    private final Calendar today = serviceDay(26);
    private final VisitRepository repository = mock(VisitRepository.class);
    private final KvpServiceActionHandler handler = mock(KvpServiceActionHandler.class);

    @Test
    public void completedNonFinalAndFinalCardsCanBeProcessedWithoutReopeningAForm() {
        Visit other = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        readCard(other);
        when(repository.getVisits(CLIENT, OTHER)).thenReturn(Collections.singletonList(other));

        assertFalse(MarpVisitStatus.read(repository, CLIENT, today).isCompleteToday());
        assertSame(other, MarpVisitStatus.findProcessableCard(repository, CLIENT, OTHER, today));
        for (String type : new String[]{Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT,
                Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT,
                Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT}) {
            Visit processed = otherCard(CLIENT, type, type + "-submission", true);
            processed.setVisitType(type);
            when(repository.getVisits(CLIENT, type)).thenReturn(Collections.singletonList(processed));
        }
        assertTrue(MarpVisitStatus.read(repository, CLIENT, today).isCompleteToday());
        assertFalse(MarpVisitStatus.findProcessableCardsToday(repository, CLIENT, today).isEmpty());
        other.setProcessed(true);
        assertTrue(MarpVisitStatus.read(repository, CLIENT, today).isCompleteToday());
        assertTrue(MarpVisitStatus.findProcessableCardsToday(repository, CLIENT, today).isEmpty());
    }

    @Test
    public void failedCardCanRetryUntilTheRefreshedRowIsProcessed() throws Exception {
        useRealSelection();
        Visit displayed = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        Visit stored = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        readCard(stored);
        doThrow(new IllegalStateException("synthetic processing failure")).doNothing()
                .when(handler).processCard(stored);

        assertThrows(IllegalStateException.class, () -> confirm(displayed, today));
        assertFalse(MarpVisitStatus.findProcessableCardsToday(repository, CLIENT, today).isEmpty());
        confirm(displayed, today);
        readCard(otherCard(CLIENT, OTHER_CARD, SUBMISSION, true));
        confirm(displayed, today);

        verify(handler, times(2)).processCard(stored);
        verify(handler, times(2)).processCard(any());
    }

    @Test
    public void exactPartialEnqueueReachesSharedClaimOnce() throws Exception {
        useRealSelection();
        Visit displayed = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        readCard(displayed);
        when(handler.localOutcome(displayed)).thenReturn(Outcome.ENQUEUE_INCOMPLETE);

        assertTrue(handler.canOfferProcessVisit(displayed));
        confirm(displayed, today);
        when(handler.localOutcome(displayed)).thenReturn(Outcome.QUEUED);
        confirm(displayed, today);

        verify(handler, times(1)).processCard(displayed);
    }

    @Test
    public void ambiguousLegacyOrUnverifiedReadbackDoesNotRetryTheCard() throws Exception {
        useRealSelection();
        Visit displayed = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        readCard(displayed);
        when(handler.localOutcome(displayed)).thenReturn(Outcome.UNVERIFIED);

        confirm(displayed, today);
        verify(handler, never()).processCard(any());
    }

    @Test
    public void staleSelectionsRejectReplacementDifferentClientAndDayRollover() throws Exception {
        useRealSelection();
        Visit displayed = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        readCard(otherCard(CLIENT, "replacement-id", SUBMISSION, false));
        confirm(displayed, today);
        readCard(otherCard(CLIENT, OTHER_CARD, "replacement-submission", false));
        confirm(displayed, today);
        readCard(otherCard("another-synthetic-client", OTHER_CARD, SUBMISSION, false));
        confirm(displayed, today);
        readCard(displayed);
        confirm(displayed, serviceDay(27));

        verify(handler, never()).processCard(any());
        verify(repository, times(3)).getLatestVisit(CLIENT, OTHER);
    }

    @Test
    public void draftsIncompleteCardsAndPriorDaysDoNotOfferSameDayProcessing() {
        Visit draft = otherCard(CLIENT, OTHER_CARD, null, false);
        Visit incomplete = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        incomplete.setJson(payload(incomplete, "[]"));
        Visit prior = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        prior.setDate(serviceDay(25).getTime());

        readCard(draft);
        assertNull(MarpVisitStatus.findProcessableCard(repository, CLIENT, OTHER, today));
        readCard(incomplete);
        assertNull(MarpVisitStatus.findProcessableCard(repository, CLIENT, OTHER, today));
        readCard(prior);
        assertNull(MarpVisitStatus.findProcessableCard(repository, CLIENT, OTHER, today));
        assertSame(prior, MarpVisitStatus.findProcessableCard(repository, CLIENT, OTHER, null));
    }

    @Test
    public void missingUniqueRowOrReplacedStagedSubmissionCannotDispatch() throws Exception {
        useRealSelection();
        Visit displayed = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        readCard(displayed);
        // The pinned repository returns null for an absent or duplicated visit ID.
        when(repository.getVisitByVisitId(OTHER_CARD)).thenReturn(null);
        confirm(displayed, today);

        Visit changed = otherCard(CLIENT, OTHER_CARD, "replacement-submission", false);
        when(repository.getVisitByVisitId(OTHER_CARD)).thenReturn(changed);
        confirm(displayed, today);

        verify(handler, never()).processCard(any());
    }

    @Test
    public void mixedStagedOrRetainedEventPayloadCannotDispatch() throws Exception {
        useRealSelection();
        Visit displayed = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        Visit stored = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        readCard(stored);
        stored.setPreProcessedJson(payload(otherCard(CLIENT, OTHER_CARD, "another-submission", false),
                COMPLETE_OBSERVATIONS));
        confirm(displayed, today);

        stored.setPreProcessedJson(displayed.getPreProcessedJson());
        stored.setJson(payload(otherCard("another-client", OTHER_CARD, SUBMISSION, false), COMPLETE_OBSERVATIONS));
        confirm(displayed, today);

        verify(handler, never()).processCard(any());
    }

    @Test
    public void advancedVisitMarkerWithNoEventDoesNotReplayClearedStaging() throws Exception {
        useRealSelection();
        Visit displayed = otherCard(CLIENT, OTHER_CARD, SUBMISSION, false);
        Visit marked = otherCard(CLIENT, OTHER_CARD, SUBMISSION, true);
        marked.setPreProcessedJson("");
        readCard(marked);

        confirm(displayed, today);

        verify(handler, never()).localOutcome(any());
        verify(handler, never()).processCard(any());
    }

    @Test
    public void onlyMissingOrIncompleteEnqueueCanReachTheSharedWriter() {
        for (Outcome outcome : Outcome.values()) {
            if (outcome == Outcome.NOT_ENQUEUED || outcome == Outcome.ENQUEUE_INCOMPLETE) {
                assertTrue(KvpServiceActionHandler.canEnqueue(outcome));
            } else {
                assertFalse(KvpServiceActionHandler.canEnqueue(outcome));
            }
        }
    }

    private void readCard(Visit card) {
        when(repository.getLatestVisit(CLIENT, OTHER)).thenReturn(card);
        when(repository.getVisitByVisitId(card.getVisitId())).thenReturn(card);
    }

    private void useRealSelection() throws Exception {
        when(handler.localOutcome(any())).thenReturn(Outcome.NOT_ENQUEUED);
        when(handler.canOfferProcessVisit(any())).thenCallRealMethod();
        doCallRealMethod().when(handler).processSelectedCard(any(), anyString(), any(), any(), any(), eq(true));
    }

    private void confirm(Visit shown, Calendar now) throws Exception {
        handler.processSelectedCard(repository, CLIENT, shown, today, now, true);
    }

    private Visit otherCard(String client, String id, String submission, boolean processed) {
        Visit visit = new Visit();
        visit.setBaseEntityId(client);
        visit.setVisitId(id);
        visit.setVisitType(OTHER);
        visit.setFormSubmissionId(submission);
        visit.setDate(today.getTime());
        visit.setJson(payload(visit, COMPLETE_OBSERVATIONS));
        visit.setPreProcessedJson(visit.getJson());
        visit.setProcessed(processed);
        return visit;
    }

    private static String payload(Visit card, String observations) {
        return "{\"baseEntityId\":\"" + card.getBaseEntityId() + "\",\"eventType\":\"" + card.getVisitType()
                + "\",\"formSubmissionId\":\"" + card.getFormSubmissionId() + "\",\"obs\":" + observations + "}";
    }

    private static Calendar serviceDay(int date) {
        Calendar day = Calendar.getInstance(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
        day.clear();
        day.set(2026, Calendar.NOVEMBER, date, 10, 0);
        return day;
    }
}
