package org.smartregister.chw.hf.domain;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

public abstract class ReportObject {

    private List<String> indicatorCodes;
    private final Date reportDate;
    private final Date startDate;
    private final Date endDate;

    public ReportObject(Date reportDate) {
        this.reportDate = reportDate;
        startDate = null;
        endDate = null;
    }

    public ReportObject(Date reportDate, Date startDate, Date endDate) {
        this.reportDate = reportDate;
        this.startDate = startDate;
        this.endDate = endDate;
    }


    public List<String> getIndicatorCodes() {
        return indicatorCodes;
    }

    public Date getReportDate() {
        return reportDate;
    }

    public Date getReportStartDate() {
        return startDate;
    }

    public Date getReportEndDate() {
        return endDate;
    }

    public JSONObject getIndicatorData() throws JSONException {
        return new JSONObject();
    }

    public String getIndicatorDataAsGson(JSONObject jsonObject) throws JSONException {
        Gson gson = new Gson();
        return gson.toJson(jsonObject);
    }
}
