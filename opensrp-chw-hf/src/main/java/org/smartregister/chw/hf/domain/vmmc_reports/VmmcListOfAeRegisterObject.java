package org.smartregister.chw.hf.domain.vmmc_reports;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class VmmcListOfAeRegisterObject extends ReportObject {

    private Date reportDate;
    private Date startDate;
    private Date endDate;

    public VmmcListOfAeRegisterObject(Date reportDate) {
        super(reportDate, null, null);
        this.reportDate = reportDate;
    }

    public VmmcListOfAeRegisterObject(Date reportDate, Date startDate, Date endDate) {
        super(reportDate, startDate, endDate);
        this.reportDate = reportDate;
        this.startDate = startDate;
        this.endDate = endDate;
    }


    @Override
    public JSONObject getIndicatorData() throws JSONException {
        JSONArray dataArray = new JSONArray();
        List<Map<String, String>> getVmmcRegisterList = ReportDao.getVmmcListOfAeRegister(reportDate, startDate, endDate);
        int i = 0;

        for (Map<String, String> getVmmcRegister : getVmmcRegisterList) {
            JSONObject reportJsonObject = new JSONObject();
            reportJsonObject.put("id", ++i);
            reportJsonObject.put("vmmc_client_id", getCbhsClientDetails(getVmmcRegister, "vmmc_client_id"));
            reportJsonObject.put("names", getCbhsClientDetails(getVmmcRegister, "names"));
            reportJsonObject.put("age", getCbhsClientDetails(getVmmcRegister, "age"));
            reportJsonObject.put("type_of_adverse_event", getCbhsClientDetails(getVmmcRegister, "type_of_adverse_event"));
            reportJsonObject.put("mc_procedure_date", getCbhsClientDetails(getVmmcRegister, "mc_procedure_date"));
            reportJsonObject.put("date_nae_occured", getCbhsClientDetails(getVmmcRegister, "date_nae_occured"));
            reportJsonObject.put("male_circumcision_method", getCbhsClientDetails(getVmmcRegister, "male_circumcision_method"));

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
