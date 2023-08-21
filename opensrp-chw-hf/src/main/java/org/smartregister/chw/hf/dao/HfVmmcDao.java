package org.smartregister.chw.hf.dao;

import android.util.Log;

import org.smartregister.chw.vmmc.dao.VmmcDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class HfVmmcDao extends VmmcDao {

//    public static String getClientStatus(String baseEntityId) {
//        String sql = "SELECT client_status FROM ec_vmmc_bio_medical_services p " +
//                " WHERE p.entity_id = '" + baseEntityId + "'";
//
//        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "client_status");
//
//        List<String> res = readData(sql, dataMap);
//        if (res != null && res.size() != 0 && res.get(0) != null) {
//            return res.get(0);
//        }
//        return "";
//    }

//    public static boolean hasVmmcFollowup(String baseEntityId) {
//        String sql = "SELECT visit_type FROM ec_vmmc_followup p " +
//                " WHERE p.entity_id = '" + baseEntityId + "'";
//
//        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "visit_type");
//
//        List<String> res = readData(sql, dataMap);
//        return res != null && res.size() != 0 && res.get(0) != null;
//    }

    //trial ang
    public static int getVisitNumber(String baseEntityID) {
        String sql = "SELECT visit_number  FROM ec_vmmc_follow_up_visit WHERE entity_id='" + baseEntityID + "' ORDER BY visit_number DESC LIMIT 1";
        DataMap<Integer> map = cursor -> getCursorIntValue(cursor, "visit_number");
        List<Integer> res = readData(sql, map);

        if (res != null && res.size() > 0 && res.get(0) != null) {
            return res.get(0) + 1;
        } else
            return 0;

    }
}
