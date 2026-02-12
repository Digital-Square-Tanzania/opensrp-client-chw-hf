package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.MenuRes;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.activity.CoreAypFacilityServicesRegisterActivity;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.fragment.AypFacilityServicesRegisterFragment;
import org.smartregister.family.util.Utils;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class AypFacilityServicesRegisterActivity extends CoreAypFacilityServicesRegisterActivity {

    public static void startRegistration(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, AypFacilityServicesRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.AYP_FORM_NAME, Constants.FORMS.AYP_FACILITY_SCREENING);
        activity.startActivity(intent);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new AypFacilityServicesRegisterFragment();
    }

    @Override
    @MenuRes
    public int getMenuResource() {
        return R.menu.bottom_nav_ayp_facility_services;
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }
}
