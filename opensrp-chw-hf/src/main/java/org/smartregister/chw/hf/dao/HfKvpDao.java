package org.smartregister.chw.hf.dao;

import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.CLIENT_STATUS;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.clientandeventmodel.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
        // The submitted event is available before the sync processor projects it into the register.
        for (Visit visit : visits) {
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
        String payload = StringUtils.defaultIfBlank(visit.getPreProcessedJson(), visit.getJson());
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
