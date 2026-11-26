package org.smartregister.chw.hf.utils;

import org.json.JSONException;
import org.junit.Test;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.CONSENT_DECLINED;
import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.HIDDEN;
import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.PENDING;
import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.REGISTERED;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT;

public class MarpPrepRegistrationReadbackTest {
    private static final String CLIENT = "synthetic-client";
    private static final Calendar TODAY = day(26, 9, 0);

    @Test
    public void noVisitDraftOnlyAndUnclosedVisitAreIneligible() throws Exception {
        List<Visit> cards = new ArrayList<>();
        assertEquals(HIDDEN, evaluate(cards, "negative", null, successful()));
        Visit draft = visit(KVP_BIO_MEDICAL_SERVICE_VISIT, 26, null);
        draft.setProcessed(false);
        draft.setFormSubmissionId(null);
        cards.add(draft);
        assertEquals(HIDDEN, evaluate(cards, "negative", null, successful()));
        cards = closedDay(26, null);
        cards.remove(3);
        assertEquals(HIDDEN, evaluate(cards, "negative", null, successful()));
    }

    @Test
    public void missingHtsRequiresAllFourExactLocalSuccessOutcomes() throws Exception {
        List<Visit> cards = closedDay(26, null);
        Visit biomedical = cards.get(0);
        for (Outcome unproven : Outcome.values()) {
            if (unproven != Outcome.LOCAL_SUCCESS) {
                assertEquals(HIDDEN, evaluate(cards, null, null,
                        visit -> visit == biomedical ? unproven : Outcome.LOCAL_SUCCESS));
                assertEquals(HIDDEN, evaluate(cards, null, null,
                        visit -> visit == cards.get(3) ? unproven : Outcome.LOCAL_SUCCESS));
            }
        }
        assertTrue(evaluate(cards, null, null, successful()).canRegister());
    }

    @Test
    public void historyReplacementDoesNotInheritAnEarlierSubmissionOutcome() throws Exception {
        List<Visit> cards = closedDay(26, "negative");
        Visit prior = cards.get(3);
        Visit replacement = visit(KVP_OTHER_SERVICE_VISIT, 26, null);
        replacement.setFormSubmissionId("new-other-submission");
        cards.set(3, replacement);
        assertEquals(prior.getVisitId(), replacement.getVisitId());
        assertEquals(HIDDEN, evaluate(cards, null, null,
                card -> card == replacement ? Outcome.ENQUEUE_INCOMPLETE : Outcome.LOCAL_SUCCESS));
        assertEquals(PENDING, evaluate(cards, null, null, successful()));

        cards.add(prior);
        assertEquals(HIDDEN, evaluate(cards, null, null, successful()));
    }

    @Test
    public void allCardsMustBelongToTheSameClientAndLocalDay() throws Exception {
        List<Visit> cards = closedDay(26, null);
        cards.get(3).setDate(day(25, 23, 59).getTime());
        assertEquals(HIDDEN, evaluate(cards, null, null, successful()));
        cards.set(3, visit(KVP_OTHER_SERVICE_VISIT, 26, null));
        cards.get(3).setBaseEntityId("another-client");
        assertEquals(HIDDEN, evaluate(cards, null, null, successful()));
    }

    @Test
    public void exactEarlierClosureSurvivesRolloverButUnverifiedNewBiomedicalBlocksEntry() throws Exception {
        List<Visit> cards = closedDay(25, "negative");
        assertEquals(PENDING, evaluate(cards, null, null, successful()));
        Visit newer = visit(KVP_BIO_MEDICAL_SERVICE_VISIT, 26, "positive");
        cards.add(newer);
        assertEquals(HIDDEN, evaluate(cards, null, null,
                card -> card == newer ? Outcome.QUEUED : Outcome.LOCAL_SUCCESS));
        assertEquals(HIDDEN, evaluate(cards, null, null, successful()));
    }

    @Test
    public void newCardWithoutHtsKeepsTheEarlierRecordedExclusion() throws Exception {
        List<Visit> cards = closedDay(25, "known_positive");
        cards.addAll(closedDay(26, null));
        assertEquals(HIDDEN, evaluate(cards, "negative", null, successful()));
    }

    @Test
    public void exactHtsAndScreeningExclusionsAlsoSuppressConsentNoRetry() throws Exception {
        for (String result : new String[]{"positive", "known_positive", "inconclusive", "refused_results"}) {
            assertEquals(HIDDEN, evaluate(closedDay(26, result), null, false, successful()));
        }
        assertEquals(HIDDEN, evaluate(closedDay(26, "negative"), "positive", false, successful()));
    }

    @Test
    public void aRealMissingResultAllowsConsentNoRetryAndConfirmedRegistrationHasNoLaunch() throws Exception {
        assertEquals(CONSENT_DECLINED, evaluate(closedDay(26, null), null, false, successful()));
        assertTrue(evaluate(closedDay(26, "negative"), null, false, successful()).canRegister());
        MarpPrepRegistrationEntry registered = evaluate(new ArrayList<>(), "positive", true, successful());
        assertEquals(REGISTERED, registered);
        assertFalse(registered.canRegister());
    }

    @Test
    public void unreadableOrMismatchedBiomedicalPayloadDoesNotCountAsMissingHts() {
        List<Visit> cards = closedDay(26, null);
        Visit biomedical = cards.get(0);
        biomedical.setJson(biomedical.getJson().replace(biomedical.getFormSubmissionId(), "another-submission"));
        assertThrows(JSONException.class, () -> evaluate(cards, null, null, successful()));
        biomedical.setJson("{\"obs\":[]}");
        assertThrows(JSONException.class, () -> evaluate(cards, null, null, successful()));
    }

    private static MarpPrepRegistrationEntry evaluate(List<Visit> cards, String screening, Boolean consent,
                                                       Function<Visit, Outcome> outcome) throws JSONException {
        return MarpPrepRegistrationReadback.evaluate(cards, CLIENT, 28, TODAY, screening, consent, outcome);
    }

    private static Function<Visit, Outcome> successful() {
        return visit -> Outcome.LOCAL_SUCCESS;
    }

    private static List<Visit> closedDay(int serviceDate, String result) {
        List<Visit> cards = new ArrayList<>();
        cards.add(visit(KVP_BIO_MEDICAL_SERVICE_VISIT, serviceDate, result));
        cards.add(visit(KVP_BEHAVIORAL_SERVICE_VISIT, serviceDate, null));
        cards.add(visit(KVP_STRUCTURAL_SERVICE_VISIT, serviceDate, null));
        cards.add(visit(KVP_OTHER_SERVICE_VISIT, serviceDate, null));
        return cards;
    }

    private static Visit visit(String type, int serviceDate, String result) {
        Visit visit = new Visit();
        visit.setBaseEntityId(CLIENT);
        visit.setVisitType(type);
        visit.setDate(day(serviceDate, 0, 1).getTime());
        visit.setVisitId(type + "-" + serviceDate);
        visit.setFormSubmissionId(visit.getVisitId() + "-submission");
        visit.setProcessed(true);
        String observations = result == null ? "[]" : "[{\"fieldCode\":\"hiv_status\",\"values\":[\"" + result + "\"]}]";
        visit.setJson("{\"baseEntityId\":\"" + CLIENT + "\",\"eventType\":\"" + type
                + "\",\"formSubmissionId\":\"" + visit.getFormSubmissionId() + "\",\"obs\":" + observations + "}");
        return visit;
    }

    private static Calendar day(int date, int hour, int minute) {
        Calendar time = Calendar.getInstance(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
        time.clear();
        time.set(2026, Calendar.NOVEMBER, date, hour, minute);
        return time;
    }
}
