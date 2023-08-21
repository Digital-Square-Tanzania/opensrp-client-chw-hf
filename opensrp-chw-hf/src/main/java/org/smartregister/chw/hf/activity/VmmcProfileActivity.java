package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import org.smartregister.chw.core.activity.CoreFamilyProfileActivity;
import org.smartregister.chw.core.activity.CoreVmmcProfileActivity;
import org.smartregister.chw.core.presenter.CoreFamilyOtherMemberActivityPresenter;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.dao.HfAncDao;
import org.smartregister.chw.hf.dao.HfVmmcDao;
import org.smartregister.chw.vmmc.domain.Visit;
import org.smartregister.chw.vmmc.util.Constants;
import org.smartregister.chw.vmmc.VmmcLibrary;

public class VmmcProfileActivity extends CoreVmmcProfileActivity {

    private static String baseEntityId;
    private String getGentialExaminationValue;
    private String getAnyComplaintsValue;
    private String getDiagnosedValue;
    private String getAnyComplicationsPreviousSurgicalProcedureValue;
    private String getSymptomsHematologicalDiseaseValue;
    private String getKnownAllergiesValue;
    private String getHivTestResultValue;



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

    }

    @NonNull
    @Override
    public CoreFamilyOtherMemberActivityPresenter presenter() {
        return null;
    }

    @Override
    public void setProfileImage(String s, String s1) {

    }

    @Override
    public void setProfileDetailThree(String s) {

    }

    @Override
    public void toggleFamilyHead(boolean b) {

    }

    @Override
    public void togglePrimaryCaregiver(boolean b) {

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
//            ((TextView) findViewById(R.id.vViewHistory)).setText(R.string.visits_history);
//            ((TextView) findViewById(R.id.ivViewHistoryArrow)).setText(getString(R.string.view_visits_history));
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
    public void refreshList() {

    }

    @Override
    public void updateHasPhone(boolean b) {

    }

    @Override
    public void setFamilyServiceStatus(String s) {

    }

    @Override
    public void verifyHasPhone() {

    }

    @Override
    public void notifyHasPhone(boolean b) {

    }

//    @Override
//    protected void setupButtons(){
//        getGentialExaminationValue = HfVmmcDao.getGentialExamination(memberObject.getBaseEntityId());
//        getDiagnosedValue = HfVmmcDao.getDiagnosed(memberObject.getBaseEntityId());
//        getAnyComplicationsPreviousSurgicalProcedureValue = HfVmmcDao.getAnyComplicationsPreviousSurgicalProcedure(memberObject.getBaseEntityId());
//        getHivTestResultValue = HfVmmcDao.getHivTestResult(memberObject.getBaseEntityId());
//        getKnownAllergiesValue = HfVmmcDao.getKnownAllergiesValue(memberObject.getBaseEntityId());
//        getSymptomsHematologicalDiseaseValue = HfVmmcDao.getSymptomsHematologicalDiseaseValue(memberObject.getBaseEntityId());
//        getAnyComplaintsValue = HfVmmcDao.getAnyComplaints(memberObject.getBaseEntityId());
//
//        if (getGentialExaminationValue.equalsIgnoreCase("None") &&
//                getDiagnosedValue.equalsIgnoreCase("None") &&
//                getKnownAllergiesValue.equalsIgnoreCase("None") &&
//                getAnyComplicationsPreviousSurgicalProcedureValue.equalsIgnoreCase("No") &&
//                getHivTestResultValue.equalsIgnoreCase("Negative") &&
//                getSymptomsHematologicalDiseaseValue.equalsIgnoreCase("None") &&
//                getAnyComplaintsValue.equalsIgnoreCase("None")
//        ){
//            textViewRecordVmmc.setVisibility(View.GONE);
//            textViewProcedureVmmc.setVisibility(View.VISIBLE);
//            textViewDischargeVmmc.setVisibility(View.GONE);
//            textViewNotifiableVmmc.setVisibility(View.GONE);
//            rlLastVisit.setVisibility(View.VISIBLE);
//        }
//    }
}

