package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.hf.utils.JsonFormUtils.getAutoPopulatedJsonEditFormString;
import static org.smartregister.util.Utils.getName;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.smartregister.chw.cecap.CecapLibrary;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.cecap.domain.MemberObject;
import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.chw.cecap.util.VisitUtils;
import org.smartregister.chw.core.activity.CoreCecapMemberProfileActivity;
import org.smartregister.chw.core.dao.AncDao;
import org.smartregister.chw.core.form_data.NativeFormsDataBinder;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.core.utils.CoreReferralUtils;
import org.smartregister.chw.core.utils.UpdateDetailsUtil;
import org.smartregister.chw.core.utils.Utils;
import org.smartregister.chw.hf.BuildConfig;
import org.smartregister.chw.hf.HealthFacilityApplication;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.custom_view.CecapFloatingMenu;
import org.smartregister.chw.hf.dataloader.FamilyMemberDataLoader;
import org.smartregister.chw.hf.utils.AllClientsUtils;
import org.smartregister.chw.hivst.dao.HivstDao;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.ld.dao.LDDao;
import org.smartregister.chw.malaria.dao.MalariaDao;
import org.smartregister.chw.sbc.dao.SbcDao;
import org.smartregister.chw.vmmc.dao.VmmcDao;
import org.smartregister.commonregistry.CommonPersonObject;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.commonregistry.CommonRepository;
import org.smartregister.family.util.DBConstants;
import org.smartregister.family.util.JsonFormUtils;

import timber.log.Timber;

public class CecapMemberProfileActivity extends CoreCecapMemberProfileActivity {

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, CecapMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void recordCecap(MemberObject memberObject) {
        CecapVisitActivity.startMe(this, memberObject.getBaseEntityId(), false, textViewRecordCecap.getText().equals(getString(R.string.cecap_via_approach)));
    }

    @Override
    protected void setupViews() {
        super.setupViews();
        try {
            VisitUtils.processVisits(CecapLibrary.getInstance().visitRepository(), CecapLibrary.getInstance().visitDetailsRepository(), CecapMemberProfileActivity.this);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    @Override
    public void openMedicalHistory() {
        CecapMedicalHistoryActivity.startMe(this, memberObject);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupViews();
        fetchProfileData();
        profilePresenter.refreshProfileBottom();
        if (CecapDao.hasTestResults(memberObject.getBaseEntityId())) {
            rlTestResults.setVisibility(View.VISIBLE);
            viewSeparator1.setVisibility(View.VISIBLE);
        } else {
            rlTestResults.setVisibility(View.GONE);
            viewSeparator1.setVisibility(View.GONE);
        }
    }

    @Override
    public void initializeFloatingMenu() {
        baseCecapFloatingMenu = new CecapFloatingMenu(this, memberObject);
        baseCecapFloatingMenu.setGravity(Gravity.BOTTOM | Gravity.RIGHT);
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addContentView(baseCecapFloatingMenu, linearLayoutParams);


        OnClickFloatingMenu onClickFloatingMenu = viewId -> {
            switch (viewId) {
                case R.id.cecap_fab:
                    checkPhoneNumberProvided();
                    ((CecapFloatingMenu) baseCecapFloatingMenu).animateFAB();
                    break;
                case R.id.cecap_call_layout:
                    ((CecapFloatingMenu) baseCecapFloatingMenu).launchCallWidget();
                    ((CecapFloatingMenu) baseCecapFloatingMenu).animateFAB();
                    break;
                default:
                    Timber.d("Unknown fab action");
                    break;
            }

        };

        ((CecapFloatingMenu) baseCecapFloatingMenu).setFloatMenuClickListener(onClickFloatingMenu);
    }

    private void checkPhoneNumberProvided() {
        boolean phoneNumberAvailable = (StringUtils.isNotBlank(memberObject.getPhoneNumber()));
        ((CecapFloatingMenu) baseCecapFloatingMenu).redraw(phoneNumberAvailable);
    }

    public void openTestResults() {
        Intent intent = new Intent(this, CecapTestResultsViewActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberObject.getBaseEntityId());
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem addMember = menu.findItem(org.smartregister.chw.core.R.id.add_member);
        if (addMember != null) {
            addMember.setVisible(false);
        }

        getMenuInflater().inflate(org.smartregister.chw.core.R.menu.other_member_menu, menu);

        CommonRepository commonRepository = org.smartregister.family.util.Utils.context().commonrepository(org.smartregister.family.util.Utils.metadata().familyMemberRegister.tableName);

        // show profile view
        CommonPersonObject personObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        CommonPersonObjectClient commonPersonObject = new CommonPersonObjectClient(personObject.getCaseId(), personObject.getDetails(), "");
        commonPersonObject.setColumnmaps(personObject.getColumnmaps());

        String gender = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.GENDER, false);
        menu.findItem(R.id.action_location_info).setVisible(true);
        menu.findItem(R.id.action_pregnancy_out_come).setVisible(false);
        menu.findItem(R.id.action_remove_member).setVisible(true);
        if (isOfReproductiveAge(commonPersonObject, gender) && gender.equalsIgnoreCase("female") && !AncDao.isANCMember(memberObject.getBaseEntityId())) {
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

        if (isOfReproductiveAge(commonPersonObject, gender))
            menu.findItem(R.id.action_fp_initiation).setVisible(HealthFacilityApplication.getApplicationFlavor().hasFp());

        if (isOfReproductiveAge(commonPersonObject, gender) && gender.equalsIgnoreCase("female"))
            menu.findItem(R.id.action_fp_ecp_provision).setVisible(HealthFacilityApplication.getApplicationFlavor().hasFp());


        if (HealthFacilityApplication.getApplicationFlavor().hasLD()) {
            menu.findItem(R.id.action_ld_registration).setVisible(isOfReproductiveAge(commonPersonObject, gender) && gender.equalsIgnoreCase("female") && !LDDao.isRegisteredForLD(memberObject.getBaseEntityId()));
        }

        if (gender.equalsIgnoreCase("male") && HealthFacilityApplication.getApplicationFlavor().hasVmmc())
            menu.findItem(R.id.action_vmmc_registration).setVisible(!VmmcDao.isRegisteredForVmmc(memberObject.getBaseEntityId()));

        if (HealthFacilityApplication.getApplicationFlavor().hasHivst()) {
            String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
            int age = Utils.getAgeFromDate(dob);
            menu.findItem(R.id.action_hivst_registration).setVisible(!HivstDao.isRegisteredForHivst(memberObject.getBaseEntityId()) && age >= 15);
        }
        if (HealthFacilityApplication.getApplicationFlavor().hasKvpPrEP()) {
            String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
            int age = Utils.getAgeFromDate(dob);
            menu.findItem(R.id.action_kvp_registration).setVisible(!KvpDao.isRegisteredForKvp(memberObject.getBaseEntityId()) && age >= 15);
        }
        if (HealthFacilityApplication.getApplicationFlavor().hasSbc()) {
            String dob = Utils.getValue(commonPersonObject.getColumnmaps(), DBConstants.KEY.DOB, false);
            int age = Utils.getAgeFromDate(dob);
            menu.findItem(R.id.action_sbc_registration).setVisible(!SbcDao.isRegisteredForSbc(memberObject.getBaseEntityId()) && age >= 10);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == org.smartregister.chw.core.R.id.action_pregnancy_confirmation) {
            startPregnancyConfirmation();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_ld_registration) {
            startLDRegistration();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_pmtct_register) {
            startPmtctRegisration();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_anc_registration) {
            startAncTransferInRegistration();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_location_info) {
            JSONObject preFilledForm = getAutoPopulatedJsonEditFormString(CoreConstants.JSON_FORM.getFamilyDetailsRegister(), this, getFamilyRegistrationDetails(), Utils.metadata().familyRegister.updateEventType);
            if (preFilledForm != null) startFormActivity(preFilledForm);
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_hivst_registration) {
            startHivstRegistration();
            return true;
        } else if (itemId == org.smartregister.chw.core.R.id.action_kvp_registration) {
            startKvpRegistration();
        } else if (itemId == org.smartregister.chw.core.R.id.action_registration) {
            if (UpdateDetailsUtil.isIndependentClient(memberObject.getBaseEntityId())) {
                startFormForEdit(org.smartregister.chw.core.R.string.registration_info,
                        CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
            } else {
                startFormForEdit(org.smartregister.chw.core.R.string.edit_member_form_title,
                        CoreConstants.JSON_FORM.getFamilyMemberRegister());
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void startPregnancyConfirmation() {
        AncRegisterActivity.startAncRegistrationActivity(this, memberObject.getBaseEntityId(), "", CoreConstants.JSON_FORM.ANC_PREGNANCY_CONFIRMATION, null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
    }

    protected void startAncTransferInRegistration() {
        AncRegisterActivity.startAncRegistrationActivity(this, memberObject.getBaseEntityId(), "", org.smartregister.chw.hf.utils.Constants.JSON_FORM.ANC_TRANSFER_IN_REGISTRATION, null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName());
    }


    protected void startPmtctRegisration() {
        PncRegisterActivity.startPncRegistrationActivity(this, memberObject.getBaseEntityId(), "", org.smartregister.chw.hf.utils.Constants.JsonForm.getPmtctRegistrationForClientsPostPnc(), null, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyName(), null, false);
    }


    protected void startLDRegistration() {
        try {
            LDRegistrationFormActivity.startMe(this, memberObject.getBaseEntityId(), false, getName(getName(memberObject.getFirstName(), memberObject.getMiddleName()), memberObject.getLastName()), String.valueOf(memberObject.getAge()));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected void startHivstRegistration() {
        HivstRegisterActivity.startHivstRegistrationActivity(this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getAge());
    }


    protected void startKvpRegistration() {

        if (memberObject.getGender().equalsIgnoreCase(org.smartregister.chw.hf.utils.Constants.GENDER.MALE)) {
            KvpRegisterActivity.startKvpScreeningMale(this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getAge());
        }
        if (memberObject.getGender().equalsIgnoreCase(org.smartregister.chw.hf.utils.Constants.GENDER.FEMALE)) {
            KvpRegisterActivity.startKvpScreeningFemale(this, memberObject.getBaseEntityId(), memberObject.getGender(), memberObject.getAge());
        }
    }

    private boolean isOfReproductiveAge(CommonPersonObjectClient commonPersonObject, String gender) {
        if (gender.equalsIgnoreCase("Female")) {
            return Utils.isMemberOfReproductiveAge(commonPersonObject, 10, 55);
        } else if (gender.equalsIgnoreCase("Male")) {
            return Utils.isMemberOfReproductiveAge(commonPersonObject, 15, 49);
        } else {
            return false;
        }
    }

    protected @NotNull CommonPersonObjectClient getFamilyRegistrationDetails() {
        CommonRepository commonRepository = CoreReferralUtils.getCommonRepository(Utils.metadata().familyRegister.tableName);
        CommonPersonObject personObject = commonRepository.findByBaseEntityId(memberObject.getFamilyBaseEntityId());
        String caseId = personObject.getCaseId();
        CommonPersonObjectClient commonPersonObjectClient = new CommonPersonObjectClient(caseId, personObject.getDetails(), "");
        commonPersonObjectClient.setColumnmaps(personObject.getColumnmaps());
        commonPersonObjectClient.setDetails(personObject.getDetails());
        return commonPersonObjectClient;
    }

    public void startFormActivity(JSONObject jsonForm) {
        Form form = new Form();
        form.setWizard(false);

        Intent intent = new Intent(this, org.smartregister.family.util.Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    public void startFormForEdit(Integer title_resource, String formName) {
        try {
            JSONObject form = null;
            boolean isPrimaryCareGiver = memberObject.getPrimaryCareGiver().equals(memberObject.getBaseEntityId());
            String titleString = title_resource != null ? getResources().getString(title_resource) : null;

           if (formName.equals(CoreConstants.JSON_FORM.getFamilyMemberRegister())) {

                String eventName = Utils.metadata().familyMemberRegister.updateEventType;

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new FamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString, eventName, memberObject.getUniqueId()));

                form = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getFamilyMemberRegister());
            } else if (formName.equals(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm())) {
                String eventName = Utils.metadata().familyMemberRegister.updateEventType;

                NativeFormsDataBinder binder = new NativeFormsDataBinder(this, memberObject.getBaseEntityId());
                binder.setDataLoader(new FamilyMemberDataLoader(memberObject.getFamilyName(), isPrimaryCareGiver, titleString, eventName, memberObject.getUniqueId()));

                form = binder.getPrePopulatedForm(CoreConstants.JSON_FORM.getAllClientUpdateRegistrationInfoForm());
            }

            startFormActivity(form);
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    protected void removeIndividualProfile() {
        CommonRepository commonRepository = org.smartregister.family.util.Utils.context().commonrepository(org.smartregister.family.util.Utils.metadata().familyMemberRegister.tableName);
        final CommonPersonObject commonPersonObject = commonRepository.findByBaseEntityId(memberObject.getBaseEntityId());
        final CommonPersonObjectClient client = new CommonPersonObjectClient(commonPersonObject.getCaseId(), commonPersonObject.getDetails(), "");
        client.setColumnmaps(commonPersonObject.getColumnmaps());

        IndividualProfileRemoveActivity.startIndividualProfileActivity(CecapMemberProfileActivity.this,
                client, memberObject.getFamilyBaseEntityId(), memberObject.getFamilyHead(), memberObject.getPrimaryCareGiver(), FamilyRegisterActivity.class.getCanonicalName());
    }
}
