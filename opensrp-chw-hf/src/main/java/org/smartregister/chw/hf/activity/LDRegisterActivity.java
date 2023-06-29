package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.hf.utils.Constants.JsonForm.getLdRegistration;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreLDRegisterActivity;
import org.smartregister.chw.hf.fragment.LDDischargedRegisterFragment;
import org.smartregister.chw.hf.fragment.LDRegisterFragment;
import org.smartregister.chw.hf.presenter.LDRegisterPresenter;
import org.smartregister.chw.ld.interactor.BaseLDRegisterInteractor;
import org.smartregister.chw.ld.model.BaseLDRegisterModel;
import org.smartregister.chw.ld.util.Constants;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class LDRegisterActivity extends CoreLDRegisterActivity {
    private long mLastExecutionTime = 0;
    private static final long MINIMUM_INTERVAL_MS = 3000;
    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new LDRegisterFragment();
    }

    public static void startLDRegistrationActivity(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, LDRegisterActivity.class);
        intent.putExtra(org.smartregister.chw.ld.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.LD_FORM_NAME, getLdRegistration());
        intent.putExtra(org.smartregister.chw.ld.util.Constants.ACTIVITY_PAYLOAD.ACTION, org.smartregister.chw.ld.util.Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        activity.startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Closing the register activity to return to profile activity if the launched form was Labour and Delivery Labour Stage Form
        if (ACTION.equals(LDProfileActivity.LD_PROFILE_ACTION))
            finish();
    }

    @Override
    protected void initializePresenter() {
        presenter = new LDRegisterPresenter(this, new BaseLDRegisterModel(), new BaseLDRegisterInteractor());
    }

    @Override
    protected Fragment[] getOtherFragments() {
        Fragment[] otherFragments = new Fragment[1];
        otherFragments[0] = new LDDischargedRegisterFragment();
        return otherFragments;
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        //Necessary evil to disable multiple sequential clicks of actions that do sometimes cause app crushes
        long currentTime = System.currentTimeMillis();
        if (currentTime - mLastExecutionTime < MINIMUM_INTERVAL_MS) {
            // too soon to execute the function again, ignore this call
            return;
        }

        // record the current time as the last execution time
        mLastExecutionTime = currentTime;
        super.startFormActivity(jsonForm);
    }
}
