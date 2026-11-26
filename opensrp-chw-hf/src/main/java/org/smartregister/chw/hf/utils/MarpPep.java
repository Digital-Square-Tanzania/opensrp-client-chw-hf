package org.smartregister.chw.hf.utils;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.util.List;
import java.util.Map;

public final class MarpPep {
    public static final String ENCOUNTER_TYPE = "KVP PrEP and PEP";
    public static final String NEED = "pep_required";
    public static final String SERVICE = "pep_provision";
    public static final String PREP_ENROLMENT = "enrolled_to_prep";
    public static final String YES = "yes";
    public static final String NO = "no";
    public static final String PROVIDED = "provided";
    public static final String NOT_PROVIDED = "not_provided";
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
        if (original == null || original.getObs() == null || replacement == null
                || StringUtils.isNotBlank(MarpEnrollmentDate.observation(replacement, NEED))) {
            return;
        }
        if (original.getObs().stream().anyMatch(obs -> NEED.equals(field(obs)) || SERVICE.equals(field(obs)))) {
            // History replaces the event and rebuilds visit details from its observations.
            replacement.getObs().removeIf(MarpPep::isPepObservation);
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
