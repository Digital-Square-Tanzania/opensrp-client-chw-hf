package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.core.utils.Utils.passToolbarTitle;
import static org.smartregister.chw.hf.utils.AllClientsUtils.isOfReproductiveAge;
import static org.smartregister.chw.hf.utils.Constants.GENDER.FEMALE;
import static org.smartregister.chw.hf.utils.Constants.JsonForm.HIV_REGISTRATION;
import static org.smartregister.util.Utils.getName;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.vijay.jsonwizard.utils.FormUtils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.core.activity.CoreFamilyPlanningMemberProfileActivity;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.fp.dao.FpDao;
import org.smartregister.chw.fp.domain.Visit;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.fp.util.VisitUtils;
import org.smartregister.chw.hf.BuildConfig;
import org.smartregister.chw.hf.HealthFacilityApplication;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.adapter.ReferralCardViewAdapter;
import org.smartregister.chw.hf.contract.FamilyPlanningMemberProfileContract;
import org.smartregister.chw.hf.interactor.HfFamilyPlanningProfileInteractor;
import org.smartregister.chw.hf.presenter.HfFamilyPlanningMemberProfilePresenter;
import org.smartregister.chw.hf.utils.AllClientsUtils;
import org.smartregister.chw.hf.utils.Constants;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.ld.dao.LDDao;
import org.smartregister.chw.malaria.dao.MalariaDao;
import org.smartregister.chw.sbc.dao.SbcDao;
import org.smartregister.chw.vmmc.dao.VmmcDao;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.domain.Task;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.Utils;

import java.util.Set;

import timber.log.Timber;

public class FpMemberProfileActivity extends CoreFamilyPlanningMemberProfileActivity implements FamilyPlanningMemberProfileContract.View {

    private CommonPersonObjectClient commonPersonObjectClient;
    private CommonPersonObjectClient commonPersonObject;

    public static void startFpMemberProfileActivity(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, FpMemberProfileActivity.class);
        passToolbarTitle(activity, intent);
        intent.putExtra(FamilyPlanningConstants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        activity.startActivity(intent);
    }

    public void setReferralTasks(Set<Task> taskList) {
        if (notificationAndReferralRecyclerView != null && taskList.size() > 0) {
            RecyclerView.Adapter mAdapter = new ReferralCardViewAdapter(taskList, this, getCommonPersonObjectClient(), CoreConstants.REGISTERED_ACTIVITIES.FP_REGISTER_ACTIVITY);
            notificationAndReferralRecyclerView.setAdapter(mAdapter);
            notificationAndReferralLayout.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onCreation() {
        super.onCreation();
        setCommonPersonObjectClient(getClientDetailsByBaseEntityID(fpMemberObject.getBaseEntityId()));
        delayRefreshSetupViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(org.smartregister.chw.core.R.menu.family_planning_member_profile_menu, menu);
        menu.findItem(R.id.action_malaria_followup_visit).setVisible(false);
        menu.findItem(R.id.action_malaria_diagnosis).setVisible(false);
        menu.findItem(R.id.action_fp_change).setVisible(false);

        if (fpMemberObject.getGender().equalsIgnoreCase(FEMALE)) {
            menu.findItem(R.id.action_fp_ecp_provision).setVisible(true);
        }

        // show profile view
        CommonRepository commonRepository = org.smartregister.family.util.Utils.context().commonrepository(org.smartregister.family.util.Utils.metadata().familyMemberRegister.tableName);
        CommonPersonObject personObject = commonRepository.findByBaseEntityId(fpMemberObject.getBaseEntityId());
        commonPersonObject = new CommonPersonObjectClient(personObject.getCaseId(), personObject.getDetails(), "");
        commonPersonObject.setColumnmaps(personObject.getColumnmaps());

        String gender = org.smartregister.chw.core.utils.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        menu.findItem(R.id.action_pregnancy_out_come).setVisible(false);
        if (BuildConfig.BUILD_FOR_BORESHA_AFYA_SOUTH) {
            AllClientsUtils.updateHivMenuItems(fpMemberObject.getBaseEntityId(), menu);
            // AllClientsUtils.updateTbMenuItems(fpMemberObject.getBaseEntityId(), menu);

        }
        if (isOfReproductiveAge(commonPersonObject, gender) && gender.equalsIgnoreCase("female") && !AncDao.isANCMember(fpMemberObject.getBaseEntityId())) {
            menu.findItem(R.id.action_pregnancy_confirmation).setVisible(true);
            menu.findItem(R.id.action_anc_registration).setVisible(true);
            menu.findItem(R.id.action_pregnancy_out_come).setVisible(true);
            menu.findItem(R.id.action_pmtct_register).setVisible(true);
        } else {
            menu.findItem(R.id.action_anc_registration).setVisible(false);
            menu.findItem(R.id.action_pregnancy_confirmation).setVisible(false);
            menu.findItem(R.id.action_anc_registration).setVisible(false);
            menu.findItem(R.id.action_pregnancy_out_come).setVisible(false);
            menu.findItem(R.id.action_pmtct_register).setVisible(false);
        }

        if (HealthFacilityApplication.getApplicationFlavor().hasLD()) {
            menu.findItem(R.id.action_ld_registration).setVisible(isOfReproductiveAge(commonPersonObject, gender) && gender.equalsIgnoreCase("female") && !LDDao.isRegisteredForLD(fpMemberObject.getBaseEntityId()));
        }

        if (gender.equalsIgnoreCase("male") && HealthFacilityApplication.getApplicationFlavor().hasVmmc())
            menu.findItem(R.id.action_vmmc_registration).setVisible(!VmmcDao.isRegisteredForVmmc(fpMemberObject.getBaseEntityId()));

        if (HealthFacilityApplication.getApplicationFlavor().hasHivst()) {
            String dob = org.smartregister.chw.core.utils.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
            int age = org.smartregister.chw.core.utils.Utils.getAgeFromDate(dob);
            menu.findItem(R.id.action_hivst_registration).setVisible(!HivstDao.isRegisteredForHivst(fpMemberObject.getBaseEntityId()) && age >= 15);
        }
        if (HealthFacilityApplication.getApplicationFlavor().hasKvpPrEP()) {
            String dob = org.smartregister.chw.core.utils.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
            int age = org.smartregister.chw.core.utils.Utils.getAgeFromDate(dob);
            menu.findItem(R.id.action_kvp_registration).setVisible(!KvpDao.isRegisteredForKvp(fpMemberObject.getBaseEntityId()) && age >= 15);
        }
        if (HealthFacilityApplication.getApplicationFlavor().hasSbc()) {
            String dob = org.smartregister.chw.core.utils.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
            int age = org.smartregister.chw.core.utils.Utils.getAgeFromDate(dob);
            menu.findItem(R.id.action_sbc_registration).setVisible(!SbcDao.isRegisteredForSbc(fpMemberObject.getBaseEntityId()) && age >= 10);
        }
        if (HealthFacilityApplication.getApplicationFlavor().hasCecap()) {
            String dob = org.smartregister.chw.core.utils.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
            int age = org.smartregister.chw.core.utils.Utils.getAgeFromDate(dob);
            menu.findItem(R.id.action_cancer_preventive_services_registration).setVisible(!CecapDao.isRegisteredForCecap(fpMemberObject.getBaseEntityId()) && age >= 14);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_fp_ecp_provision) {
            FpRegisterActivity.startFpRegistrationActivity(this, fpMemberObject.getBaseEntityId(), Constants.JsonForm.getFPEcpScreening());
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_pregnancy_confirmation) {
            startPregnancyConfirmation();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_pmtct_register) {
            startPmtctRegisration();
            return true;
        }
        if (itemId == org.smartregister.chw.core.R.id.action_anc_registration) {
            startAncTransferInRegistration();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_anc_registration) {
            startAncRegister();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_pregnancy_out_come) {
            startPncRegister();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_vmmc_registration) {
            startVmmcRegister();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_hiv_registration) {
            startHivRegister();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_cbhs_registration) {
            startHivRegister();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_pmtct_register) {
            startPmtctRegisration();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_ld_registration) {
            startLDRegistration();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_hivst_registration) {
            startHivstRegistration();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_agyw_screening) {
            startAgywScreening();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_kvp_registration) {
            startKvpRegistration();
        } else if (itemId == org.smartregister.chw.core.R.id.action_prep_registration) {
            startPrEPRegistration();
        } else if (itemId == org.smartregister.chw.core.R.id.action_sbc_registration) {
            startSbcRegistration();
        } else if (itemId == org.smartregister.chw.core.R.id.action_gbv_registration) {
            startGbvRegistration();
        } else if (itemId == org.smartregister.chw.core.R.id.action_cancer_preventive_services_registration) {
            startCancerPreventiveServicesRegistration();
        } else if (itemId == org.smartregister.chw.core.R.id.action_asrh_registration) {
            startAsrhRegistration();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void startAncRegister() {
        AncRegisterActivity.startAncRegistrationActivity(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId(), fpMemberObject.getPhoneNumber(), CoreConstants.JSON_FORM.getAncRegistration(), null, fpMemberObject.getFamilyBaseEntityId(), fpMemberObject.getFamilyName());
    }

    protected void startPncRegister() {
        PncRegisterActivity.startPncRegistrationActivity(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId(), fpMemberObject.getPhoneNumber(), CoreConstants.JSON_FORM.getPregnancyOutcome(), null, fpMemberObject.getFamilyBaseEntityId(), fpMemberObject.getFamilyName(), null, false);
    }

    protected void startPmtctRegisration() {
        PncRegisterActivity.startPncRegistrationActivity(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId(), fpMemberObject.getPhoneNumber(), Constants.JsonForm.getPmtctRegistrationForClientsPostPnc(), null, fpMemberObject.getFamilyBaseEntityId(), fpMemberObject.getFamilyName(), null, false);
    }

    protected void startLDRegistration() {
        String firstName = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.FIRST_NAME, true);
        String middleName = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.MIDDLE_NAME, true);
        String lastName = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.LAST_NAME, true);

        String dob = org.smartregister.family.util.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, true);
        int age = StringUtils.isNotBlank(dob) ? org.smartregister.family.util.Utils.getAgeFromDate(dob) : 0;

        try {
            LDRegistrationFormActivity.startMe(this, fpMemberObject.getBaseEntityId(), false, getName(getName(firstName, middleName), lastName), String.valueOf(age));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected void startHivstRegistration() {
        String gender = org.smartregister.chw.core.utils.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        String dob = org.smartregister.chw.core.utils.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = org.smartregister.chw.core.utils.Utils.getAgeFromDate(dob);
        HivstRegisterActivity.startHivstRegistrationActivity(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId(), gender, age);
    }

    protected void startKvpRegistration() {
        String gender = AllClientsUtils.getClientGender(fpMemberObject.getBaseEntityId());
        String dob = org.smartregister.chw.core.utils.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = org.smartregister.chw.core.utils.Utils.getAgeFromDate(dob);
        if (gender.equalsIgnoreCase(Constants.GENDER.MALE)) {
            KvpRegisterActivity.startKvpScreeningMale(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId(), gender, age);
        }
        if (gender.equalsIgnoreCase(Constants.GENDER.FEMALE)) {
            KvpRegisterActivity.startKvpScreeningFemale(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId(), gender, age);
        }
    }

    protected void startVmmcRegister() {
        VmmcRegisterActivity.startVmmcRegistrationActivity(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId());
    }

    protected void startHivRegister() {
        try {
            HivRegisterActivity.startHIVFormActivity(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId(), HIV_REGISTRATION, (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, HIV_REGISTRATION).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    protected void startPrEPRegistration() {
        String gender = AllClientsUtils.getClientGender(fpMemberObject.getBaseEntityId());
        String dob = org.smartregister.chw.core.utils.Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
        int age = org.smartregister.chw.core.utils.Utils.getAgeFromDate(dob);
        PrEPRegisterActivity.startMe(this, fpMemberObject.getBaseEntityId(), gender, age);
    }

    protected void startAgywScreening() {
        //do nothing
    }

    protected void startSbcRegistration() {
        SbcRegisterActivity.startRegistration(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId());
    }

    protected void startGbvRegistration() {
        //Implement
    }

    protected void startCancerPreventiveServicesRegistration() {
        CecapRegisterActivity.startRegistration(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId());
    }

    protected void startAsrhRegistration() {
        //Not Required
    }

    protected void startAncTransferInRegistration() {
        AncRegisterActivity.startAncRegistrationActivity(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId(), fpMemberObject.getPhoneNumber(), Constants.JSON_FORM.ANC_TRANSFER_IN_REGISTRATION, null, fpMemberObject.getFamilyBaseEntityId(), fpMemberObject.getFamilyName());
    }

    protected void startPregnancyConfirmation() {
        AncRegisterActivity.startAncRegistrationActivity(FpMemberProfileActivity.this, fpMemberObject.getBaseEntityId(), fpMemberObject.getPhoneNumber(), CoreConstants.JSON_FORM.ANC_PREGNANCY_CONFIRMATION, null, fpMemberObject.getFamilyBaseEntityId(), fpMemberObject.getFamilyName());
    }


    @Override
    protected void onResume() {
        super.onResume();
        try {
            VisitUtils.processVisits(fpMemberObject.getBaseEntityId());
        } catch (Exception e) {
            Timber.e(e);
        }
        ((FamilyPlanningMemberProfileContract.Presenter) fpProfilePresenter).fetchReferralTasks();
        if (notificationAndReferralRecyclerView != null && notificationAndReferralRecyclerView.getAdapter() != null) {
            notificationAndReferralRecyclerView.getAdapter().notifyDataSetChanged();
        }
        delayRefreshSetupViews();
    }

    private void delayRefreshSetupViews() {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                fpMemberObject = FpDao.getMember(commonPersonObjectClient.getCaseId());
                getLastVisit();
                setupViews();
                Visit lastVisit = FpDao.getLatestVisit(fpMemberObject.getBaseEntityId(), FamilyPlanningConstants.EVENT_TYPE.FP_POINT_OF_SERVICE_DELIVERY);
                if (lastVisit != null)
                    refreshMedicalHistory(true);
            }, 300);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public CommonPersonObjectClient getCommonPersonObjectClient() {
        return commonPersonObjectClient;
    }

    public void setCommonPersonObjectClient(CommonPersonObjectClient commonPersonObjectClient) {
        this.commonPersonObjectClient = commonPersonObjectClient;
    }

    @Override
    protected void initializePresenter() {
        showProgressBar(true);
        fpProfilePresenter = new HfFamilyPlanningMemberProfilePresenter(this, new HfFamilyPlanningProfileInteractor(), fpMemberObject);
        fpProfilePresenter.refreshProfileBottom();
    }

    @Override
    public void openMedicalHistory() {
        FpMedicalHistoryActivity.startMe(FpMemberProfileActivity.this, fpMemberObject);
    }

    @Override
    public Visit getLastVisit() {
        try {
            Visit lastVisit = FpDao.getLatestVisit(fpMemberObject.getBaseEntityId());
            if (lastVisit.getParentVisitID() != null) {
                Visit parentVisit = FpDao.getLatestVisitById(lastVisit.getParentVisitID());
                if (parentVisit != null) return parentVisit;
            }
            return lastVisit;
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    @Override
    public boolean isFirstVisit() {
        return FpDao.getLatestVisit(fpMemberObject.getBaseEntityId(), FamilyPlanningConstants.EVENT_TYPE.FP_PROVIDE_METHOD) == null;
    }

    @Override
    public void startPointOfServiceDeliveryForm() {
        startFormActivity(FamilyPlanningConstants.FORMS.FP_POINT_OF_SERVICE_DELIVERY, fpMemberObject.getBaseEntityId(), null);
    }

    @Override
    public void startFpCounselingForm() {
        startFormActivity(FamilyPlanningConstants.FORMS.FP_COUNSELING, fpMemberObject.getBaseEntityId(), null);
    }

    @Override
    public void startFpScreeningForm() {
        FpScreeningActivity.startMe(this, fpMemberObject.getBaseEntityId(), false);
    }

    @Override
    public void startProvideFpMethod() {
        startFormActivity(FamilyPlanningConstants.FORMS.FP_PROVISION_OF_FP_METHOD, fpMemberObject.getBaseEntityId(), null);
    }

    @Override
    public void startProvideOtherServices() {
        FpOtherServicesActivity.startMe(this, fpMemberObject.getBaseEntityId(), false);
    }

    @Override
    public void startFpFollowupVisit() {
        FpFollowupVisitProvisionOfServicesActivity.startMe(this, fpMemberObject.getBaseEntityId(), false);
    }

    @Override
    public void showFollowUpVisitButton() {
        //Not Required
    }

    @Override
    protected void removeMember() {
        // Not required for HF (as seen in other profile activities)?
    }

    @Override
    protected void startFamilyPlanningRegistrationActivity() {
        FpRegisterActivity.startFpRegistrationActivity(this, fpMemberObject.getBaseEntityId(), CoreConstants.JSON_FORM.getFpChangeMethodForm(fpMemberObject.getGender()));
    }

    @Override
    public void verifyHasPhone() {
        // TODO -> Implement for HF
    }

    @Override
    public void notifyHasPhone(boolean b) {
        // TODO -> Implement for HF
    }

    @Override
    public Class getFormActivity() {
        return Utils.metadata().familyMemberFormActivity;
    }

    @Override
    public void setFollowUpButtonOverdue() {
        showFollowUpVisitButton();
        textViewRecordFp.setBackground(getResources().getDrawable(org.smartregister.chw.fp.R.drawable.record_btn_selector));
    }

//    @Override
//    public void viewRegistrationDetails() {
//        FpRegistrationDetailsActivity.startMe(FpMemberProfileActivity.this, fpMemberObject);
//    }
}
