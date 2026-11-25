package org.smartregister.chw.hf.utils;

import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.util.regex.Pattern;

public final class MarpHts {
    public static final String ENCOUNTER_TYPE = "KVP HTS";
    public static final String CTC_SERVICES = "ctc_services";
    public static final String CTC_NUMBER = "ctc_number";

    private static final Pattern UNFORMATTED_CTC_ID = Pattern.compile("[0-9]{14}");

    private MarpHts() {
    }

    public static boolean isPositiveResult(String result) {
        return "positive".equalsIgnoreCase(result) || "known_positive".equalsIgnoreCase(result);
    }

    public static boolean isCtcEnrolled(String answer) {
        return "enrolled_but_not_on_arv".equals(answer) || "enrolled_and_on_arv".equals(answer);
    }

    public static String formatCtcId(String value) {
        if (!UNFORMATTED_CTC_ID.matcher(value).matches()) {
            return value;
        }
        return value.substring(0, 2) + "-" + value.substring(2, 4) + "-"
                + value.substring(4, 8) + "-" + value.substring(8);
    }

    public static void retainRemovedAnswers(Event original, Event replacement) {
        if (original == null || original.getObs() == null) {
            return;
        }
        // A Biomedical edit replaces the event. Carry forward the observations that HTS no longer edits.
        for (Obs obs : original.getObs()) {
            String field = obs.getFormSubmissionField();
            if ("client_type".equals(field) || "received_test_results".equals(field)) {
                replacement.addObs(obs);
            }
        }
    }
}
