package org.smartregister.chw.hf.actionhelper.kvp;

import android.content.Context;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.kvp.domain.VisitDetail;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.KvpJsonFormUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

public class KvpOtherServicesActionHelper implements BaseKvpVisitAction.KvpVisitActionHelper {

    private static final String SERVICES = "other_services_provided";
    private static final String REFERRALS = "other_services_referrals_provided";
    private static final String OTHER_TEXT = "others_services_provided";
    private static final String SERVICE_VACCINE = "covid_vaccine";
    private static final String LEGACY_REFERRAL = "vaccine_type";
    private static final String LEGACY_REFERRAL_TEXT = "vaccine";

    private final boolean female;
    private final boolean male;
    private boolean hasLegacyReferral;
    private String legacyServiceText;
    private String legacyReferralText;
    private String initialForm;
    private String processedPayload;
    private boolean complete;

    public KvpOtherServicesActionHelper(String registrationSex) {
        female = Constants.FEMALE.equalsIgnoreCase(StringUtils.trimToEmpty(registrationSex));
        male = Constants.MALE.equalsIgnoreCase(StringUtils.trimToEmpty(registrationSex));
    }

    @Override
    public void onJsonFormLoaded(String jsonPayload, Context context, Map<String, List<VisitDetail>> details) {
        try {
            JSONObject form = new JSONObject(jsonPayload);
            JSONObject services = FormUtils.getFieldFromForm(form, SERVICES);
            JSONObject referrals = FormUtils.getFieldFromForm(form, REFERRALS);
            String legacyVaccinationLabel = form.getString("legacy_vaccination_label");
            form.remove("legacy_vaccination_label");
            if (details != null) {
                restoreLegacyVaccinations(services, referrals, details, legacyVaccinationLabel);
            }
            filterOptions(services);
            filterOptions(referrals);
            initialForm = form.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    private void restoreLegacyVaccinations(JSONObject services, JSONObject referrals,
                                           Map<String, List<VisitDetail>> details, String label) throws JSONException {
        String savedServiceText = savedText(details.get(SERVICE_VACCINE));
        legacyReferralText = savedText(details.get(LEGACY_REFERRAL_TEXT));
        if (StringUtils.isNotBlank(savedServiceText)) {
            JSONArray options = services.getJSONArray("options");
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);
                if (SERVICE_VACCINE.equals(option.getString("key")) && option.optBoolean("value")) {
                    legacyServiceText = savedServiceText;
                    option.put("text", legacyVaccinationLabel(label, legacyServiceText));
                    break;
                }
            }
        }

        List<VisitDetail> savedReferrals = details.get(REFERRALS);
        if (savedReferrals == null) {
            return;
        }
        JSONArray options = referrals.getJSONArray("options");
        JSONObject legacy = codedField(LEGACY_REFERRAL).put("text", "Vaccination (Specify)");
        options.put(legacy);
        // Restore the original generic referral before presenting it under the localized History caption.
        KvpJsonFormUtils.getValue(referrals, savedReferrals);
        hasLegacyReferral = legacy.optBoolean("value");
        if (hasLegacyReferral) {
            legacy.put("text", legacyVaccinationLabel(label, legacyReferralText));
        }
    }

    private static String legacyVaccinationLabel(String label, String detail) {
        return StringUtils.isBlank(detail) ? label : label + " (" + detail + ")";
    }

    private static String savedText(List<VisitDetail> details) {
        return details == null || details.isEmpty() ? null : KvpJsonFormUtils.getValue(details.get(0));
    }

    @Override
    public String getPreProcessed() {
        return initialForm;
    }

    @Override
    public void onPayloadReceived(String jsonPayload) {
        complete = false;
        processedPayload = null;
        if (StringUtils.isBlank(jsonPayload)) {
            return;
        }
        try {
            JSONObject form = new JSONObject(jsonPayload);
            JSONObject serviceChecklist = FormUtils.getFieldFromForm(form, SERVICES);
            JSONObject referralChecklist = FormUtils.getFieldFromForm(form, REFERRALS);
            if (serviceChecklist == null || referralChecklist == null) {
                return;
            }
            Set<String> services = normalizeChecklist(serviceChecklist);
            Set<String> referrals = normalizeChecklist(referralChecklist);
            JSONObject other = FormUtils.getFieldFromForm(form, OTHER_TEXT);
            boolean otherSelected = services.contains("other");
            if (!otherSelected && other != null) {
                other.remove("value");
            }

            JSONArray fields = form.getJSONObject("step1").getJSONArray("fields");
            preserveLegacyText(fields, SERVICE_VACCINE, services.contains(SERVICE_VACCINE) ? legacyServiceText : null);
            preserveLegacyText(fields, LEGACY_REFERRAL_TEXT, referrals.contains(LEGACY_REFERRAL) ? legacyReferralText : null);
            complete = !services.isEmpty() && !referrals.isEmpty()
                    && (!otherSelected || (other != null && !other.isNull("value")
                    && StringUtils.isNotBlank(other.optString("value"))));
            processedPayload = form.toString();
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    private JSONArray filterOptions(JSONObject checklist) throws JSONException {
        JSONArray options = checklist.getJSONArray("options");
        for (int i = options.length() - 1; i >= 0; i--) {
            String key = options.getJSONObject(i).getString("key");
            if ((!female && ("rch".equals(key) || "cecap".equals(key)))
                    || (!male && "vmmc".equals(key))
                    || (!hasLegacyReferral && LEGACY_REFERRAL.equals(key))) {
                options.remove(i);
            }
        }
        return options;
    }

    private Set<String> normalizeChecklist(JSONObject checklist) throws JSONException {
        JSONArray options = filterOptions(checklist);
        Set<String> selected = new LinkedHashSet<>();
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (option.optBoolean("value")) {
                selected.add(option.getString("key"));
            }
        }
        if (selected.contains("none") && selected.size() > 1) {
            // A conflicting saved answer needs a fresh choice in this group.
            selected.clear();
            for (int i = 0; i < options.length(); i++) {
                options.getJSONObject(i).remove("value");
            }
        }
        checklist.put("value", selected.isEmpty() ? null : new JSONArray(selected));
        return selected;
    }

    private static void preserveLegacyText(JSONArray fields, String key, String text) throws JSONException {
        for (int i = fields.length() - 1; i >= 0; i--) {
            if (key.equals(fields.getJSONObject(i).getString("key"))) {
                fields.remove(i);
            }
        }
        if (StringUtils.isNotBlank(text)) {
            fields.put(codedField(key).put("type", "hidden").put("value", text));
        }
    }

    private static JSONObject codedField(String key) throws JSONException {
        return new JSONObject().put("key", key).put("openmrs_entity", "concept")
                .put("openmrs_entity_id", key).put("openmrs_entity_parent", "");
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
    public String postProcess(String jsonPayload) {
        return processedPayload;
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
    public void onPayloadReceived(BaseKvpVisitAction action) {
    }
}
