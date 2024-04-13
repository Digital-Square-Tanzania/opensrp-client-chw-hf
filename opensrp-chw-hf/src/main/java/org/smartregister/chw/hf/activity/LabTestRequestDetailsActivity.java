package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.core.utils.CoreJsonFormUtils.TITLE;
import static org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.FIELDS;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.VALUE;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.lab.activity.BaseLabTestRequestDetailsActivity;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.chw.lab.util.LabJsonFormUtils;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import timber.log.Timber;

public class LabTestRequestDetailsActivity extends BaseLabTestRequestDetailsActivity {
    public static void startProfileActivity(Activity activity, String baseEntityId, String testSampleId) {
        Intent intent = new Intent(activity, LabTestRequestDetailsActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.TEST_SAMPLE_ID, testSampleId);
        activity.startActivity(intent);
    }

    @Override
    public void recordSampleRequestProcessing(String baseEntityId, String testRequestSampleId) {
        try {
            JSONObject sampleProcessingJson = FormUtils.getFormUtils().getFormJson(Constants.FORMS.LAB_HVL_PROCESSING);
            String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
            LabJsonFormUtils.getRegistrationForm(sampleProcessingJson, baseEntityId, locationId);


            JSONArray fields = sampleProcessingJson.getJSONObject(STEP1).getJSONArray(FIELDS);
            JSONObject sampleId = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "sample_id");
            sampleId.put(VALUE, testRequestSampleId);

            startFormActivity(sampleProcessingJson);
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    @Override
    public void recordSampleRequestResults(String baseEntityId, String testRequestSampleId) {
        try {
            JSONObject sampleResultsJson;
            if (testSample.getSampleType().equalsIgnoreCase("hvl")) {
                sampleResultsJson = FormUtils.getFormUtils().getFormJson(Constants.FORMS.LAB_HVL_RESULTS);
            } else {
                sampleResultsJson = FormUtils.getFormUtils().getFormJson(Constants.FORMS.LAB_HEID_RESULTS);
            }

            String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
            LabJsonFormUtils.getRegistrationForm(sampleResultsJson, baseEntityId, locationId);

            JSONArray fields = sampleResultsJson.getJSONObject(STEP1).getJSONArray(FIELDS);
            JSONObject sampleId = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "sample_id");
            sampleId.put(VALUE, testRequestSampleId);

            startFormActivity(sampleResultsJson);
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        try {
            Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
            intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

            Form form = new Form();
            form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
            form.setWizard(false);
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

            form.setWizard(true);
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
            form.setName(jsonForm.getJSONObject(STEP1).getString(TITLE));
            form.setNextLabel(this.getResources().getString(org.smartregister.chw.core.R.string.next));
            form.setPreviousLabel(this.getResources().getString(org.smartregister.chw.core.R.string.back));
            form.setSaveLabel(this.getResources().getString(org.smartregister.chw.core.R.string.save));


            startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}
