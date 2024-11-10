package org.smartregister.chw.hf.domain.kvp_reports;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class KvpMissedApReportObject extends ReportObject {

    private Date reportDate;

    public KvpMissedApReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }


    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONArray dataArray = new JSONArray();
        List<Map<String, String>> getVmmcRegisterList = ReportDao.getKvpMissedAp(reportDate);

        int i = 0;

        for (Map<String, String> getVmmcRegister : getVmmcRegisterList) {
            JSONObject reportJsonObject = new JSONObject();
            reportJsonObject.put("id", ++i);
            reportJsonObject.put("names", getKvpClientDetails(getVmmcRegister, "names"));
            reportJsonObject.put("uic_id", getKvpClientDetails(getVmmcRegister, "uic_id"));
            reportJsonObject.put("gender", getKvpClientDetails(getVmmcRegister, "gender"));
            reportJsonObject.put("age", getKvpClientDetails(getVmmcRegister, "age"));
            reportJsonObject.put("last_visit_date", getKvpClientDetails(getVmmcRegister, "last_visit_date"));
            reportJsonObject.put("most_recent_appointment_date", getKvpClientDetails(getVmmcRegister, "most_recent_appointment_date"));
            reportJsonObject.put("days_dispenses_last_visit", getKvpClientDetails(getVmmcRegister, "days_dispenses_last_visit"));

            dataArray.put(reportJsonObject);
        }


        JSONObject resultJsonObject = new JSONObject();
        resultJsonObject.put("reportData", dataArray);

        return resultJsonObject;
    }

    private String getKvpClientDetails(Map<String, String> chwRegistrationFollowupClient, String key) {
        String details = chwRegistrationFollowupClient.get(key);
        if (StringUtils.isNotBlank(details)) {
            return details;
        }
        return "-";
    }

}
