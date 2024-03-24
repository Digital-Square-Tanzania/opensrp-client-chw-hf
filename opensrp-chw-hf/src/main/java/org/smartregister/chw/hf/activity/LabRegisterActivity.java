package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreLabRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.hf.domain.JSONObjectHolder;
import org.smartregister.chw.hf.fragment.LabManifestsRegisterFragment;
import org.smartregister.chw.hf.fragment.LabTestRequestsRegisterFragment;
import org.smartregister.chw.hf.model.LabRegisterModel;
import org.smartregister.chw.lab.activity.BaseLabRegisterActivity;
import org.smartregister.chw.lab.interactor.BaseLabRegisterInteractor;
import org.smartregister.chw.lab.presenter.BaseLabRegisterPresenter;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class LabRegisterActivity extends CoreLabRegisterActivity {

    public static void startLabRegisterActivity(Activity activity, String baseEntityID, String formName) {
        Intent intent = new Intent(activity, LabRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.LAB_FORM_NAME, formName);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, FamilyPlanningConstants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        activity.startActivity(intent);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new LabTestRequestsRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{new LabManifestsRegisterFragment(),};
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.LAB);
        }
    }

    @Override
    protected void initializePresenter() {
        presenter = new BaseLabRegisterPresenter(this, new LabRegisterModel(), new BaseLabRegisterInteractor());
    }


    @Override
    public void startFormActivity(JSONObject jsonForm) {
        Intent intent = new Intent(this, HfJsonWizardFormActivity.class);

        // Set the large JSONObject in JSONObjectHolder
        JSONObjectHolder.getInstance().setLargeJSONObject(jsonForm);

        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }

        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }
}
