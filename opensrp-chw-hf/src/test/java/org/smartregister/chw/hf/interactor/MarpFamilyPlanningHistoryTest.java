package org.smartregister.chw.hf.interactor;

import static org.junit.Assert.assertEquals;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.observation;

import org.junit.Test;
import org.smartregister.chw.hf.utils.MarpPregnancyStatus;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.util.List;

public class MarpFamilyPlanningHistoryTest {
    private static final String METHOD = "family_planning_service";
    private static final String CONDOM = "condom_provided";

    @Test
    public void submittingNewPregnantVisitNeedsNoFamilyPlanningObservations() {
        Event visit = pregnantVisit();
        KvpBioMedicalServiceInteractor.retainHiddenFamilyPlanningAnswers(null, visit);
        assertEquals(1, visit.getObs().size());
        assertEquals(MarpPregnancyStatus.PREGNANT, observation(visit, MarpPregnancyStatus.FIELD));
    }

    @Test
    public void resavingPregnantHistoryRetainsOnlyPreviouslyRecordedFamilyPlanningAnswers() {
        Event original = new Event();
        record(original, METHOD, "implanon");
        record(original, CONDOM, "no");
        record(original, "condoms_given", "yes");

        Event replacement = pregnantVisit();
        KvpBioMedicalServiceInteractor.retainHiddenFamilyPlanningAnswers(original, replacement);

        assertEquals("implanon", observation(replacement, METHOD));
        assertEquals("no", observation(replacement, CONDOM));
        assertEquals("", observation(replacement, "condoms_given"));
        assertEquals(3, replacement.getObs().size());
    }

    @Test
    public void resavingWhenVisibleKeepsTheNewFamilyPlanningAnswer() {
        Event original = new Event();
        record(original, METHOD, "implanon");
        Event replacement = new Event();
        record(replacement, MarpPregnancyStatus.FIELD, MarpPregnancyStatus.BREASTFEEDING);
        record(replacement, METHOD, "jadelle");

        KvpBioMedicalServiceInteractor.retainHiddenFamilyPlanningAnswers(original, replacement);

        assertEquals("jadelle", observation(replacement, METHOD));
        assertEquals(2, replacement.getObs().size());
    }

    private static Event pregnantVisit() {
        Event visit = new Event();
        record(visit, MarpPregnancyStatus.FIELD, MarpPregnancyStatus.PREGNANT);
        return visit;
    }

    private static void record(Event event, String field, String value) {
        Obs answer = new Obs();
        answer.setFormSubmissionField(field);
        answer.setValues(List.of(value));
        event.addObs(answer);
    }
}
