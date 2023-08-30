package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreVmmcProfileActivity;
import org.smartregister.chw.core.custom_views.CoreVmmcFloatingMenu;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.custom_view.VmmcFloatingMenu;
import org.smartregister.chw.hf.utils.VmmcReferralFormUtils;
import org.smartregister.chw.vmmc.domain.Visit;
import org.smartregister.chw.vmmc.util.Constants;
import org.smartregister.chw.vmmc.VmmcLibrary;
import timber.log.Timber;

public class VmmcProfileActivity extends CoreVmmcProfileActivity {

    private static String baseEntityId;

    public static void startVmmcActivity(Activity activity, String baseEntityId) {
        VmmcProfileActivity.baseEntityId = baseEntityId;
        Intent intent = new Intent(activity, VmmcProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.VMMC_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    public void openFollowupVisit() {
        VmmcServiceActivity.startVmmcVisitActivity(this, baseEntityId, false);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.textview_procedure_vmmc) {
            VmmcProcedureActivity.startVmmcVisitProcedureActivity(this, baseEntityId, false);
        }

        if (id == R.id.textview_discharge_vmmc) {
            VmmcDischargeActivity.startVmmcVisitDischargeActivity(this, baseEntityId, false);
        }
        if (id == R.id.textview_notifiable_vmmc) {
            VmmcNotifiableAdverseActivity.startVmmcVisitActivity(this, baseEntityId, false);
        }
        if (id == R.id.textview_followup_vmmc) {
            VmmcFollowUpActivity.startVmmcVisitActivity(this, baseEntityId, false);

        }
        else {
            super.onClick(view);
        }
    }

    @Override
    protected Class<? extends CoreFamilyProfileActivity> getFamilyProfileActivityClass() {
        return null;
    }

    @Override
    protected void removeMember() {
//        Implement vmmc function here
    }

    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    @Override
    public void setProfileImage(String s, String s1) {
//        Implement vmmc function here
    }

    @Override
    public void setProfileDetailThree(String s) {
//        Implement vmmc function here
    }

    @Override
    public void toggleFamilyHead(boolean b) {
//        Implement vmmc function here
    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {
//        Implement vmmc function here
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        Visit vmmcServices = getVisit(Constants.EVENT_TYPE.VMMC_SERVICES);
        Visit vmmcProcedure = getVisit(Constants.EVENT_TYPE.VMMC_PROCEDURE);
        Visit vmmcDischarge = getVisit(Constants.EVENT_TYPE.VMMC_DISCHARGE);
        Visit vmmcFollowUp = getVisit(Constants.EVENT_TYPE.VMMC_FOLLOW_UP_VISIT);
        Visit vmmcNotifiableAdverse = getVisit(Constants.EVENT_TYPE.VMMC_NOTIFIABLE_EVENTS);


        if (vmmcServices != null || vmmcProcedure != null || vmmcDischarge != null || vmmcFollowUp != null || vmmcNotifiableAdverse != null) {
            rlLastVisit.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
        } else {
            rlLastVisit.setVisibility(View.GONE);
        }
    }

    @Override
    public void openMedicalHistory() {
        VmmcMedicalHistoryActivity.startMe(this, memberObject);
    }

    private Visit getVisit(String eventType) {
        return VmmcLibrary.getInstance().visitRepository().getLatestVisit(baseEntityId, eventType);
    }

    @Override
    public void initializeFloatingMenu() {

        baseVmmcFloatingMenu = new VmmcFloatingMenu(this, memberObject);

        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.vmmc_fab:
                    ((CoreVmmcFloatingMenu) baseVmmcFloatingMenu).animateFAB();
                    break;
                case R.id.call_layout:
                    ((CoreVmmcFloatingMenu) baseVmmcFloatingMenu).launchCallWidget();
                    ((CoreVmmcFloatingMenu) baseVmmcFloatingMenu).animateFAB();
                    break;
                case R.id.refer_to_facility_layout:
                    VmmcReferralFormUtils.startVmmcReferral(this, memberObject.getBaseEntityId());
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }
        };

        ((CoreVmmcFloatingMenu) baseVmmcFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
        baseVmmcFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.END);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseVmmcFloatingMenu, linearLayoutParams);
    }

        @Override
    public void refreshList() {
//        Implement vmmc function here
    }

    @Override
    public void updateHasPhone(boolean b) {
//        Implement vmmc function here
    }

    @Override
    public void setFamilyServiceStatus(String s) {
//        Implement vmmc function here
    }

    @Override
    public void verifyHasPhone() {
//        Implement vmmc function here
    }

    @Override
    public void notifyHasPhone(boolean b) {
//        Implement vmmc function here
    }
}

