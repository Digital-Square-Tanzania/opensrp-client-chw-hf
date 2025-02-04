package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.hts.activity.BaseHtsVisitActivity;
import org.smartregister.chw.hts.interactor.BaseHtsServiceVisitInteractor;
import org.smartregister.chw.hts.presenter.BaseHtsVisitPresenter;
import org.smartregister.chw.hts.util.Constants;
import org.smartregister.family.util.Utils;

public class HtsVisitActivity extends BaseHtsVisitActivity {
    public static void startMe(Activity activity, String baseEntityID, String profileType, Boolean isEditMode) {
        Intent intent = new Intent(activity, HtsVisitActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, isEditMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, profileType);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void registerPresenter() {
        presenter = new BaseHtsVisitPresenter(memberObject, this, new BaseHtsServiceVisitInteractor(Constants.EVENT_TYPE.HTS_SERVICES));
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
