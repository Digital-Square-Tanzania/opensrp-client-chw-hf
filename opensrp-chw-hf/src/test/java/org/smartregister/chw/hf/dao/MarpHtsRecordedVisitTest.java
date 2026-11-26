package org.smartregister.chw.hf.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.clientandeventmodel.Event;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class MarpHtsRecordedVisitTest {
    @Test
    public void syncedBiomedicalEventSuppressesSuggestionEvenWhenResultAndClientStatusAreAbsent() {
        Event event = new Event().withEventDate(time(24).getTime());
        Map<String, Object> row = Map.of("eventType", Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT,
                "json", new Gson().toJson(event));
        assertTrue(HfKvpDao.hasPriorMarpServiceEvent(List.of(row), time(24)));
    }

    @Test
    public void syncedPartialVisitsSuppressSuggestionOnlyOnSubsequentDays() {
        Event event = new Event().withEventDate(time(24).getTime());
        Map<String, Object> row = Map.of("eventType", Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT,
                "json", new Gson().toJson(event));
        assertFalse(HfKvpDao.hasPriorMarpServiceEvent(List.of(row), time(24)));
        assertTrue(HfKvpDao.hasPriorMarpServiceEvent(List.of(row), time(25)));
    }

    private static Calendar time(int day) {
        Calendar result = Calendar.getInstance(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
        result.clear();
        result.set(2026, Calendar.NOVEMBER, day, 9, 0);
        return result;
    }
}
