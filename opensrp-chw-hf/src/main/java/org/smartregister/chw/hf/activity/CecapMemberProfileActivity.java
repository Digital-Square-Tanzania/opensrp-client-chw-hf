package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.cecap.CecapLibrary;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.cecap.domain.MemberObject;
import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.chw.cecap.util.VisitUtils;
import org.smartregister.chw.core.activity.CoreCecapMemberProfileActivity;
import org.smartregister.chw.core.listener.OnClickFloatingMenu;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.custom_view.CecapFloatingMenu;

import timber.log.Timber;

public class CecapMemberProfileActivity extends CoreCecapMemberProfileActivity {

    public static void startMe(Activity activity, String baseEntityID) {
        Intent intent = new Intent(activity, CecapMemberProfileActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityID);
        activity.startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void recordCecap(MemberObject memberObject) {
        if (textViewRecordCecap.getText().equals(getString(R.string.cecap_via_approach)))
            CecapVisitActivity.startMe(this, memberObject.getBaseEntityId(), false, true);
        else
            CecapVisitActivity.startMe(this, memberObject.getBaseEntityId(), false, false);
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

        if (hasPendingVia(memberObject.getBaseEntityId())) {
            textViewRecordCecap.setVisibility(View.VISIBLE);
            textViewRecordCecap.setText(getString(R.string.cecap_via_approach));
        } else if (CecapDao.hasTestResults(memberObject.getBaseEntityId())) {
            textViewRecordCecap.setVisibility(View.GONE);
        } else {
            textViewRecordCecap.setVisibility(View.VISIBLE);
            textViewRecordCecap.setText(getString(R.string.record_cecap));
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

    public boolean hasPendingVia(String baseEntityID) {

        String screenTestPerformed = CecapDao.getScreenTestPerformed(baseEntityID);
        boolean hasPendingVia = false;

        if (screenTestPerformed != null && screenTestPerformed.contains("hpv_dna")) {
            String hpvFindings = CecapDao.getHpvFindings(baseEntityID);
            if (hpvFindings != null && hpvFindings.equals("positive")) {
                hasPendingVia = true;
            }
        }
        return hasPendingVia;
    }
}
