package org.smartregister.chw.hf.domain.vmmc_reports;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class VmmcServiceRegisterObject extends ReportObject {

    private Date reportDate;
    private Date startDate;
    private Date endDate;

    public VmmcServiceRegisterObject(Date reportDate) {
        super(reportDate, null, null);
        this.reportDate = reportDate;
    }

    public VmmcServiceRegisterObject(Date reportDate, Date startDate, Date endDate) {
        super(reportDate, startDate, endDate);
        this.reportDate = reportDate;
        this.startDate = startDate;
        this.endDate = endDate;
    }


    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONArray dataArray = new JSONArray();
        List<Map<String, String>> getVmmcRegisterList = ReportDao.getVmmcServiceRegister(reportDate, startDate, endDate);
        int i = 0;

        for (Map<String, String> getVmmcRegister : getVmmcRegisterList) {
            JSONObject reportJsonObject = new JSONObject();
            reportJsonObject.put("id", ++i);

            reportJsonObject.put("enrollment_date", getCbhsClientDetails(getVmmcRegister, "enrollment_date"));
            reportJsonObject.put("names", getCbhsClientDetails(getVmmcRegister, "names"));
            reportJsonObject.put("vmmc_client_id", getCbhsClientDetails(getVmmcRegister, "vmmc_client_id"));
            reportJsonObject.put("age", getCbhsClientDetails(getVmmcRegister, "age"));
            reportJsonObject.put("reffered_from", getCbhsClientDetails(getVmmcRegister, "reffered_from"));
            reportJsonObject.put("tested_hiv", getCbhsClientDetails(getVmmcRegister, "tested_hiv"));

            reportJsonObject.put("hiv_result", getCbhsClientDetails(getVmmcRegister, "hiv_result"));
            reportJsonObject.put("client_referred_to", getCbhsClientDetails(getVmmcRegister, "client_referred_to"));
            reportJsonObject.put("mc_procedure_date", getCbhsClientDetails(getVmmcRegister, "mc_procedure_date"));
            reportJsonObject.put("male_circumcision_method", getCbhsClientDetails(getVmmcRegister, "male_circumcision_method"));
            reportJsonObject.put("intraoperative_adverse_event_occured", getCbhsClientDetails(getVmmcRegister, "intraoperative_adverse_event_occured"));
            reportJsonObject.put("first_visit", getCbhsClientDetails(getVmmcRegister, "first_visit"));
            reportJsonObject.put("sec_visit", getCbhsClientDetails(getVmmcRegister, "sec_visit"));
            reportJsonObject.put("post_op_adverse", getCbhsClientDetails(getVmmcRegister, "post_op_adverse"));
            reportJsonObject.put("post_op_adverse_first_visit", getCbhsClientDetails(getVmmcRegister, "post_op_adverse_first_visit"));
            reportJsonObject.put("post_op_adverse_sec_visit", getCbhsClientDetails(getVmmcRegister, "post_op_adverse_sec_visit"));
//            reportJsonObject.put("NAE", getCbhsClientDetails(getVmmcRegister, "NAE"));
            reportJsonObject.put("health_care_provider", getCbhsClientDetails(getVmmcRegister, "health_care_provider"));
            reportJsonObject.put("mc_procedure_comment", getCbhsClientDetails(getVmmcRegister, "mc_procedure_comment"));


            dataArray.put(reportJsonObject);
        }


        JSONObject resultJsonObject = new JSONObject();
        resultJsonObject.put("reportData", dataArray);

        return resultJsonObject;
    }

    private String getCbhsClientDetails(Map<String, String> chwRegistrationFollowupClient, String key) {
        String details = chwRegistrationFollowupClient.get(key);
        if (StringUtils.isNotBlank(details)) {
            return details;
        }
        return "-";
    }

}
