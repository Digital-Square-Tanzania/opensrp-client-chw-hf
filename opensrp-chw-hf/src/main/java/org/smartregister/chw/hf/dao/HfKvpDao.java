package org.smartregister.chw.hf.dao;

import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.CLIENT_STATUS;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.hf.utils.MarpPregnancyStatus;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.clientandeventmodel.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class HfKvpDao extends KvpDao {

    public static String getClientStatus(String baseEntityId) {
        String sql = "SELECT client_status FROM ec_kvp_bio_medical_services p " +
                " WHERE p.entity_id = '" + baseEntityId + "' AND trim(p.client_status) <> '' LIMIT 1";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "client_status");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        Event local = findLocalClientStatus(baseEntityId);
        return local == null ? "" : MarpEnrollmentDate.observation(local, CLIENT_STATUS);
    }

    private static Event findLocalClientStatus(String baseEntityId) {
        return findLocalClientStatus(KvpLibrary.getInstance().visitRepository()
                .getVisits(baseEntityId, Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT));
    }

    static Event findLocalClientStatus(List<Visit> visits) {
        return findLocalClientStatus(visits, null);
    }

    private static Event findLocalClientStatus(List<Visit> visits, Date before) {
        // The submitted event is available before the sync processor projects it into the register.
        for (Visit visit : visits) {
            if (!isBefore(visit.getDate(), before)) {
                continue;
            }
            Event event = readSubmittedBiomedicalEvent(visit);
            if (StringUtils.isNotBlank(MarpEnrollmentDate.observation(event, CLIENT_STATUS))) {
                return event;
            }
        }
        return null;
    }

    public static Event readSubmittedBiomedicalEvent(Visit visit) {
        if (visit == null || MarpVisitStatus.isDraft(visit)) {
            return null;
        }
        return readEvent(StringUtils.defaultIfBlank(visit.getPreProcessedJson(), visit.getJson()));
    }

    public static boolean hasRecordedMarpVisitForHts(String baseEntityId, Calendar today) {
        // Synced visits may be in the event store without a row in the local visit repository.
        List<Map<String, Object>> rows = readData(
                "SELECT eventType, json FROM event WHERE baseEntityId = ? AND eventType IN (?, ?, ?, ?)",
                new String[]{baseEntityId, Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT,
                        Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT,
                        Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT,
                        Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT});
        return hasPriorMarpServiceEvent(rows, today) || StringUtils.isNotBlank(getClientStatus(baseEntityId));
    }

    static boolean hasPriorMarpServiceEvent(List<Map<String, Object>> rows, Calendar today) {
        if (rows == null) {
            return false;
        }
        Calendar startOfDay = (Calendar) today.clone();
        startOfDay.set(Calendar.HOUR_OF_DAY, 0);
        startOfDay.set(Calendar.MINUTE, 0);
        startOfDay.set(Calendar.SECOND, 0);
        startOfDay.set(Calendar.MILLISECOND, 0);
        for (Map<String, Object> row : rows) {
            if (Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT.equals(row.get("eventType"))) {
                return true;
            }
            Event event = readEvent((String) row.get("json"));
            if (event != null && event.getEventDate() != null && event.getEventDate().before(startOfDay.getTime())) {
                return true;
            }
        }
        return false;
    }

    public static String getScreeningPregnancyStatus(String baseEntityId) {
        List<Map<String, Object>> rows = readData(
                "SELECT json FROM event WHERE baseEntityId = ? AND eventType = ? "
                        + "ORDER BY eventDate DESC, dateCreated DESC LIMIT 1",
                new String[]{baseEntityId, Constants.EVENT_TYPE.KVP_REGISTRATION});
        Event screening = rows == null || rows.isEmpty() ? null : readEvent((String) rows.get(0).get("json"));
        return MarpPregnancyStatus.fromScreening(screening);
    }

    public static String getLatestPregnancyBreastfeedingStatus(String baseEntityId) {
        String eventType = Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
        List<Visit> visits = KvpLibrary.getInstance().visitRepository().getVisits(baseEntityId, eventType);
        // Server-restored events may not have a row in the local visit repository.
        List<Map<String, Object>> rows = readData(
                "SELECT json FROM event WHERE baseEntityId = ? AND eventType = ? AND json LIKE ? "
                        + "ORDER BY eventDate DESC, dateCreated DESC",
                new String[]{baseEntityId, eventType, "%" + MarpPregnancyStatus.FIELD + "%"});
        return latestPregnancyStatus(visits, rows);
    }

    public static boolean isFamilyPlanningExempt(Visit visit, String gender) {
        String baseEntityId = visit.getBaseEntityId();
        if (!Constants.FEMALE.equalsIgnoreCase(gender)
                || !MarpPregnancyStatus.appliesTo(gender, KvpDao.getDominantKVPGroup(baseEntityId))) {
            return false;
        }

        Event currentEvent = readSubmittedBiomedicalEvent(visit);
        if (currentEvent == null) {
            return false;
        }
        String currentStatus = MarpEnrollmentDate.observation(currentEvent, MarpPregnancyStatus.FIELD);
        if (MarpPregnancyStatus.isAnswer(currentStatus)) {
            return MarpPregnancyStatus.PREGNANT.equals(currentStatus);
        }

        Date visitDate = visit.getDate();
        if (visitDate == null) {
            return false;
        }
        String eventType = Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT;
        VisitRepository repository = KvpLibrary.getInstance().visitRepository();
        List<Visit> visits = repository.getVisits(baseEntityId, eventType);
        List<Map<String, Object>> rows = readData(
                "SELECT json FROM event WHERE baseEntityId = ? AND eventType = ? "
                        + "ORDER BY eventDate DESC, dateCreated DESC",
                new String[]{baseEntityId, eventType});
        // Processing can run days after submission. Resolve the fallback as of this visit.
        String previousStatus = latestPregnancyStatus(visits, rows, visitDate);
        if (MarpPregnancyStatus.isAnswer(previousStatus)) {
            return MarpPregnancyStatus.PREGNANT.equals(previousStatus);
        }

        Calendar visitDay = Calendar.getInstance();
        visitDay.setTime(visitDate);
        return findLocalClientStatus(visits, visitDate) == null
                && !hasPriorSyncedClientStatus(rows, visitDate)
                && !MarpVisitStatus.hasSubmittedVisitBeforeToday(repository, baseEntityId, visitDay)
                && MarpPregnancyStatus.PREGNANT.equals(getScreeningPregnancyStatus(baseEntityId));
    }

    private static boolean hasPriorSyncedClientStatus(List<Map<String, Object>> rows, Date before) {
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Event event = readEvent((String) row.get("json"));
                if (event != null && isBefore(event.getEventDate(), before)
                        && StringUtils.isNotBlank(MarpEnrollmentDate.observation(event, CLIENT_STATUS))) {
                    return true;
                }
            }
        }
        return false;
    }

    static String latestPregnancyStatus(List<Visit> visits, List<Map<String, Object>> eventRows) {
        return latestPregnancyStatus(visits, eventRows, null);
    }

    private static String latestPregnancyStatus(List<Visit> visits, List<Map<String, Object>> eventRows, Date before) {
        String status = "";
        Date latestDate = null;
        // The repository returns visits in creation order. Prefer the most recent clinical date.
        for (Visit visit : visits) {
            String value = MarpEnrollmentDate.observation(readSubmittedBiomedicalEvent(visit), MarpPregnancyStatus.FIELD);
            Date date = visit.getDate();
            if (MarpPregnancyStatus.isAnswer(value) && date != null && isBefore(date, before)
                    && (latestDate == null || !date.before(latestDate))) {
                status = value;
                latestDate = date;
            }
        }
        if (eventRows != null) {
            for (Map<String, Object> row : eventRows) {
                Event event = readEvent((String) row.get("json"));
                String value = MarpEnrollmentDate.observation(event, MarpPregnancyStatus.FIELD);
                if (MarpPregnancyStatus.isAnswer(value) && event.getEventDate() != null
                        && isBefore(event.getEventDate(), before)
                        && (latestDate == null || event.getEventDate().after(latestDate))) {
                    status = value;
                    latestDate = event.getEventDate();
                }
            }
        }
        return status;
    }

    private static boolean isBefore(Date date, Date before) {
        return before == null || (date != null && date.before(before));
    }

    private static Event readEvent(String payload) {
        try {
            return new Gson().fromJson(payload, Event.class);
        } catch (JsonParseException e) {
            Timber.e(e);
            return null;
        }
    }

    public static String getVmmcServices(String baseEntityId) {
        String sql = "SELECT vmcc_provided FROM ec_kvp_bio_medical_services p " +
                " WHERE p.entity_id = '" + baseEntityId + "' AND vmcc_provided IS NOT NULL ORDER BY last_interacted_with DESC LIMIT 1";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "vmcc_provided");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }

    public static String getClientEnrollmentDate(String baseEntityId) {
        Event local = findLocalClientStatus(baseEntityId);
        if (local != null && local.getDetails() != null) {
            String date = local.getDetails().get(MarpEnrollmentDate.EVENT_DETAIL);
            if (StringUtils.isNotBlank(date)) {
                return date;
            }
        }
        return getStoredEnrollmentDate(baseEntityId);
    }

    public static String getStoredEnrollmentDate(String baseEntityId) {
        String sql = "SELECT enrollment_date FROM ec_kvp_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "'";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "enrollment_date");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }

    public static String getOtherKvpClientGroups(String baseEntityId) {
        String sql = "SELECT other_screened_client_group FROM ec_kvp_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "'";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "other_screened_client_group");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }


    public static boolean hasPrepFollowup(String baseEntityId) {
        String sql = "SELECT visit_type FROM ec_prep_followup p " +
                " WHERE p.entity_id = '" + baseEntityId + "'";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "visit_type");

        List<String> res = readData(sql, dataMap);
        return res != null && !res.isEmpty() && res.get(0) != null;
    }

    public static Date getHbvTestDate(String baseEntityId) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String sql = "SELECT hbv_test_date FROM ec_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "'";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "hbv_test_date");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            try {
                return simpleDateFormat.parse(res.get(0));
            } catch (ParseException e) {
                Timber.e(e);
                return null;
            }
        }
        return null;
    }

    public static Date getHcvTestDate(String baseEntityId) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String sql = "SELECT hcv_test_date FROM ec_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "'";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "hcv_test_date");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            try {
                return simpleDateFormat.parse(res.get(0));
            } catch (ParseException e) {
                Timber.e(e);
                return null;
            }
        }
        return null;
    }

    public static Date getCrclTestDate(String baseEntityId) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String sql = "SELECT crcl_test_date FROM ec_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "'";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "crcl_test_date");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            try {
                return simpleDateFormat.parse(res.get(0));
            } catch (ParseException e) {
                Timber.e(e);
                return null;
            }
        }
        return null;
    }

    public static String getCrclResults(String baseEntityId) {
        String sql = "SELECT crcl_results FROM ec_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "'";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "crcl_results");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return null;
    }

    public static String getPrepInitiationDate(String baseEntityId) {
        String sql = "SELECT prep_initiation_date FROM ec_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "'";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "prep_initiation_date");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return null;
    }

    public static boolean wereSelfTestingKitsDistributed(String baseEntityId) {
        String sql = "SELECT kits_distributed FROM ec_kvp_bio_medical_services p " +
                " WHERE p.entity_id = '" + baseEntityId + "' ORDER BY kvp_visit_date DESC LIMIT 1";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "kits_distributed");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0).equalsIgnoreCase("yes");
        }
        return false;
    }

    public static String getHivStatus(String baseEntityId) {
        String sql = "SELECT hiv_status FROM ec_kvp_bio_medical_services p " +
                " WHERE p.entity_id = '" + baseEntityId + "' ORDER BY kvp_visit_date DESC LIMIT 1";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "hiv_status");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }

    public static String getPrepStatus(String baseEntityId) {
        String sql = "SELECT prep_status FROM ec_prep_followup p " +
                " WHERE p.entity_id = '" + baseEntityId + "' ORDER BY kvp_visit_date DESC LIMIT 1";
        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "prep_status");

        List<String> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0);
        }
        return null;
    }
}
