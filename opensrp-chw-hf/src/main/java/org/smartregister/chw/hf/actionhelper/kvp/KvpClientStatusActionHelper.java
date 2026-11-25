package org.smartregister.chw.hf.actionhelper.kvp;

import static org.smartregister.AllConstants.OPTIONS;
import static org.smartregister.client.utils.constants.JsonFormConstants.FIELDS;
import static org.smartregister.client.utils.constants.JsonFormConstants.KEY;
import static org.smartregister.client.utils.constants.JsonFormConstants.STEP1;
import static org.smartregister.client.utils.constants.JsonFormConstants.VALUE;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.CLIENT_STATUS;
import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.ENROLLMENT_DATE;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hf.dao.HfKvpDao;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;

import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class KvpClientStatusActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    public static final String OTHER_CATEGORY = "other_kvp_category";

    protected MemberObject memberObject;
    private String jsonPayload;
    private boolean editingVisit;
    private boolean firstClientStatus;
    private boolean complete;

    public KvpClientStatusActionHelper(MemberObject memberObject) {
        this.memberObject = memberObject;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
        editingVisit = map != null;
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject global = jsonObject.getJSONObject("global");

            // The visit action restores saved selections after preprocessing, including an empty selection.
            if (!editingVisit) {
                JSONArray fields = jsonObject.getJSONObject(STEP1).getJSONArray(FIELDS);
                JSONObject category = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, OTHER_CATEGORY);
                JSONArray options = category.getJSONArray(OPTIONS);
                String otherScreenedGroups = getScreeningDefaults();
                for (int i = 0; i < options.length(); i++) {
                    JSONObject option = options.getJSONObject(i);
                    if (otherScreenedGroups.contains(option.getString(KEY))) {
                        option.put(VALUE, true);
                    }
                }
            }

            firstClientStatus = !hasPreviousClientStatus();
            global.put("first_visit_done", firstClientStatus ? "no" : "yes");

            return jsonObject.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }

        return null;
    }

    protected String getScreeningDefaults() {
        return HfKvpDao.getOtherKvpClientGroups(memberObject.getBaseEntityId());
    }

    protected boolean hasPreviousClientStatus() {
        return StringUtils.isNotBlank(HfKvpDao.getClientStatus(memberObject.getBaseEntityId()));
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        complete = false;
        if (StringUtils.isBlank(jsonPayload)) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            String clientStatus = CoreJsonFormUtils.getValue(jsonObject, CLIENT_STATUS);
            complete = StringUtils.isNotBlank(clientStatus)
                    && (!requiresEnrollmentDate(clientStatus)
                    || MarpEnrollmentDate.isValid(CoreJsonFormUtils.getValue(jsonObject, ENROLLMENT_DATE)));
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    private boolean requiresEnrollmentDate(String clientStatus) {
        return firstClientStatus && MarpEnrollmentDate.isTransferOrReturn(clientStatus);
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
    public String postProcess(String payload) {
        if (StringUtils.isBlank(payload)) {
            return null;
        }
        try {
            JSONObject form = new JSONObject(payload);
            if (!requiresEnrollmentDate(CoreJsonFormUtils.getValue(form, CLIENT_STATUS))) {
                // Hidden widgets can retain answers after a user changes Visit Type.
                JSONArray fields = form.getJSONObject(STEP1).getJSONArray(FIELDS);
                JSONObject date = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, ENROLLMENT_DATE);
                if (date != null && date.has(VALUE)) {
                    date.remove(VALUE);
                    return form.toString();
                }
            }
        } catch (JSONException e) {
            Timber.e(e);
        }
        return null;
    }

    @Override
    public String evaluateSubTitle() {
        return null;
    }

    @Override
    public BaseKvpVisitAction.Status evaluateStatusOnPayload() {
        return complete ? BaseKvpVisitAction.Status.COMPLETED : BaseKvpVisitAction.Status.PENDING;
    }

    @Override
    public void onPayloadReceived(BaseKvpVisitAction baseKvpVisitAction) {
        //overridden
    }
}
