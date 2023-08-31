package org.smartregister.chw.hf.utils;

import android.app.Activity;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.activity.VmmcReferralRegistrationActivity;

public class VmmcReferralFormUtils {

    public static void startVmmcReferral(Activity context, String baseEntityId) {
        JSONObject formJsonObject;
        try {
            formJsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(context, Constants.JsonForm.getVmmcReferralForm());
            if (formJsonObject != null) {
                formJsonObject.put(Constants.REFERRAL_TASK_FOCUS, Constants.FOCUS.REFERRALS);
                VmmcReferralRegistrationActivity.startGeneralReferralFormActivityForResults(context, baseEntityId, formJsonObject, false);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
