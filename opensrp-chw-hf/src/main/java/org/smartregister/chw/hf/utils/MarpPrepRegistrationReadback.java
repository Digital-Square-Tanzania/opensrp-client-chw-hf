package org.smartregister.chw.hf.utils;

import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.MarpVisitProcessing;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import timber.log.Timber;

import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.HIDDEN;
import static org.smartregister.chw.hf.utils.MarpPrepRegistrationEntry.REGISTERED;
import static org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
import static org.smartregister.chw.kvp.util.MarpVisitProcessing.Outcome.LOCAL_SUCCESS;

public final class MarpPrepRegistrationReadback {
    private static final String CONSENT = "agreed_to_use_prep";

    private MarpPrepRegistrationReadback() {
    }

    public static MarpPrepRegistrationEntry read(VisitRepository repository, String clientId, int age, Calendar today) {
        try {
            SQLiteDatabase database = repository.getWritableDatabase();
            boolean ownsTransaction = !database.inTransaction();
            // Keep visit snapshots, local outcomes and consent on the same database view.
            if (ownsTransaction) {
                database.beginTransactionNonExclusive();
            }
            try {
                Boolean consent = readConsent(database, clientId);
                if (Boolean.TRUE.equals(consent)) {
                    return REGISTERED;
                }
                if (age < 15) {
                    return HIDDEN;
                }
                JSONObject screening = latestEvent(database, clientId, Constants.EVENT_TYPE.KVP_REGISTRATION);
                if (screening == null) {
                    return HIDDEN;
                }
                String screeningStatus = answer(screening, "hiv_status_on_screening", "positive", "negative");
                return evaluate(readCards(database, clientId), clientId, age, today, screeningStatus, consent,
                        MarpVisitProcessing::getLocalOutcome);
            } finally {
                if (ownsTransaction) {
                    database.endTransaction();
                }
            }
        } catch (JSONException | RuntimeException exception) {
            Timber.w("Unable to verify local MARP PrEP registration evidence");
            return HIDDEN;
        }
    }

    static MarpPrepRegistrationEntry evaluate(List<Visit> visits, String clientId, int age, Calendar today,
                                               String screeningStatus, Boolean consent,
                                               Function<Visit, MarpVisitProcessing.Outcome> outcome) throws JSONException {
        if (Boolean.TRUE.equals(consent)) {
            return REGISTERED;
        }
        Map<Long, Map<String, List<Visit>>> days = new TreeMap<>(Collections.reverseOrder());
        for (Visit visit : visits) {
            if (!clientId.equals(visit.getBaseEntityId()) || !MarpVisitProcessing.isServiceType(visit.getVisitType())
                    || MarpVisitStatus.isDraft(visit)) {
                continue;
            }
            if (visit.getDate() == null || dayOf(visit.getDate(), today) > dayOf(today.getTime(), today)) {
                return HIDDEN;
            }
            days.computeIfAbsent(dayOf(visit.getDate(), today), day -> new HashMap<>())
                    .computeIfAbsent(visit.getVisitType(), type -> new ArrayList<>()).add(visit);
        }

        boolean closedAndProcessed = false;
        String hts = null;
        for (Map<String, List<Visit>> cards : days.values()) {
            List<Visit> biomedicalCards = cards.get(KVP_BIO_MEDICAL_SERVICE_VISIT);
            if (hts == null && biomedicalCards != null) {
                if (!hasLocalSuccess(biomedicalCards, outcome)) {
                    return HIDDEN;
                }
                hts = biomedicalAnswer(biomedicalCards.get(0));
            }
            if (!closedAndProcessed && MarpVisitStatus.hasAllServiceCards(cards.keySet())) {
                closedAndProcessed = cards.values().stream().allMatch(card -> hasLocalSuccess(card, outcome));
            }
            if (closedAndProcessed && hts != null) {
                break;
            }
        }
        return MarpPrepRegistrationEntry.evaluate(closedAndProcessed, age, hts, screeningStatus, consent);
    }

    private static boolean hasLocalSuccess(List<Visit> cards, Function<Visit, MarpVisitProcessing.Outcome> outcome) {
        return cards.size() == 1 && outcome.apply(cards.get(0)) == LOCAL_SUCCESS;
    }

    private static String biomedicalAnswer(Visit visit) throws JSONException {
        JSONObject payload = new JSONObject(visit.getJson());
        if (!matchesEvent(payload, visit.getBaseEntityId(), visit.getVisitType(), visit.getFormSubmissionId())) {
            throw new JSONException("Biomedical payload identity is unverified");
        }
        return answer(payload, "hiv_status", "negative", "positive", "known_positive", "inconclusive", "refused_results");
    }

    private static long dayOf(Date date, Calendar calendar) {
        Calendar day = (Calendar) calendar.clone();
        day.setTime(date);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        return day.getTimeInMillis();
    }

    private static List<Visit> readCards(SQLiteDatabase database, String clientId) {
        List<Visit> visits = new ArrayList<>();
        // A failed read must not appear as an empty result from a newer Biomedical visit.
        try (Cursor rows = database.rawQuery("SELECT visit_id, visit_type, visit_date, form_submission_id, "
                + "processed, visit_json FROM visits WHERE base_entity_id = ? AND visit_type IN (?, ?, ?, ?)",
                new String[]{clientId, KVP_BIO_MEDICAL_SERVICE_VISIT,
                        Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT, Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT,
                        Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT})) {
            while (rows.moveToNext()) {
                Visit visit = new Visit();
                visit.setBaseEntityId(clientId);
                visit.setVisitId(rows.getString(0));
                visit.setVisitType(rows.getString(1));
                visit.setFormSubmissionId(rows.getString(3));
                visit.setProcessed(rows.getInt(4) == 1);
                if (MarpVisitStatus.isDraft(visit)) {
                    continue;
                }
                visit.setDate(new Date(Long.parseLong(rows.getString(2))));
                visit.setJson(rows.getString(5));
                visits.add(visit);
            }
        }
        return visits;
    }

    private static Boolean readConsent(SQLiteDatabase database, String clientId) throws JSONException {
        String projected = null;
        boolean hasProjection;
        try (Cursor rows = database.rawQuery("SELECT " + CONSENT + " FROM ec_prep_register WHERE base_entity_id = ?",
                new String[]{clientId})) {
            hasProjection = rows.moveToFirst();
            if (hasProjection) {
                projected = rows.getString(0);
                if (rows.moveToNext()) {
                    throw new JSONException("PrEP registration projection is ambiguous");
                }
            }
        }
        if ("yes".equalsIgnoreCase(projected)) {
            return true;
        }
        JSONObject registration = latestEvent(database, clientId, Constants.EVENT_TYPE.PrEP_REGISTRATION);
        if (registration != null) {
            if ("no".equals(answer(registration, CONSENT, "yes", "no"))) {
                return false;
            }
            // Consent Yes can be persisted before the register projection is updated.
            throw new JSONException("PrEP consent has no confirmed register projection");
        }
        if ("no".equalsIgnoreCase(projected)) {
            return false;
        }
        if (hasProjection) {
            throw new JSONException("PrEP consent is unverified");
        }
        return null;
    }

    private static JSONObject latestEvent(SQLiteDatabase database, String clientId, String type) throws JSONException {
        try (Cursor rows = database.rawQuery("SELECT json, formSubmissionId, eventDate, dateCreated FROM event "
                + "WHERE baseEntityId = ? AND eventType = ? ORDER BY eventDate DESC, dateCreated DESC LIMIT 2",
                new String[]{clientId, type})) {
            if (!rows.moveToFirst()) {
                return null;
            }
            JSONObject payload = new JSONObject(rows.getString(0));
            String submission = rows.getString(1);
            String eventDate = rows.getString(2);
            String dateCreated = rows.getString(3);
            if (!matchesEvent(payload, clientId, type, submission) || StringUtils.isBlank(eventDate)
                    || (rows.moveToNext() && eventDate.equals(rows.getString(2))
                    && Objects.equals(dateCreated, rows.getString(3)))) {
                throw new JSONException("Local registration event identity or order is unverified");
            }
            return payload;
        }
    }

    private static boolean matchesEvent(JSONObject payload, String clientId, String type, String submission) {
        return StringUtils.isNotBlank(submission) && submission.equals(payload.optString("formSubmissionId"))
                && clientId.equals(payload.optString("baseEntityId")) && type.equals(payload.optString("eventType"));
    }

    private static String answer(JSONObject event, String field, String... choices) throws JSONException {
        JSONArray observations = event.getJSONArray("obs");
        String recorded = null;
        for (int index = 0; index < observations.length(); index++) {
            JSONObject observation = observations.getJSONObject(index);
            if (!field.equals(observation.optString("fieldCode"))
                    && !field.equals(observation.optString("formSubmissionField"))) {
                continue;
            }
            JSONArray values = observation.getJSONArray("values");
            if (recorded != null || values.length() != 1) {
                throw new JSONException("Local registration answer is ambiguous");
            }
            String value = values.getString(0);
            for (String choice : choices) {
                if (choice.equalsIgnoreCase(value)) {
                    recorded = choice;
                    break;
                }
            }
            if (recorded == null) {
                throw new JSONException("Local registration answer is unverified");
            }
        }
        return recorded;
    }
}
