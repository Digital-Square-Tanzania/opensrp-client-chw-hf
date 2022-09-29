package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.core.activity.CoreKvpProfileActivity;
import org.smartregister.chw.kvp.util.Constants;

public class PrEPProfileActivity extends CoreKvpProfileActivity {

    public static void startProfile(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, PrEPProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.PrEP_PROFILE);
        activity.startActivity(intent);
    }


    @Override
    public void openFollowupVisit() {
      PrEPVisitActivity.startPrEPVisitActivity(this,memberObject.getBaseEntityId(), false);
    }
}
