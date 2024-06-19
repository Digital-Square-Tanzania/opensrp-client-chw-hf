package org.smartregister.chw.hf.activity;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static com.vijay.jsonwizard.constants.JsonFormConstants.STEP1;
import static com.vijay.jsonwizard.constants.JsonFormConstants.VALUE;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.vijay.jsonwizard.constants.JsonFormConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreLabRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.hf.domain.JSONObjectHolder;
import org.smartregister.chw.hf.fragment.LabManifestsRegisterFragment;
import org.smartregister.chw.hf.fragment.LabTestRequestsRegisterFragment;
import org.smartregister.chw.hf.model.LabRegisterModel;
import org.smartregister.chw.hf.repository.UniqueLabTestSampleTrackingIdRepository;
import org.smartregister.chw.lab.LabLibrary;
import org.smartregister.chw.lab.activity.BaseLabRegisterActivity;
import org.smartregister.chw.lab.interactor.BaseLabRegisterInteractor;
import org.smartregister.chw.lab.pojo.RegisterParams;
import org.smartregister.chw.lab.presenter.BaseLabRegisterPresenter;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.chw.lab.util.LabJsonFormUtils;
import org.smartregister.domain.Location;
import org.smartregister.repository.LocationRepository;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_GET_JSON && resultCode == RESULT_OK) {
            String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                String encounter_type = jsonObject.getString("encounter_type");
                if (encounter_type.equalsIgnoreCase(Constants.EVENT_TYPE.LAB_HVL_SAMPLE_COLLECTION)) {
                    JSONArray fields = jsonObject.getJSONObject(org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1)
                            .getJSONArray(org.smartregister.chw.referral.util.JsonFormConstants.FIELDS);

                    JSONObject sampleRequestSampleId = org.smartregister.family.util.JsonFormUtils.getFieldJSONObject(fields, "sample_id");
                    if (sampleRequestSampleId != null) {
                        String obtainedSampleTrackingId = sampleRequestSampleId.getString(JsonFormConstants.VALUE);

                        String extractedSampleTrackingId = obtainedSampleTrackingId.substring(8, 15);
                        new UniqueLabTestSampleTrackingIdRepository().close(extractedSampleTrackingId);
                    }
                }
            } catch (JSONException e) {
                Timber.e(e);
            }
        }
    }
}
