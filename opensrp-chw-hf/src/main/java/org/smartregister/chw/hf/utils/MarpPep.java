package org.smartregister.chw.hf.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.util.JsonFormUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MarpPep {
    public static final String ENCOUNTER_TYPE = "KVP PrEP and PEP";
    public static final String NEED = "pep_required";
    public static final String SERVICE = "pep_provision";
    public static final String PREP_ENROLMENT = "enrolled_to_prep";
    public static final String YES = "yes";
    public static final String NO = "no";
    public static final String PROVIDED = "provided";
    public static final String NOT_PROVIDED = "not_provided";
    public static final String REVIEW_TRIGGER = "marp_pep_review_trigger";
    public static final String REVIEWED_TRIGGER = "marp_pep_reviewed_trigger";
    public static final String SEXUAL_VIOLENCE = "sexual_violence";
    private static final String LEGACY_NOT_PROVIDED = "legacy_not_provided";

    private MarpPep() {
    }

    public static String answer(Map<String, List<VisitDetail>> details, String field) {
        List<VisitDetail> entries = details == null ? null : details.get(field);
        return entries == null || entries.isEmpty() ? "" : KvpJsonFormUtils.getValue(entries.get(0));
    }

    public static boolean isServiceAnswer(String value) {
        return PROVIDED.equals(value) || NOT_PROVIDED.equals(value);
    }

    public static boolean eligible(String knownHivStatus, String visitHivStatus, boolean enrolledInPrep) {
        return !enrolledInPrep && !MarpHts.isPositiveResult(knownHivStatus)
                && !MarpHts.isPositiveResult(visitHivStatus);
    }

    public static boolean hasSexualViolence(Event event) {
        if (event == null || event.getObs() == null) {
            return false;
        }
        for (Obs obs : event.getObs()) {
            if ("gbv_screening".equals(field(obs)) && obs.getValues() != null
                    && containsSexualViolence(obs.getValues())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSexualViolence(Collection<?> values) {
        for (Object value : values) {
            if (SEXUAL_VIOLENCE.equals(value)
                    || (value instanceof Collection && containsSexualViolence((Collection<?>) value))) {
                return true;
            }
        }
        return false;
    }

    public static String reviewTriggerAfterScreening(boolean previouslySelected, boolean selected,
                                                     String currentNeed, String trigger) {
        if (!selected) {
            return null;
        }
        return !previouslySelected && NO.equals(currentNeed) ? UUID.randomUUID().toString() : trigger;
    }

    public static String eventDetail(Event event, String key) {
        return event == null || event.getDetails() == null ? null : event.getDetails().get(key);
    }

    public static void setEventDetail(Event event, String key, String value) {
        if (value != null) {
            event.addDetails(key, value);
        } else if (event.getDetails() != null) {
            event.getDetails().remove(key);
        }
    }

    public static boolean needsReview(String trigger, String reviewed) {
        return trigger != null && !trigger.equals(reviewed);
    }

    public static boolean hasExplicitAnswer(String payload) {
        String need = payloadAnswer(payload, NEED);
        return NO.equals(need) || (YES.equals(need) && isServiceAnswer(payloadAnswer(payload, SERVICE)));
    }

    public static String payloadAnswer(String payload, String key) {
        if (StringUtils.isBlank(payload)) {
            return "";
        }
        try {
            JSONArray fields = new JSONObject(payload).getJSONObject("step1").getJSONArray("fields");
            JSONObject field = JsonFormUtils.getFieldJSONObject(fields, key);
            return field == null ? "" : field.optString(JsonFormUtils.VALUE);
        } catch (JSONException e) {
            return "";
        }
    }

    public static void replaceAnswers(Event assessment, Event biomedical) {
        boolean hadPrepAnswer = biomedical.getObs() != null
                && biomedical.getObs().stream().anyMatch(obs -> PREP_ENROLMENT.equals(field(obs)));
        if (biomedical.getObs() != null) {
            biomedical.getObs().removeIf(obs -> NEED.equals(field(obs)) || SERVICE.equals(field(obs)));
        }
        for (Obs obs : assessment.getObs()) {
            if (isPepObservation(obs) && (!hadPrepAnswer || !PREP_ENROLMENT.equals(field(obs)))) {
                biomedical.addObs(obs);
            }
        }
    }

    public static void retainPrepAnswer(Event original, Event replacement) {
        if (original != null && original.getObs() != null && replacement.getObs() != null) {
            for (Obs obs : original.getObs()) {
                if (PREP_ENROLMENT.equals(field(obs))) {
                    replacement.getObs().removeIf(item -> PREP_ENROLMENT.equals(field(item)));
                    replacement.addObs(obs);
                    return;
                }
            }
        }
    }

    public static void addHistoryAnswers(String need, String service, Map<String, String> history) {
        if (YES.equals(need) || (StringUtils.isBlank(need) && PROVIDED.equals(service))) {
            history.put(NEED, YES);
            if (isServiceAnswer(service)) {
                history.put(SERVICE, service);
            }
        } else if (NO.equals(need)) {
            history.put(NEED, NO);
        } else if (StringUtils.isBlank(need) && NOT_PROVIDED.equals(service)) {
            history.put(SERVICE, LEGACY_NOT_PROVIDED);
        }
    }

    public static void retainUneditedAnswers(Event original, Event replacement) {
        if (original == null || original.getObs() == null || replacement == null) {
            return;
        }
        if (original.getObs().stream().anyMatch(obs -> NEED.equals(field(obs)) || SERVICE.equals(field(obs)))) {
            // History replaces the event and rebuilds visit details from its observations.
            if (replacement.getObs() != null) {
                replacement.getObs().removeIf(MarpPep::isPepObservation);
            }
            for (Obs obs : original.getObs()) {
                if (isPepObservation(obs)) {
                    replacement.addObs(obs);
                }
            }
        }
    }

    private static boolean isPepObservation(Obs obs) {
        String field = field(obs);
        return NEED.equals(field) || SERVICE.equals(field) || PREP_ENROLMENT.equals(field);
    }

    private static String field(Obs obs) {
        return StringUtils.defaultIfBlank(obs.getFormSubmissionField(), obs.getFieldCode());
    }
}
