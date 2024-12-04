package org.smartregister.chw.hf.actionhelper.prep;

import static com.vijay.jsonwizard.constants.JsonFormConstants.TYPE;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;
import static org.smartregister.family.util.JsonFormUtils.STEP2;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hf.dao.HfKvpDao;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.family.util.JsonFormUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class PrEPInitiationActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private String prepVisitStatus;
    private String prep_status;
    private String jsonPayload;
    private String baseEntityId;

    public PrEPInitiationActionHelper(String baseEntityId, String prepVisitStatus) {
        this.baseEntityId = baseEntityId;
        this.prepVisitStatus = prepVisitStatus;
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

            try {
                String prepStatus = HfKvpDao.getPrepStatus(baseEntityId);
                if (prepStatus != null) {
                    global.put("prep_status", prepStatus);
                } else {
                    global.put("prep_status", "");
                }
            } catch (Exception e) {
                global.put("prep_status", "");
            }

            JSONArray fields = jsonObject.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1).getJSONArray(org.smartregister.chw.referral.util.JsonFormConstants.FIELDS);

            global.put("sex", KvpDao.getPrEPMember(baseEntityId).getGender());

            try {
                JSONObject prepStatusNotDiscontinuedObject = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "prep_status_not_discontinued");
                if (prepVisitStatus != null && prepVisitStatus.equalsIgnoreCase("new_client")) {
                    prepStatusNotDiscontinuedObject.getJSONArray("options").remove(4);
                    prepStatusNotDiscontinuedObject.getJSONArray("options").remove(2);
                    prepStatusNotDiscontinuedObject.getJSONArray("options").remove(1);
                } else if (prepVisitStatus != null && prepVisitStatus.equalsIgnoreCase("returning_client")) {
                    prepStatusNotDiscontinuedObject.getJSONArray("options").remove(3);
                    prepStatusNotDiscontinuedObject.getJSONArray("options").remove(0);
                } else if (prepVisitStatus != null && prepVisitStatus.equalsIgnoreCase("transfer_in")) {
                    prepStatusNotDiscontinuedObject.getJSONArray("options").remove(4);
                    prepStatusNotDiscontinuedObject.getJSONArray("options").remove(3);
                    prepStatusNotDiscontinuedObject.getJSONArray("options").remove(0);
                }
            } catch (Exception e) {
                Timber.e(e);
            }

            String prepInitiationDate = HfKvpDao.getPrepInitiationDate(baseEntityId);

            if (StringUtils.isNotBlank(prepInitiationDate) && !prepInitiationDate.equalsIgnoreCase("-")) {
                JSONObject original_prep_initiation_date_for_continuing_clients = JsonFormUtils.getFieldJSONObject(fields, "original_prep_initiation_date_for_continuing_clients");
                original_prep_initiation_date_for_continuing_clients.put(TYPE, "hidden");
                original_prep_initiation_date_for_continuing_clients.put(VALUE, prepInitiationDate);
            }

            return jsonObject.toString();
        } catch (Exception e) {
            Timber.e(e);
        }

        return null;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            prep_status = CoreJsonFormUtils.getValue(jsonObject, "prep_status");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BaseKvpVisitAction.ScheduleStatus getPreProcessedStatus() {
        return null;
    }

    @Override
    public String getPreProcessedSubTitle() {
        return null;
    }

    @Override
    public String postProcess(String s) {
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseKvpVisitAction.Status evaluateStatusOnPayload() {
        if (StringUtils.isBlank(prep_status))
            return BaseKvpVisitAction.Status.PENDING;
        else {
            return BaseKvpVisitAction.Status.COMPLETED;
        }
    }

    @Override
    public void onPayloadReceived(BaseKvpVisitAction baseKvpVisitAction) {
        //overridden
    }
}
