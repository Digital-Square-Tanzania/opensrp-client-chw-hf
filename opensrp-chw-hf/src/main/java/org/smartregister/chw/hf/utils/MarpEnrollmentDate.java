package org.smartregister.chw.hf.utils;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;

public final class MarpEnrollmentDate {
    public static final String EVENT_DETAIL = "marp_enrollment_date";
    public static final String CLIENT_STATUS = "client_status";
    public static final String ENROLLMENT_DATE = "enrollment_date";
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);

    private MarpEnrollmentDate() {
    }

    public static boolean isTransferOrReturn(String visitType) {
        return "transfer_in".equals(visitType) || "return".equals(visitType);
    }

    public static String fromTimestamp(long milliseconds) {
        return FORMAT.format(Instant.ofEpochMilli(milliseconds).atZone(ZoneId.systemDefault()).toLocalDate());
    }

    public static String observation(Event event, String field) {
        if (event != null && event.getObs() != null) {
            for (Obs obs : event.getObs()) {
                if (field.equals(obs.getFormSubmissionField())) {
                    List<Object> values = obs.getValues();
                    return values != null && values.size() == 1 && values.get(0) != null
                            ? values.get(0).toString() : "";
                }
            }
        }
        return "";
    }

    public static void recordFirstVisitDate(Event event) {
        String date = observation(event, ENROLLMENT_DATE);
        if (!isValid(date)) {
            throw new IllegalArgumentException("A first Transfer in or Return requires a valid MARP enrolment date");
        }
        event.addDetails(EVENT_DETAIL, date);
    }

    public static void retainOnResave(Event original, Event replacement) {
        String visitType = observation(replacement, CLIENT_STATUS);
        if (!isTransferOrReturn(visitType) || !visitType.equals(observation(original, CLIENT_STATUS))) {
            return;
        }

        String date = original.getDetails() == null ? null : original.getDetails().get(EVENT_DETAIL);
        if (StringUtils.isNotBlank(date)) {
            replacement.addDetails(EVENT_DETAIL, date);
        }

        // Preserve visit history as well. Observation-only legacy dates never become register overrides.
        for (Obs obs : original.getObs()) {
            if (ENROLLMENT_DATE.equals(obs.getFormSubmissionField())) {
                replacement.getObs().removeIf(value -> ENROLLMENT_DATE.equals(value.getFormSubmissionField()));
                replacement.addObs(obs);
                break;
            }
        }
    }

    public static boolean isValid(String value) {
        if (value == null) {
            return false;
        }
        try {
            LocalDate date = LocalDate.parse(value, FORMAT);
            return date.getYear() > 0 && !date.isAfter(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
