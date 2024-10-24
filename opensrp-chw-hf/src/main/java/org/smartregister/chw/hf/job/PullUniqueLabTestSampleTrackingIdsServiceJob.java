package org.smartregister.chw.hf.job;

import android.content.Intent;

import androidx.annotation.NonNull;

import org.smartregister.AllConstants;
import org.smartregister.chw.hf.sync.intent.PullUniqueLabTestSampleTrackingIdsIntentService;
import org.smartregister.job.BaseJob;

/**
 * Created by cozej4 on 18/06/2024
 */
public class PullUniqueLabTestSampleTrackingIdsServiceJob extends BaseJob {

    public static final String TAG = "PullUniqueLabTestSampleTrackingIdsServiceJob";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Intent intent = new Intent(getApplicationContext(), PullUniqueLabTestSampleTrackingIdsIntentService.class);
        getApplicationContext().startService(intent);
        return params != null && params.getExtras().getBoolean(AllConstants.INTENT_KEY.TO_RESCHEDULE, false) ? Result.RESCHEDULE : Result.SUCCESS;
    }
}
