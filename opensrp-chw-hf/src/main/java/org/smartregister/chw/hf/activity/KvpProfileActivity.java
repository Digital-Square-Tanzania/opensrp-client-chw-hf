package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.hf.utils.Constants.JsonForm.HIV_REGISTRATION;
import static org.smartregister.util.Utils.getName;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONException;
import org.smartregister.chw.core.activity.CoreKvpProfileActivity;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.dao.HfKvpDao;
import org.smartregister.chw.hf.utils.AllClientsUtils;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.kvp.KvpLibrary;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.Utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

public class KvpProfileActivity extends CoreKvpProfileActivity {

    public static void startProfile(Activity activity, String baseEntityId) {
        Intent intent = new Intent(activity, KvpProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.KVP_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        if (HfKvpDao.wereSelfTestingKitsDistributed(memberObject.getBaseEntityId())) {
            if (HivstDao.isRegisteredForHivst(memberObject.getBaseEntityId())) {
                boolean shouldIssueHivSelfTestingKits = false;
                String lastSelfTestingFollowupDateString = HivstDao.clientLastFollowup(memberObject.getBaseEntityId());
                if (lastSelfTestingFollowupDateString == null) {
                    shouldIssueHivSelfTestingKits = true;
                } else {
                    try {
                        Date lastSelfTestingFollowupDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(lastSelfTestingFollowupDateString);
                        Visit lastVisit = getVisit(org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT);
                        if (truncateTimeFromDate(lastSelfTestingFollowupDate).before(truncateTimeFromDate(lastVisit.getDate())) && lastVisit.getProcessed()) {
                            shouldIssueHivSelfTestingKits = true;
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
                }

                if (shouldIssueHivSelfTestingKits) {
                    textViewRecordKvp.setVisibility(View.GONE);
                    visitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setText(R.string.issue_selft_testing_kits);
                    textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_followup));
                    textViewVisitDone.setVisibility(View.VISIBLE);
                    textViewVisitDoneEdit.setOnClickListener(view -> HivstProfileActivity.startProfile(KvpProfileActivity.this, memberObject.getBaseEntityId(), true));
                    imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
                } else {
                    textViewRecordKvp.setVisibility(View.VISIBLE);
                    visitDone.setVisibility(View.GONE);
                    textViewVisitDone.setVisibility(View.GONE);
                    if (isVisitOnProgress(profileType)) {
                        textViewRecordKvp.setVisibility(View.GONE);
                        visitInProgress.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                textViewRecordKvp.setVisibility(View.GONE);
                visitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setText(R.string.register_client);
                textViewVisitDone.setText(getContext().getString(R.string.pending_hivst_registration));
                textViewVisitDone.setVisibility(View.VISIBLE);
                textViewVisitDoneEdit.setOnClickListener(v -> startHivstRegistration());
                imageViewCross.setImageResource(org.smartregister.chw.core.R.drawable.activityrow_notvisited);
            }
        }
    }

    private Date truncateTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    @Override
    public void openFollowupVisit() {
        KvpServiceActivity.startMe(this, memberObject.getBaseEntityId());
    }

    @Override
    protected void startPrEPRegistration() {
        PrEPRegisterActivity.startMe(this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getAge());
    }


    @Override
    public void refreshMedicalHistory(boolean hasHistory) {
        Visit kvpBehavioralServices = getVisit(org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT);
        Visit kvpBioMedicalServices = getVisit(org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT);
        Visit kvpStructuralServices = getVisit(org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT);
        Visit kvpOtherServicesVisit = getVisit(org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT);


        if (kvpBehavioralServices != null || kvpBioMedicalServices != null || kvpOtherServicesVisit != null || kvpStructuralServices != null) {
            rlLastVisit.setVisibility(View.VISIBLE);
            findViewById(R.id.view_notification_and_referral_row).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.vViewHistory)).setText(R.string.visits_history);
            ((TextView) findViewById(R.id.ivViewHistoryArrow)).setText(getString(R.string.view_visits_history));
            textViewRecordKvp.setText(R.string.record_kvp_followup_visit);
        } else {
            rlLastVisit.setVisibility(View.GONE);
            textViewRecordKvp.setText(R.string.record_kvp);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);
        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());

        AllClientsUtils.updateOptionsMenu(menu, client);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_anc_registration) {
            startAncRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_pregnancy_out_come) {
            startPncRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_initiation) {
            startFpRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_fp_ecp_provision) {
            startFpEcpScreening();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_malaria_registration) {
            startMalariaRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_iccm_registration) {
            startIntegratedCommunityCaseManagementEnrollment();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_vmmc_registration) {
            startVmmcRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_hiv_registration) {
            startHivRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_cbhs_registration) {
            startHivRegister();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_tb_registration) {
            startTbRegister();
        } else if (i == org.smartregister.chw.core.R.id.action_pmtct_register) {
            startPmtctRegisration();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_ld_registration) {
            startLDRegistration();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_hivst_registration) {
            startHivstRegistration();
            return true;
        } else if (i == org.smartregister.chw.core.R.id.action_prep_registration) {
            startPrEPRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_sbc_registration) {
            startSbcRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_gbv_registration) {
            startGbvRegistration();
        } else if (i == org.smartregister.chw.core.R.id.action_cancer_preventive_services_registration) {
            startCancerPreventiveServicesRegistration();
        }
        return super.onOptionsItemSelected(item);
    }

    protected void startSbcRegistration() {
        SbcRegisterActivity.startRegistration(KvpProfileActivity.this, memberObject.getBaseEntityId());
    }

    protected void startAncRegister() {
        AncRegisterActivity.startAncRegistrationActivity(KvpProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), CoreConstants.JSON_FORM.getAncRegistration(), null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
    }

    protected void startPncRegister() {
        PncRegisterActivity.startPncRegistrationActivity(KvpProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), CoreConstants.JSON_FORM.getPregnancyOutcome(), null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName(), null, false);
    }

    protected void startMalariaRegister() {
        MalariaRegisterActivity.startMalariaRegistrationActivity(KvpProfileActivity.this, memberObject.getBaseEntityId());

    }

    protected void startVmmcRegister() {

        VmmcRegisterActivity.startVmmcRegistrationActivity(KvpProfileActivity.this, memberObject.getBaseEntityId());
    }

    protected void startIntegratedCommunityCaseManagementEnrollment() {

    }

    protected void startHivRegister() {
        try {
            HivRegisterActivity.startHIVFormActivity(KvpProfileActivity.this, memberObject.getBaseEntityId(), HIV_REGISTRATION, (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, HIV_REGISTRATION).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    protected void startTbRegister() {
        try {
            TbRegisterActivity.startTbFormActivity(KvpProfileActivity.this, memberObject.getBaseEntityId(), CoreConstants.JSON_FORM.getTbRegistration(), (new FormUtils()).getFormJsonFromRepositoryOrAssets(this, CoreConstants.JSON_FORM.getTbRegistration()).toString());
        } catch (JSONException e) {
            Timber.e(e);
        }
    }

    protected void startFpRegister() {
        FpRegisterActivity.startFpRegistrationActivity(this, memberObject.getBaseEntityId(), CoreConstants.JSON_FORM.getFpRegistrationForm(memberObject.getGender()));
    }

    protected void startFpEcpScreening() {
        FpRegisterActivity.startFpRegistrationActivity(this, memberObject.getBaseEntityId(), org.smartregister.chw.hf.utils.Constants.JsonForm.getFPEcpScreening());
    }

    protected void startPmtctRegisration() {
        PncRegisterActivity.startPncRegistrationActivity(KvpProfileActivity.this, memberObject.getBaseEntityId(), memberObject.getPhoneNumber(), org.smartregister.chw.hf.utils.Constants.JsonForm.getPmtctRegistrationForClientsPostPnc(), null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName(), null, false);
    }

    protected void startLDRegistration() {
        String firstName = memberObject.getFirstName();
        String middleName = memberObject.getMiddleName();
        String lastName = memberObject.getLastName();
        try {
            LDRegistrationFormActivity.startMe(this, memberObject.getBaseEntityId(), false, getName(getName(firstName, middleName), lastName), String.valueOf(memberObject.getAge()));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected void startGbvRegistration() {
        //Implement
    }

    protected void startCancerPreventiveServicesRegistration() {
        CecapRegisterActivity.startRegistration(KvpProfileActivity.this, memberObject.getBaseEntityId());
    }


    @Override
    public void startHivstRegistration() {
        CommonRepository commonRepository = Utils.context().commonrepository(Utils.metadata().familyMemberRegister.tableName);

        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client =
                new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());
        String gender = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        HivstRegisterActivity.startHivstRegistrationActivity(this, memberObject.getBaseEntityId(), gender);
    }

    @Override
    public void openMedicalHistory() {
        KvpMedicalHistoryActivity.startMe(this, memberObject);
    }

    private Visit getVisit(String eventType) {
        return KvpLibrary.getInstance().visitRepository().getLatestVisit(memberObject.getBaseEntityId(), eventType);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
        if (KvpDao.hasTestResults(memberObject.getBaseEntityId())) {
            rlTestResults.setVisibility(View.VISIBLE);
            viewSeparator1.setVisibility(View.VISIBLE);
        } else {
            rlTestResults.setVisibility(View.GONE);
            viewSeparator1.setVisibility(View.GONE);
        }
    }

    @Override
    public void openTestResults() {
        Intent intent = new Intent(this, KvpTestResultsViewActivity.class);
        intent.putExtra(org.smartregister.chw.cecap.util.Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberObject.getBaseEntityId());
        startActivity(intent);
    }
}
