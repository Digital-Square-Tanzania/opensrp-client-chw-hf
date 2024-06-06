package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1;
import static org.smartregister.client.utils.constants.JsonFormConstants.MIN_DATE;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.FIELDS;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.VALUE;

import android.app.Activity;
import android.content.Intent;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.domain.JSONObjectHolder;
import org.smartregister.chw.lab.activity.BaseManifestDetailsActivity;
import org.smartregister.chw.lab.dao.LabDao;
import org.smartregister.chw.lab.domain.Manifest;
import org.smartregister.chw.lab.domain.TestSample;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.chw.lab.util.LabJsonFormUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import timber.log.Timber;

public class LabManifestDetailsActivity extends BaseManifestDetailsActivity {
    public static void startProfileActivity(Activity activity, String batchNumber) {
        Intent intent = new Intent(activity, LabManifestDetailsActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BATCH_NUMBER, batchNumber);
        activity.startActivity(intent);
    }

    @Override
    public void dispatchManifest() {
        try {
            JSONObject sampleProcessingJson = FormUtils.getFormUtils().getFormJson(Constants.FORMS.LAB_MANIFEST_DISPATCH);
            String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
            LabJsonFormUtils.getRegistrationForm(sampleProcessingJson, UUID.randomUUID().toString(), locationId);


            JSONArray fields = sampleProcessingJson.getJSONObject(STEP1).getJSONArray(FIELDS);
            JSONObject batchNumberObj = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "batch_number");
            batchNumberObj.put(VALUE, batchNumber);


            String maxDate = getMaxDate(manifest);
            if(StringUtils.isNotBlank(maxDate)){
                JSONObject maxSampleDate = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "max_sample_date");
                maxSampleDate.put(VALUE, maxDate.split(" ")[0]);

                JSONObject dispatchDate = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "dispatch_date");
                dispatchDate.put(MIN_DATE, maxDate.split(" ")[0]);

                JSONObject maxSampleTime = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "max_sample_time");
                maxSampleTime.put(VALUE, maxDate.split(" ")[1]);
            }

            startFormActivity(sampleProcessingJson);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = new Intent(this, HfJsonWizardFormActivity.class);

        // Set the large JSONObject in JSONObjectHolder
        JSONObjectHolder.getInstance().setLargeJSONObject(jsonForm);

        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    private String getMaxDate(Manifest manifest) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());

        String maxDate = "";
        List<String> sampleList = convertToList(manifest.getSampleList());
        for (String sampleId : sampleList) {
            TestSample testSample = LabDao.getTestSamplesRequestsBySampleId(sampleId).get(0);
            if (StringUtils.isNotBlank(testSample.getSampleSeparationDate()) && StringUtils.isNotBlank(testSample.getSampleSeparationTime())) {
                if (maxDate.isEmpty() || sdf.parse(testSample.getSampleSeparationDate() + " " + testSample.getSampleSeparationTime()).after(sdf.parse(maxDate))) {
                    maxDate = testSample.getSampleSeparationDate() + " " + testSample.getSampleSeparationTime();
                }
            } else if (StringUtils.isNotBlank(testSample.getSampleCollectionDate()) && StringUtils.isNotBlank(testSample.getSampleCollectionTime())) {
                if (maxDate.isEmpty() || sdf.parse(testSample.getSampleCollectionDate() + " " + testSample.getSampleCollectionTime()).after(sdf.parse(maxDate))) {
                    maxDate = testSample.getSampleCollectionDate() + " " + testSample.getSampleCollectionTime();
                }
            }
        }
        return maxDate;
    }
}
