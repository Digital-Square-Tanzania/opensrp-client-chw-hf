package org.smartregister.chw.hf.activity;

import static org.smartregister.util.Utils.getAllSharedPreferences;

import android.app.Activity;
import android.content.Intent;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;

import org.smartregister.chw.core.activity.CoreLabRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.fragment.LabManifestsRegisterFragment;
import org.smartregister.chw.hf.fragment.LabTestRequestsRegisterFragment;
import org.smartregister.chw.hf.listener.CdpBottomNavigationListener;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.helper.BottomNavigationHelper;
import org.smartregister.listener.BottomNavigationListener;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class LabRegisterActivity extends CoreLabRegisterActivity {
    private final String userLocationTag = getAllSharedPreferences().fetchUserLocationTag();

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
        return new Fragment[]{
                new LabManifestsRegisterFragment(),
        };
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.LAB);
        }
    }
}
