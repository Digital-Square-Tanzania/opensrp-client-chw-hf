package org.smartregister.chw.hf.dao;

import static org.junit.Assert.assertEquals;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.BREASTFEEDING;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.FIELD;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.NEITHER;
import static org.smartregister.chw.hf.utils.MarpPregnancyStatus.PREGNANT;

import com.google.gson.Gson;

import org.junit.Test;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class MarpPregnancyStatusHistoryTest {
    @Test
    public void latestSubmittedAnswerSkipsDraftsAndLegacyVisitsWithoutAnswers() {
        Visit mostRecentAnswer = visit(3000, BREASTFEEDING, true);
        Visit lateEntryForOlderVisit = visit(1000, PREGNANT, true);
        Visit unanswered = visit(4000, "", true);
        Visit draft = visit(5000, NEITHER, false);
        assertEquals(BREASTFEEDING, HfKvpDao.latestPregnancyStatus(
                List.of(mostRecentAnswer, lateEntryForOlderVisit, unanswered, draft), List.of()));
        assertEquals("", HfKvpDao.latestPregnancyStatus(List.of(unanswered, draft), List.of()));
    }

    @Test
    public void includesServerRestoredAnswersAndPrefersLocalAnswerOnDateTie() {
        Visit submitted = visit(2000, NEITHER, true);
        Map<String, Object> restored = Map.of("json", eventJson(3000, BREASTFEEDING));
        Map<String, Object> older = Map.of("json", eventJson(1000, PREGNANT));
        Map<String, Object> sameTime = Map.of("json", eventJson(2000, PREGNANT));
        assertEquals(BREASTFEEDING, HfKvpDao.latestPregnancyStatus(List.of(submitted), List.of(older, restored)));
        assertEquals(NEITHER, HfKvpDao.latestPregnancyStatus(List.of(submitted), List.of(older, sameTime)));
        assertEquals(PREGNANT, HfKvpDao.latestPregnancyStatus(List.of(), List.of(older)));
    }

    private static Visit visit(long timestamp, String answer, boolean submitted) {
        Visit visit = new Visit();
        visit.setDate(new Date(timestamp));
        visit.setPreProcessedJson(eventJson(timestamp, answer));
        if (submitted) {
            visit.setFormSubmissionId("synthetic-" + timestamp);
        }
        return visit;
    }

    private static String eventJson(long timestamp, String answer) {
        Event event = (Event) new Event().withEventDate(new Date(timestamp));
        if (!answer.isEmpty()) {
            Obs obs = new Obs();
            obs.setFormSubmissionField(FIELD);
            obs.setValues(List.of(answer));
            event.addObs(obs);
        }
        return new Gson().toJson(event);
    }
}
