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
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.dao.HfKvpDao;
import org.smartregister.chw.hf.utils.MarpEnrollmentDate;
import org.smartregister.chw.hf.utils.MarpPregnancyStatus;
import org.smartregister.chw.hf.utils.MarpVisitStatus;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class KvpClientStatusActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    public static final String OTHER_CATEGORY = "other_kvp_category";

    protected MemberObject memberObject;
    private String jsonPayload;
    private boolean editingVisit;
    private boolean firstClientStatus;
    private final boolean requiresPregnancyStatus;
    private Context context;
    private String savedPregnancyStatus = "";
    private boolean complete;

    public KvpClientStatusActionHelper(MemberObject memberObject) {
        this(memberObject, false);
    }

    public KvpClientStatusActionHelper(MemberObject memberObject, boolean requiresPregnancyStatus) {
        this.memberObject = memberObject;
        this.requiresPregnancyStatus = requiresPregnancyStatus;
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> map) {
        this.jsonPayload = jsonPayload;
        this.context = context;
        editingVisit = map != null;
        savedPregnancyStatus = "";
    }

    @Override
    public String getPreProcessed() {
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            JSONObject global = jsonObject.getJSONObject("global");
            JSONArray fields = jsonObject.getJSONObject(STEP1).getJSONArray(FIELDS);
            JSONObject pregnancyStatus = configurePregnancyStatus(fields);

            // The visit action restores saved selections after preprocessing, including an empty selection.
            if (!editingVisit) {
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
            if (pregnancyStatus != null && firstClientStatus && !editingVisit && !hasEarlierMarpVisit()) {
                String screeningStatus = getScreeningPregnancyStatus();
                if (MarpPregnancyStatus.isAnswer(screeningStatus)) {
                    pregnancyStatus.put(VALUE, screeningStatus);
                }
            }

            return jsonObject.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }

        return null;
    }

    private JSONObject configurePregnancyStatus(JSONArray fields) throws JSONException {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject field = fields.getJSONObject(i);
            if (!MarpPregnancyStatus.FIELD.equals(field.optString("key"))) {
                continue;
            }
            if (requiresPregnancyStatus) {
                field.put("label", context.getString(R.string.marp_pregnancy_breastfeeding_status));
                field.getJSONObject("v_required").put("err", context.getString(R.string.marp_visit_type_required));
                JSONArray options = field.getJSONArray(OPTIONS);
                org.smartregister.util.JsonFormUtils.getFieldJSONObject(options, MarpPregnancyStatus.PREGNANT)
                        .put("text", context.getString(R.string.marp_status_pregnant));
                org.smartregister.util.JsonFormUtils.getFieldJSONObject(options, MarpPregnancyStatus.NEITHER)
                        .put("text", context.getString(R.string.marp_status_neither));
                org.smartregister.util.JsonFormUtils.getFieldJSONObject(options, MarpPregnancyStatus.BREASTFEEDING)
                        .put("text", context.getString(R.string.marp_status_breastfeeding));
                return field;
            }
            fields.remove(i);
            break;
        }
        return null;
    }

    protected String getScreeningDefaults() {
        return HfKvpDao.getOtherKvpClientGroups(memberObject.getBaseEntityId());
    }

    protected boolean hasPreviousClientStatus() {
        return StringUtils.isNotBlank(HfKvpDao.getClientStatus(memberObject.getBaseEntityId()));
    }

    protected String getScreeningPregnancyStatus() {
        return HfKvpDao.getScreeningPregnancyStatus(memberObject.getBaseEntityId());
    }

    protected boolean hasEarlierMarpVisit() {
        return MarpVisitStatus.hasSubmittedVisitBeforeToday(KvpLibrary.getInstance().visitRepository(),
                memberObject.getBaseEntityId(), Calendar.getInstance());
    }

    public String getPregnancyBreastfeedingStatus() {
        if (!requiresPregnancyStatus) {
            return "";
        }
        return MarpPregnancyStatus.isAnswer(savedPregnancyStatus) ? savedPregnancyStatus : getLatestPregnancyStatus();
    }

    protected String getLatestPregnancyStatus() {
        return HfKvpDao.getLatestPregnancyBreastfeedingStatus(memberObject.getBaseEntityId());
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        complete = false;
        savedPregnancyStatus = "";
        if (StringUtils.isBlank(jsonPayload)) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(jsonPayload);
            String clientStatus = CoreJsonFormUtils.getValue(jsonObject, CLIENT_STATUS);
            String pregnancyStatus = CoreJsonFormUtils.getValue(jsonObject, MarpPregnancyStatus.FIELD);
            complete = StringUtils.isNotBlank(clientStatus)
                    && (!requiresEnrollmentDate(clientStatus)
                    || MarpEnrollmentDate.isValid(CoreJsonFormUtils.getValue(jsonObject, ENROLLMENT_DATE)))
                    && (!requiresPregnancyStatus || MarpPregnancyStatus.isAnswer(pregnancyStatus));
            if (complete && requiresPregnancyStatus) {
                savedPregnancyStatus = pregnancyStatus;
            }
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
