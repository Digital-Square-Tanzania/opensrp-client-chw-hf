package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.vmmc.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreVmmcProfileActivity;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.adapter.ReferralCardViewAdapter;
import org.smartregister.chw.hf.contract.VmmcProfileContract;
import org.smartregister.chw.hf.presenter.FamilyOtherMemberActivityPresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.domain.Task;
import org.smartregister.family.model.BaseFamilyOtherMemberProfileActivityModel;
import org.smartregister.family.util.JsonFormUtils;

import java.util.Set;

import timber.log.Timber;

public class VmmcProfileActivity extends CoreVmmcProfileActivity implements VmmcProfileContract.InteractorCallback {

    private static String baseEntityId;
    private CommonPersonObjectClient commonPersonObjectClient;

    public static void startVmmcActivity(Activity activity, String baseEntityId) {
        VmmcProfileActivity.baseEntityId = baseEntityId;
        Intent intent = new Intent(activity, VmmcProfileActivity.class);
        intent.putExtra(BASE_ENTITY_ID, baseEntityId);
        passToolbarTitle(activity, intent);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        findViewById(R.id.record_visit_vmmc).setVisibility(View.GONE);
        this.setOnMemberTypeLoadedListener(memberType -> {
            switch (memberType.getMemberType()) {
                case CoreConstants.TABLE_NAME.ANC_MEMBER:
                    AncMedicalHistoryActivity.startMe(VmmcProfileActivity.this, memberType.getMemberObject());
                    break;
                case CoreConstants.TABLE_NAME.PNC_MEMBER:
                    PncMedicalHistoryActivity.startMe(VmmcProfileActivity.this, memberType.getMemberObject());
                    break;
                case CoreConstants.TABLE_NAME.CHILD:
                    ChildMedicalHistoryActivity.startMe(VmmcProfileActivity.this, memberType.getMemberObject());
                    break;
                default:
                    Timber.v("Member info undefined");
                    break;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem item = menu.findItem(R.id.action_remove_member);
        if (item != null) {
            menu.removeItem(item.getItemId());
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == JsonFormUtils.REQUEST_CODE_GET_JSON) {
            try {
                String jsonString = data.getStringExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON);
                JSONObject form = new JSONObject(jsonString);

            } catch (Exception ex) {
                Timber.e(ex);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return FamilyProfileActivity.class;
    }

    @Override
    protected void removeMember() {
        //Implemented from abstract class but not required right now for HF
    }

    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        if (presenter == null) {
            presenter = new FamilyOtherMemberActivityPresenter(this, new BaseFamilyOtherMemberProfileActivityModel(),
                    null, memberObject.getRelationalId(), memberObject.getBaseEntityId(),
                    memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), memberObject.getAddress(),
                    memberObject.getLastName());
        }
        return (CoreFamilyOtherMemberActivityPresenter) presenter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCommonPersonObjectClient(getClientDetailsByBaseEntityID(memberObject.getBaseEntityId()));
    }

    @Override
    public void setProfileImage(String s, String s1) {
        //Overridden from abstract class not yet implemented
    }

    @Override
    public void setProfileDetailThree(String s) {
        //Implemented from abstract class but not required right now for HF
    }

    @Override
    public void toggleFamilyHead(boolean b) {
        //Implemented from abstract class but not required right now for HF
    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {
        //Implemented from abstract class but not required right now for HF
    }

    @Override
    public void refreshList() {
        //Overridden from abstract class not yet implemented
    }

    @Override
    public void updateHasPhone(boolean hasPhone) {
        //Overridden from abstract class not yet implemented
    }

    @Override
    public void setFamilyServiceStatus(String status) {
        findViewById(R.id.rlFamilyServicesDue).setVisibility(View.GONE);
        findViewById(R.id.rlVmmcPositiveDate).setVisibility(View.GONE);
    }

    @Override
    public void verifyHasPhone() {
        //Implemented from abstract class but not required right now for HF
    }

    @Override
    public void notifyHasPhone(boolean hasPhone) {
        //Overridden from abstract class not yet implemented
    }

    @Override
    public void updateReferralTasks(Set<Task> taskList) {
        if (notificationAndReferralRecyclerView != null && taskList.size() > 0) {
            RecyclerView.Adapter mAdapter = new ReferralCardViewAdapter(taskList, this, commonPersonObjectClient, CoreConstants.REGISTERED_ACTIVITIES.VMMC_REGISTER_ACTIVITY);
            notificationAndReferralRecyclerView.setAdapter(mAdapter);
            notificationAndReferralLayout.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
        }
    }

    public void setCommonPersonObjectClient(CommonPersonObjectClient commonPersonObjectClient) {
        this.commonPersonObjectClient = commonPersonObjectClient;
    }
}
