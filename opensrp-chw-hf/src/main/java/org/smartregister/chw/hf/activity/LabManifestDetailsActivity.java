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
import org.smartregister.chw.hf.domain.JSONObjectHolder;
import org.smartregister.chw.lab.activity.BaseLabTestRequestDetailsActivity;
import org.smartregister.chw.lab.activity.BaseManifestDetailsActivity;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.chw.lab.util.LabJsonFormUtils;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

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
            batchNumberObj.put(VALUE,batchNumber);

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

}
