package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.cecap.activity.BaseCecapVisitActivity;
import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.family.util.Utils;

public class CecapVisitActivity extends BaseCecapVisitActivity {
    public static void startMe(Activity activity, String baseEntityID, Boolean isEditMode,  boolean isViaFollowupTest) {
        Intent intent = new Intent(activity, CecapVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, isEditMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.IS_VIA_FOLLOWUP_TEST, isViaFollowupTest);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }


    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }

        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }
}