package org.smartregister.chw.hf.interactor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class MarpHtsVisitEligibilityTest {
    private static final String CLIENT = "synthetic-marp-client";
    private static final String BIOMEDICAL = Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
    private static final String BEHAVIORAL = Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT;
    private final VisitRepository repository = mock(VisitRepository.class);
    private final Calendar today = time(24);

    @Test
    public void allowsFirstBiomedicalCardAfterAnotherCardOnTheSameDay() {
        Visit sameDayVisit = submittedVisit(24);
        when(repository.getVisits(CLIENT, BEHAVIORAL)).thenReturn(List.of(sameDayVisit));
        assertTrue(KvpBioMedicalServiceInteractor.canOfferHtsSuggestion(false, "", repository, CLIENT, today));
    }

    @Test
    public void suppressesExplicitEditsAndAlreadyRecordedResults() {
        assertFalse(KvpBioMedicalServiceInteractor.canOfferHtsSuggestion(true, "", repository, CLIENT, today));
        assertFalse(KvpBioMedicalServiceInteractor.canOfferHtsSuggestion(false, "positive", repository, CLIENT, today));
    }

    @Test
    public void suppressesPreviouslySubmittedBiomedicalAndEarlierPartialVisits() {
        Visit sameDayVisit = submittedVisit(24);
        when(repository.getLatestVisit(CLIENT, BIOMEDICAL)).thenReturn(sameDayVisit);
        assertFalse(KvpBioMedicalServiceInteractor.canOfferHtsSuggestion(false, "", repository, CLIENT, today));
        when(repository.getLatestVisit(CLIENT, BIOMEDICAL)).thenReturn(null);
        Visit earlierVisit = submittedVisit(23);
        when(repository.getVisits(CLIENT, BEHAVIORAL)).thenReturn(List.of(earlierVisit));
        assertFalse(KvpBioMedicalServiceInteractor.canOfferHtsSuggestion(false, "", repository, CLIENT, today));
    }

    private static Visit submittedVisit(int day) {
        Visit visit = mock(Visit.class);
        when(visit.getDate()).thenReturn(time(day).getTime());
        when(visit.getFormSubmissionId()).thenReturn("synthetic-submission");
        return visit;
    }

    private static Calendar time(int day) {
        Calendar result = Calendar.getInstance(TimeZone.getTimeZone("Africa/Dar_es_Salaam"));
        result.clear();
        result.set(2026, Calendar.NOVEMBER, day, 9, 0);
        return result;
    }
}
