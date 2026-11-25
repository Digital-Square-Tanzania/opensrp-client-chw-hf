package org.smartregister.chw.hf.utils;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public final class MarpVisitStatus {
    private static final String[] SERVICE_VISIT_TYPES = {
            Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT,
            Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT,
            Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT,
            Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT
    };

    private final boolean hasVisitHistory;
    private final boolean hasSubmittedVisit;
    private final Set<String> submittedCardsToday;

    private MarpVisitStatus(boolean hasVisitHistory, boolean hasSubmittedVisit, Set<String> submittedCardsToday) {
        this.hasVisitHistory = hasVisitHistory;
        this.hasSubmittedVisit = hasSubmittedVisit;
        this.submittedCardsToday = submittedCardsToday;
    }

    public static MarpVisitStatus read(VisitRepository repository, String baseEntityId, Calendar today) {
        boolean hasVisitHistory = false;
        boolean hasSubmittedVisit = false;
        Set<String> submittedCardsToday = new HashSet<>();
        for (String visitType : SERVICE_VISIT_TYPES) {
            for (Visit visit : repository.getVisits(baseEntityId, visitType)) {
                hasVisitHistory = true;
                if (isSubmitted(visit)) {
                    hasSubmittedVisit = true;
                    if (isOnDate(visit.getDate(), today)) {
                        submittedCardsToday.add(visitType);
                        break;
                    }
                }
            }
        }
        return new MarpVisitStatus(hasVisitHistory, hasSubmittedVisit, submittedCardsToday);
    }

    public boolean isInProgress(VisitRepository repository, String baseEntityId) {
        if (!submittedCardsToday.isEmpty()) {
            return !isCompleteToday();
        }
        for (String visitType : SERVICE_VISIT_TYPES) {
            if (isDraft(repository.getLatestVisit(baseEntityId, visitType))) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDraft(Visit visit) {
        return visit != null && !isSubmitted(visit);
    }

    public static boolean hasSubmittedVisitBeforeToday(VisitRepository repository, String baseEntityId, Calendar today) {
        Date now = today.getTime();
        for (String visitType : SERVICE_VISIT_TYPES) {
            for (Visit visit : repository.getVisits(baseEntityId, visitType)) {
                Date date = visit.getDate();
                if (isSubmitted(visit) && date != null && date.before(now) && !isOnDate(date, today)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSubmitted(Visit visit) {
        // Submit assigns the form submission ID before the event is processed.
        return Boolean.TRUE.equals(visit.getProcessed()) || StringUtils.isNotBlank(visit.getFormSubmissionId());
    }

    private static boolean isOnDate(Date date, Calendar today) {
        if (date == null) {
            return false;
        }
        Calendar visitDay = (Calendar) today.clone();
        visitDay.setTime(date);
        return visitDay.get(Calendar.ERA) == today.get(Calendar.ERA)
                && visitDay.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && visitDay.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);
    }

    public boolean hasVisitHistory() {
        return hasVisitHistory;
    }

    public boolean hasSubmittedVisit() {
        return hasSubmittedVisit;
    }

    public boolean hasSubmittedCardToday(String visitType) {
        return submittedCardsToday.contains(visitType);
    }

    public boolean isCompleteToday() {
        return submittedCardsToday.size() == SERVICE_VISIT_TYPES.length;
    }
}
