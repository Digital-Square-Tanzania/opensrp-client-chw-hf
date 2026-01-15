package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.view.View;

import org.smartregister.chw.ayp.AypLibrary;
import org.smartregister.chw.ayp.dao.AypDao;
import org.smartregister.chw.ayp.domain.MemberObject;
import org.smartregister.chw.ayp.domain.Visit;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.core.activity.CoreAypProfileActivity;
import org.smartregister.chw.hf.R;

public class AypFacilityServicesProfileActivity extends CoreAypProfileActivity {

    public static void startProfileActivity(Activity activity, String baseEntityId) {
        android.content.Intent intent = new android.content.Intent(activity, AypFacilityServicesProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    @Override
    protected void setupButtons() {
        if (textViewRecordayp != null) {
            textViewRecordayp.setVisibility(android.view.View.VISIBLE);
            textViewRecordayp.setText(R.string.ayp_facility_record_biomedical_visit);
        }
        if (textViewGraduate != null) {
            textViewGraduate.setVisibility(android.view.View.GONE);
        }
        if (textViewContinueaypService != null) {
            textViewContinueaypService.setVisibility(android.view.View.GONE);
        }
    }

    @Override
    protected Visit getAypOutSchoolVisit() {
        return AypLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.AYP_FACILITY_SERVICES);
    }

    @Override
    public void openFollowupVisit() {
        AypFacilityBiomedicalVisitActivity.startMe(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void startServiceForm() {
        AypFacilityBiomedicalVisitActivity.startMe(this, memberObject.getBaseEntityId(), false);
    }

    @Override
    public void openMedicalHistory() {
        AypFacilityServicesMedicalHistoryActivity.startMe(this, memberObject);
    }

    @Override
    public void continueService() {
        // Not implemented
    }

    @Override
    public void continueDischarge() {
        // Not implemented
    }

    @Override
    public void startHivstRegistration() {
        HivstRegisterActivity.startHivstRegistrationActivity(this, memberObject.getBaseEntityId(), memberObject.getGender());
    }

    @Override
    protected MemberObject getMemberObject(String baseEntityId) {
        return AypDao.getFacilityMember(baseEntityId);
    }

    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        boolean showLastVisit = getLatestFollowUpVisit() != null;
        rlLastVisit.setVisibility(showLastVisit ? View.VISIBLE : View.GONE);
    }

    private Visit getLatestFollowUpVisit() {
        return AypLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.AYP_FACILITY_SERVICES);
    }

}
