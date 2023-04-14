package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getMalariaConfirmation;
import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getVmmcConfirmation;
import static org.smartregister.chw.hf.utils.Constants.HIV_STATUS.POSITIVE;
import static org.smartregister.chw.hf.utils.Constants.JsonForm.getPmtctRegistration;
import static org.smartregister.chw.hf.utils.Constants.JsonForm.getPmtctRegistrationForClientsKnownOnArtForm;
import static org.smartregister.chw.pmtct.util.Constants.EVENT_TYPE.PMTCT_REGISTRATION;
import static org.smartregister.util.JsonFormUtils.FIELDS;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreMalariaRegisterActivity;
import org.smartregister.chw.core.activity.CorePmtctRegisterActivity;
import org.smartregister.chw.core.activity.CoreVmmcRegisterActivity;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.custom_view.FacilityMenu;
import org.smartregister.chw.hf.fragment.MalariaRegisterFragment;
import org.smartregister.chw.hf.fragment.PmtctRegisterFragment;
import org.smartregister.chw.hf.fragment.VmmcRegisterFragment;
import org.smartregister.chw.hf.listener.HfFamilyBottomNavListener;
import org.smartregister.chw.hf.presenter.PmtctRegisterPresenter;
import org.smartregister.chw.hf.presenter.VmmcRegisterPresenter;
import org.smartregister.chw.hf.utils.Constants;
import org.smartregister.chw.hf.utils.PncVisitUtils;
import org.smartregister.chw.pmtct.interactor.BasePmtctRegisterInteractor;
import org.smartregister.chw.pmtct.model.BasePmtctRegisterModel;
import org.smartregister.chw.vmmc.fragment.BaseVmmcRegisterFragment;
import org.smartregister.chw.vmmc.interactor.BaseVmmcRegisterInteractor;
import org.smartregister.chw.vmmc.model.BaseVmmcRegisterModel;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class VmmcRegisterActivity extends CoreVmmcRegisterActivity {
    public static void startVmmcRegistrationActivity(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, VmmcRegisterActivity.class);
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(org.smartregister.chw.vmmc.util.Constants.ACTIVITY_PAYLOAD.VMMC_FORM_NAME, getVmmcConfirmation());
        intent.putExtra(org.smartregister.chw.vmmc.util.Constants.ACTIVITY_PAYLOAD.ACTION, org.smartregister.chw.vmmc.util.Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);

//        Intent intent = new Intent(activity, MalariaRegisterActivity.class);
//        intent.putExtra(org.smartregister.chw.anc.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
//        intent.putExtra(org.smartregister.chw.malaria.util.Constants.ACTIVITY_PAYLOAD.MALARIA_FORM_NAME, getMalariaConfirmation());
//        intent.putExtra(org.smartregister.chw.anc.util.Constants.ACTIVITY_PAYLOAD.ACTION, org.smartregister.chw.anc.util.Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);

        activity.startActivity(intent);
    }

    @Override
    protected void registerBottomNavigation() {

        bottomNavigationHelper = new BottomNavigationHelper();
        bottomNavigationView = findViewById(org.smartregister.R.id.bottom_navigation);
        FamilyRegisterActivity.registerBottomNavigation(bottomNavigationHelper, bottomNavigationView, this);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new VmmcRegisterFragment();
    }
}
