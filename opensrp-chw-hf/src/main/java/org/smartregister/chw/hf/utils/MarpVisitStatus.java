package org.smartregister.chw.hf.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpVisitsUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

    public static List<Visit> findProcessableCardsToday(VisitRepository repository, String baseEntityId, Calendar today) {
        List<Visit> cards = new ArrayList<>();
        for (String type : SERVICE_VISIT_TYPES) {
            Visit card = findProcessableCard(repository, baseEntityId, type, today);
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    /** A non-null service day restricts the search to that day. */
    public static Visit findProcessableCard(VisitRepository repository, String baseEntityId, String type,
                                           Calendar serviceDay) {
        Visit latest = repository.getLatestVisit(baseEntityId, type);
        if (latest == null || StringUtils.isBlank(latest.getVisitId())) {
            return null;
        }
        // The shared processor updates by visit ID. Require an unambiguous row for that ID.
        Visit visit = repository.getVisitByVisitId(latest.getVisitId());
        if (visit == null || !baseEntityId.equals(visit.getBaseEntityId()) || !type.equals(visit.getVisitType())
                || !Boolean.FALSE.equals(visit.getProcessed()) || visit.getDate() == null
                || !latest.getVisitId().equals(visit.getVisitId())
                || StringUtils.isBlank(visit.getFormSubmissionId())
                || !visit.getFormSubmissionId().equals(latest.getFormSubmissionId())
                || !Objects.equals(visit.getDate(), latest.getDate())
                || (serviceDay != null && !isOnDate(visit.getDate(), serviceDay))
                || !matchesSubmission(visit, visit.getPreProcessedJson())
                || !matchesSubmission(visit, visit.getJson())) {
            return null;
        }
        String status;
        switch (type) {
            case Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT:
                status = KvpVisitsUtil.getBioMedicalStatus(visit);
                break;
            case Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT:
                status = KvpVisitsUtil.getBehavioralServiceStatus(visit);
                break;
            case Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT:
                status = KvpVisitsUtil.getStructuralServiceStatus(visit);
                break;
            case Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT:
                status = KvpVisitsUtil.getOtherServiceStatus(visit);
                break;
            default:
                return null;
        }
        return KvpVisitsUtil.Complete.equals(status) ? visit : null;
    }

    private static boolean matchesSubmission(Visit visit, String payload) {
        if (StringUtils.isBlank(payload)) {
            return false;
        }
        try {
            JSONObject event = new JSONObject(payload);
            return event.optJSONArray("obs") != null
                    && visit.getBaseEntityId().equals(event.optString("baseEntityId"))
                    && visit.getVisitType().equals(event.optString("eventType"))
                    && visit.getFormSubmissionId().equals(event.optString("formSubmissionId"));
        } catch (JSONException malformedPayload) {
            return false;
        }
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

    public static boolean isOnDate(Date date, Calendar today) {
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
        return hasAllServiceCards(submittedCardsToday);
    }

    static boolean hasAllServiceCards(Set<String> types) {
        return types.containsAll(Arrays.asList(SERVICE_VISIT_TYPES));
    }
}
