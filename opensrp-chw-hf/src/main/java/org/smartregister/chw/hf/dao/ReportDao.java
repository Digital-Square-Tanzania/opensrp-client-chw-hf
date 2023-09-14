package org.smartregister.chw.hf.dao;

import org.smartregister.dao.AbstractDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportDao extends AbstractDao {
    public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

    public static List<Map<String, String>> getMotherChampions(Date reportDate) {
        String sql = "SELECT chw_name, provider_id \n" +
                "from ec_mother_champion_followup \n" +
                "WHERE chw_name IS NOT NULL \n" +
                "  AND date((substr('%s', 1, 4) || '-' || substr('%s', 6, 2) || '-' || '01')) =\n" +
                "      date(substr(strftime('%Y-%m-%d', datetime(last_interacted_with / 1000, 'unixepoch', 'localtime')), 1, 4) ||\n" +
                "           '-' ||\n" +
                "           substr(strftime('%Y-%m-%d', datetime(last_interacted_with / 1000, 'unixepoch', 'localtime')), 6, 2) ||\n" +
                "           '-' || '01')\n" +
                "UNION \n" +
                "SELECT chw_name, provider_id \n" +
                "from ec_anc_partner_community_feedback \n" +
                "WHERE chw_name IS NOT NULL \n" +
                "  AND date((substr('%s', 1, 4) || '-' || substr('%s', 6, 2) || '-' || '01')) =\n" +
                "      date(substr(strftime('%Y-%m-%d', datetime(last_interacted_with / 1000, 'unixepoch', 'localtime')), 1, 4) ||\n" +
                "           '-' ||\n" +
                "           substr(strftime('%Y-%m-%d', datetime(last_interacted_with / 1000, 'unixepoch', 'localtime')), 6, 2) ||\n" +
                "           '-' || '01') \n" +
                "UNION \n" +
                "SELECT chw_name, provider_id \n" +
                "from ec_sbcc \n" +
                "WHERE chw_name IS NOT NULL \n" +
                "  AND date((substr('%s', 1, 4) || '-' || substr('%s', 6, 2) || '-' || '01')) =\n" +
                "      date(substr(sbcc_date, 7, 4) || '-' || substr(sbcc_date, 4, 2) || '-' || '01')\n" +
                "UNION\n" +
                "SELECT chw_name, provider_id\n" +
                "from ec_pmtct_community_feedback\n" +
                "WHERE chw_name IS NOT NULL\n" +
                "  AND date((substr('%s', 1, 4) || '-' || substr('%s', 6, 2) || '-' || '01')) =\n" +
                "      date(substr(strftime('%Y-%m-%d', datetime(pmtct_community_followup_visit_date / 1000, 'unixepoch', 'localtime')),\n" +
                "                  1, 4) ||\n" +
                "           '-' ||\n" +
                "           substr(strftime('%Y-%m-%d', datetime(pmtct_community_followup_visit_date / 1000, 'unixepoch', 'localtime')),\n" +
                "                  6, 2) ||\n" +
                "           '-' || '01')\n";

        String queryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(reportDate);

        sql = sql.contains("%s") ? sql.replaceAll("%s", queryDate) : sql;

        DataMap<Map<String, String>> map = cursor -> {
            Map<String, String> data = new HashMap<>();
            data.put("chw_name", cursor.getString(cursor.getColumnIndex("chw_name")));
            data.put("provider_id", cursor.getString(cursor.getColumnIndex("provider_id")));
            return data;
        };

        List<Map<String, String>> res = readData(sql, map);


        if (res != null && res.size() > 0) {
            return res;
        } else
            return new ArrayList<>();
    }

    public static List<Map<String, String>> getHfIssuingCdpStockLog(Date reportDate) {

        String query1 = " SELECT point_of_service,other_pos ,female_condoms_offset,male_condoms_offset\n" +
                "        FROM ec_cdp_issuing_hf\n" +
                "        WHERE point_of_service='rch_clinic' \n" +
                " OR point_of_service='ctc' OR point_of_service='opd' OR point_of_service='other'\n" +
                " OR point_of_service='tb_clinic' OR point_of_service='outreach'  \n" +
                "        AND date((substr('%s', 1, 4) || '-' || substr('%s', 6, 2) || '-' || '01')) =\n" +
                "        date(substr(condom_restock_date, 7, 4) || '-' || substr(condom_restock_date, 4, 2) || '-' || '01')";


        String query2 =
                "SELECT  requester,cof.condom_type as condom_type,quantity_response\n" +
                        "                       FROM ec_cdp_order_feedback cof\n" +
                        "      INNER JOIN ec_cdp_orders eco ON eco.form_submission_id = cof.request_reference\n" +
                        "  INNER JOIN task t ON t.for = eco.base_entity_id\n" +
                        "        WHERE (t.status='IN_PROGRESS'OR t.status='COMPLETED') \n" +
                        "  AND date((substr('%s', 1, 4) || '-' || substr('%s', 6, 2) || '-' || '01')) =\n" +
                        "      date(substr(strftime('%Y-%m-%d', datetime(response_at / 1000, 'unixepoch', 'localtime')),\n" +
                        "                  1, 4) ||\n" +
                        "           '-' ||\n" +
                        "           substr(strftime('%Y-%m-%d', datetime(response_at / 1000, 'unixepoch', 'localtime')),\n" +
                        "                  6, 2) ||\n" +
                        "           '-' || '01')\n";


        String queryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(reportDate);

        query1 = query1.contains("%s") ? query1.replaceAll("%s", queryDate) : query1;
        query2 = query2.contains("%s") ? query2.replaceAll("%s", queryDate) : query2;

        DataMap<Map<String, String>> map1 = cursor -> {
            Map<String, String> data1 = new HashMap<>();
            data1.put("point_of_service", cursor.getString(cursor.getColumnIndex("point_of_service")));
            data1.put("other_point_of_service", cursor.getString(cursor.getColumnIndex("other_pos")));
            data1.put("female_condoms_offset", cursor.getString(cursor.getColumnIndex("female_condoms_offset")));
            data1.put("male_condoms_offset", cursor.getString(cursor.getColumnIndex("male_condoms_offset")));
            return data1;
        };

        DataMap<Map<String, String>> map2= cursor2 -> {
            Map<String, String> data2 = new HashMap<>();
            data2.put("requester", cursor2.getString(cursor2.getColumnIndex("requester")));
            data2.put("quantity_response", cursor2.getString(cursor2.getColumnIndex("quantity_response")));
            data2.put("condom_type", cursor2.getString(cursor2.getColumnIndex("condom_type")));
            return data2;
        };

        List<Map<String, String>> res1 = readData(query1, map1);
        List<Map<String, String>> res2 = readData(query2, map2);
        List<Map<String, String>> res = new ArrayList<>();
        res.addAll(res1);
        res.addAll(res2);

        if (res.size() > 0) {
            return res;
        } else
            return new ArrayList<>();
    }

    public static List<Map<String, String>> getHfCdpStockLog(Date reportDate)
    {
        String sql = " SELECT female_condoms_offset,male_condoms_offset,issuing_organization,female_condom_brand,male_condom_brand   \n" +
                "                   FROM ec_cdp_stock_log   \n" +
                "                   WHERE (issuing_organization='MSD' OR issuing_organization='PSI' OR issuing_organization='T-MARC' OR  issuing_organization='other')   \n" +
                "                   AND (stock_event_type ='increment' )   \n" +
                "                    AND date(substr(strftime('%Y-%m-%d', datetime(date_updated / 1000, 'unixepoch', 'localtime')), 1, 4) || '-' ||   \n" +
                "                    substr(strftime('%Y-%m-%d', datetime(date_updated / 1000, 'unixepoch', 'localtime')), 6, 2) || '-' || '01') =   \n" +
                "                   date((substr('%s', 1, 4) || '-' || substr('%s', 6, 2) || '-' || '01'))    \n" +
                "                  UNION ALL   \n" +
                "                  SELECT ec_cdp_order_feedback.quantity_response,'0' as  male_condoms_offset,location.name,ec_cdp_order_feedback.condom_brand,'-' as male_condom_brand   \n" +
                "                   FROM task   \n" +
                "                   INNER JOIN location ON location.uuid = task.group_id   \n" +
                "                   INNER JOIN ec_cdp_order_feedback ON  ec_cdp_order_feedback.request_reference = task.reason_reference   \n" +
                "\t\t\t\t   INNER JOIN ec_cdp_orders ON  ec_cdp_orders.form_submission_id = ec_cdp_order_feedback.request_reference\n" +
                "                   WHERE (ec_cdp_order_feedback.condom_type = 'female_condom'  AND task.status = 'COMPLETED' AND request_type='facility_to_facility')   \n" +
                "                    AND date(substr(strftime('%Y-%m-%d', datetime(response_at / 1000, 'unixepoch', 'localtime')), 1, 4) || '-' ||   \n" +
                "                    substr(strftime('%Y-%m-%d', datetime(response_at / 1000, 'unixepoch', 'localtime')), 6, 2) || '-' || '01') =   \n" +
                "                   date((substr('%s', 1, 4) || '-' || substr('%s', 6, 2) || '-' || '01'))    \n" +
                "                  UNION ALL   \n" +
                "                   SELECT '0' as  female_condoms_offset,quantity_response,location.name,'-' as female_condom_brand,ec_cdp_order_feedback.condom_brand   \n" +
                "                   FROM task   \n" +
                "                   INNER JOIN location ON location.uuid = task.group_id   \n" +
                "                   INNER JOIN ec_cdp_order_feedback ON  ec_cdp_order_feedback.request_reference = task.reason_reference   \n" +
                "\t\t\t\t   INNER JOIN ec_cdp_orders ON  ec_cdp_orders.form_submission_id = ec_cdp_order_feedback.request_reference\n" +
                "                   WHERE (ec_cdp_order_feedback.condom_type = 'male_condom'  AND task.status = 'COMPLETED' AND request_type='facility_to_facility')   \n" +
                "                    AND date(substr(strftime('%Y-%m-%d', datetime(response_at / 1000, 'unixepoch', 'localtime')), 1, 4) || '-' ||   \n" +
                "                    substr(strftime('%Y-%m-%d', datetime(response_at / 1000, 'unixepoch', 'localtime')), 6, 2) || '-' || '01') =   \n" +
                "                   date((substr('%s', 1, 4) || '-' || substr('%s', 6, 2) || '-' || '01')) ";

        String queryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(reportDate);

        sql = sql.contains("%s") ? sql.replaceAll("%s", queryDate) : sql;

        DataMap<Map<String, String>> map = cursor -> {
            Map<String, String> data = new HashMap<>();
            data.put("female_condoms_offset", cursor.getString(cursor.getColumnIndex("female_condoms_offset")));
            data.put("male_condoms_offset", cursor.getString(cursor.getColumnIndex("male_condoms_offset")));
            data.put("issuing_organization", cursor.getString(cursor.getColumnIndex("issuing_organization")));
            data.put("male_condom_brand", cursor.getString(cursor.getColumnIndex("male_condom_brand")));
            data.put("female_condom_brand", cursor.getString(cursor.getColumnIndex("female_condom_brand")));

            return data;
        };

        List<Map<String, String>> res = readData(sql, map);


        if (res != null && res.size() > 0) {
            return res;
        } else
            return new ArrayList<>();
    }

    public static List<Map<String, String>> getVmmcRegister(Date reportDate)
    {
        String sql = " WITH VMMC_CTE AS (\n" +
                "    SELECT\n" +
                "        ec_vmmc_enrollment.enrollment_date,\n" +
                "        ec_family_member.first_name,\n" +
                "        ec_family_member.middle_name,\n" +
                "        ec_family_member.last_name,\n" +
                "        ec_vmmc_enrollment.vmmc_client_id,\n" +
                "        ec_vmmc_enrollment.reffered_from,\n" +
                "        ec_vmmc_services.tested_hiv,\n" +
                "        ec_vmmc_services.hiv_result,\n" +
                "        ec_vmmc_services.client_referred_to,\n" +
                "        ec_vmmc_procedure.mc_procedure_date,\n" +
                "        ec_vmmc_procedure.male_circumcision_method,\n" +
                "        ec_vmmc_procedure.intraoperative_adverse_event_occured,\n" +
                "        ec_vmmc_follow_up_visit.visit_number,\n" +
                "        ec_vmmc_follow_up_visit.followup_visit_date AS visit_date,\n" +
                "        ec_vmmc_follow_up_visit.post_op_adverse_event_occur AS post_op_adverse,\n" +
                "        ec_vmmc_notifiable_ae.did_client_experience_nae AS NAE\n" +
                "    FROM\n" +
                "        ec_vmmc_enrollment\n" +
                "            INNER JOIN\n" +
                "        ec_family_member ON ec_family_member.base_entity_id = ec_vmmc_enrollment.base_entity_id\n" +
                "            INNER JOIN\n" +
                "        ec_vmmc_services ON ec_vmmc_services.entity_id = ec_vmmc_enrollment.base_entity_id\n" +
                "            INNER JOIN\n" +
                "        ec_vmmc_procedure ON ec_vmmc_procedure.entity_id = ec_vmmc_enrollment.base_entity_id\n" +
                "            INNER JOIN\n" +
                "        ec_vmmc_follow_up_visit ON ec_vmmc_follow_up_visit.entity_id = ec_vmmc_enrollment.base_entity_id\n" +
                "            LEFT JOIN\n" +
                "        ec_vmmc_notifiable_ae ON ec_vmmc_notifiable_ae.entity_id = ec_vmmc_enrollment.base_entity_id\n" +
                "    WHERE\n" +
                "            ec_vmmc_follow_up_visit.follow_up_visit_type = 'routine'\n" +
                ")\n" +
                "SELECT\n" +
                "    enrollment_date,\n" +
                "    first_name || ' ' || middle_name || ' ' || last_name AS names,\n" +
                "    vmmc_client_id,\n" +
                "    reffered_from,\n" +
                "    tested_hiv,\n" +
                "    hiv_result,\n" +
                "    client_referred_to,\n" +
                "    mc_procedure_date,\n" +
                "    male_circumcision_method,\n" +
                "    intraoperative_adverse_event_occured,\n" +
                "    MAX(CASE WHEN visit_number = 0 THEN visit_date END) AS first_visit,\n" +
                "    MAX(CASE WHEN visit_number = 1 THEN visit_date END) AS sec_visit,\n" +
                "    MAX(post_op_adverse) AS post_op_adverse,\n" +
                "    MAX(NAE) AS NAE\n" +
                "FROM VMMC_CTE\n" +
                "GROUP BY\n" +
                "    first_name || ' ' || middle_name || ' ' || last_name,\n" +
                "    reffered_from,\n" +
                "    tested_hiv,\n" +
                "    hiv_result,\n" +
                "    mc_procedure_date,\n" +
                "    male_circumcision_method;\n ";

        String queryDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(reportDate);

        sql = sql.contains("%s") ? sql.replaceAll("%s", queryDate) : sql;

        DataMap<Map<String, String>> map = cursor -> {
            Map<String, String> data = new HashMap<>();
            data.put("enrollment_date", cursor.getString(cursor.getColumnIndex("enrollment_date")));
            data.put("names", cursor.getString(cursor.getColumnIndex("names")));
            data.put("vmmc_client_id", cursor.getString(cursor.getColumnIndex("vmmc_client_id")));
            data.put("reffered_from", cursor.getString(cursor.getColumnIndex("reffered_from")));
            data.put("tested_hiv", cursor.getString(cursor.getColumnIndex("tested_hiv")));
            data.put("hiv_result", cursor.getString(cursor.getColumnIndex("hiv_result")));
            data.put("client_referred_to", cursor.getString(cursor.getColumnIndex("client_referred_to")));
            data.put("mc_procedure_date", cursor.getString(cursor.getColumnIndex("mc_procedure_date")));
            data.put("male_circumcision_method", cursor.getString(cursor.getColumnIndex("male_circumcision_method")));
            data.put("intraoperative_adverse_event_occured", cursor.getString(cursor.getColumnIndex("intraoperative_adverse_event_occured")));
            data.put("first_visit", cursor.getString(cursor.getColumnIndex("first_visit")));
            data.put("sec_visit", cursor.getString(cursor.getColumnIndex("sec_visit")));
            data.put("post_op_adverse", cursor.getString(cursor.getColumnIndex("post_op_adverse")));
            data.put("NAE", cursor.getString(cursor.getColumnIndex("NAE")));

            return data;
        };

        List<Map<String, String>> res = readData(sql, map);


        if (res != null && res.size() > 0) {
            return res;
        } else
            return new ArrayList<>();
    }


    public static int getReportPerIndicatorCode(String indicatorCode, Date reportDate) {
        String reportDateString = simpleDateFormat.format(reportDate);
        String sql = "SELECT indicator_value\n" +
                "FROM indicator_daily_tally\n" +
                "WHERE indicator_code = '" + indicatorCode + "'\n" +
                "  AND date((substr('" + reportDateString + "', 7, 4) || '-' || substr('" + reportDateString + "', 4, 2) || '-' || '01')) = date((substr(day, 1, 4) || '-' || substr(day, 6, 2) || '-' || '01'))\n" +
                "ORDER BY day DESC LIMIT 1";

        DataMap<Integer> map = cursor -> getCursorIntValue(cursor, "indicator_value");

        List<Integer> res = readData(sql, map);


        if (res != null && res.size() > 0 && res.get(0) != null) {
            return res.get(0);
        } else
            return 0;
    }


    public static String getLastMonthWithTallies(){
        String sql = " SELECT month FROM monthly_tallies ORDER by month DESC LIMIT 1";
        DataMap<String> map = cursor -> getCursorValue(cursor, "month");
        List<String> res = readData(sql, map);
        if (res != null && res.size() > 0 && res.get(0) != null) {
            return res.get(0);
        } else
            return null;
    }

}
