package org.smartregister.chw.hf.actionhelper.vmmc;

import android.content.Context;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hf.dao.HfVmmcDao;
import org.smartregister.chw.hf.utils.Constants;
import org.smartregister.chw.referral.util.JsonFormConstants;
import org.smartregister.chw.vmmc.domain.VisitDetail;
import org.smartregister.chw.vmmc.model.BaseVmmcVisitAction;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VmmcFollowUpActionHelper implements BaseVmmcVisitAction.VmmcVisitActionHelper {

    public Integer noOfDayPostOP;
    protected String visit_type;
    protected String notifiable_adverse_event_occured;
    protected String follow_up_visit_type;
    protected String jsonPayload;
    protected String baseEntityId;

    protected int followupVisit;



    public VmmcFollowUpActionHelper(String baseEntityId) {
        this.baseEntityId = baseEntityId;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject global = jsonObject.getJSONObject("global");

            String method_used = HfVmmcDao.getMcMethodUsed(baseEntityId);
            global.put("method_used", method_used);

            global.put("current_visit_number", HfVmmcDao.getFollowUpVisitNumber(baseEntityId));

            LocalDate todayDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormat.forPattern("dd-MM-yyyy");

            String discharge_date = HfVmmcDao.getDischargingDate(baseEntityId);

            LocalDate lastFollowUpDate = null;

            if (!Objects.equals(discharge_date, "")){
                LocalDate dischargingDateFormat = formatter.parseDateTime(discharge_date).toLocalDate();
                lastFollowUpDate = dischargingDateFormat.plusDays(7);
            }

            global.put("last_follow_up_date", lastFollowUpDate);
            global.put("today_date", todayDate);


            String male_circumcision_date = HfVmmcDao.getMcDoneDate(baseEntityId);
            LocalDate mcProcedureDate = formatter.parseDateTime(male_circumcision_date).toLocalDate();

            noOfDayPostOP = dayDifference(mcProcedureDate, todayDate);

            JSONArray fields = jsonObject.getJSONObject(Constants.JsonFormConstants.STEP1).getJSONArray(JsonFormConstants.FIELDS);
            JSONObject post_op_dates = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "post_op_dates");
            post_op_dates.put("text", "Day(s) Post-OP: " + noOfDayPostOP.toString());

            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    private int dayDifference(LocalDate date1, LocalDate date2) {
        return Days.daysBetween(date1, date2).getDays();
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            follow_up_visit_type = CoreJsonFormUtils.getValue(jsonObject, "follow_up_visit_type");
            notifiable_adverse_event_occured = CoreJsonFormUtils.getValue(jsonObject, "notifiable_adverse_event_occured");

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BaseVmmcVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String s) {
        return s;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseVmmcVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(follow_up_visit_type))
            return BaseVmmcVisitAction.Status.PENDING;
        else {
            return BaseVmmcVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseVmmcVisitAction baseVmmcVisitAction) {
        //overridden
    }
}
