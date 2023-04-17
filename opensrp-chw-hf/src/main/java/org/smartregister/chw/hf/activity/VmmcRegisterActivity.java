package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.getVmmcConfirmation;
import android.app.Activity;
import android.content.Intent;
import org.smartregister.chw.core.activity.CoreVmmcRegisterActivity;
import org.smartregister.chw.hf.fragment.VmmcRegisterFragment;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class VmmcRegisterActivity extends CoreVmmcRegisterActivity {
    public static void startVmmcRegistrationActivity(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, VmmcRegisterActivity.class);
        intent.putExtra(org.smartregister.chw.anc.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        intent.putExtra(org.smartregister.chw.vmmc.util.Constants.ACTIVITY_PAYLOAD.VMMC_FORM_NAME, getVmmcConfirmation());
        intent.putExtra(org.smartregister.chw.vmmc.util.Constants.ACTIVITY_PAYLOAD.ACTION, org.smartregister.chw.vmmc.util.Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);

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
