package org.smartregister.chw.hf.fragment;

import static com.vijay.jsonwizard.rules.RuleConstant.CALCULATION;
import static org.smartregister.chw.pmtct.util.DBConstants.KEY.FORM_SUBMISSION_ID;
import static org.smartregister.util.JsonFormUtils.FIELDS;
import static org.smartregister.util.JsonFormUtils.STEP1;

import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import com.vijay.jsonwizard.utils.FormUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.cecap.fragment.BaseTestResultsFragment;
import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.chw.cecap.util.DBConstants;
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hf.activity.CecapTestResultsViewActivity;
import org.smartregister.chw.hf.model.CecapTestResultsFragmentModel;
import org.smartregister.chw.hf.presenter.CecapTestResultsFragmentPresenter;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.util.Utils;

import timber.log.Timber;

public class CecapTestResultsFragment extends BaseTestResultsFragment {

    public static final String BASE_ENTITY_ID = "BASE_ENTITY_ID";
    private long mLastClickTime = 0;
    private String baseEntityId;

    public static CecapTestResultsFragment newInstance(String baseEntityId) {
        CecapTestResultsFragment hvlResultsFragment = new CecapTestResultsFragment();
        Bundle b = new Bundle();
        b.putString(BASE_ENTITY_ID, baseEntityId);
        hvlResultsFragment.setArguments(b);
        return hvlResultsFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (getArguments() != null) {
            this.baseEntityId = getArguments().getString(BASE_ENTITY_ID);
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new CecapTestResultsFragmentPresenter(baseEntityId, this, new CecapTestResultsFragmentModel(), null);
    }

    @Override
    protected void onViewClicked(View view) {
        // mis-clicking prevention, using threshold of 5000 ms
        if (SystemClock.elapsedRealtime() - mLastClickTime < 5000) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();
        if (getActivity() == null || !(view.getTag() instanceof CommonPersonObjectClient)) {
            return;
        }
        CommonPersonObjectClient client = (CommonPersonObjectClient) view.getTag();
        if (view.getTag(org.smartregister.pmtct.R.id.VIEW_ID) == CLICK_VIEW_NORMAL) {
            if (((TextView) view).getText().toString().equalsIgnoreCase("edit")) {
                CommonPersonObjectClient pc = (CommonPersonObjectClient) view.getTag();
                if (pc != null) {
                    openEditResultsForm(client);
                } else
                    openResultsForm(client);
            } else {
                openResultsForm(client);
            }
        }
    }

    @Override
    public void openResultsForm(CommonPersonObjectClient client) {
        String testType = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.SCREENING_TEST_PERFORMED, false);

        if (testType.contains("hpv_dna")) {
            String sampleId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.HPV_DNA_SAMPLE_ID, false);
            String baseEntityId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
            String formSubmissionId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.ENTITY_ID, false);

            try {
                JSONObject jsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireContext(), Constants.FORMS.HPV_DNA_FINDINGS);
                assert jsonObject != null;
                jsonObject.getJSONObject(STEP1).getJSONArray(FIELDS).getJSONObject(0).put("value", sampleId);
                CecapTestResultsViewActivity.startResultsForm(getContext(), jsonObject.toString(), baseEntityId, formSubmissionId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (testType.contains("pap")) {
            String sampleId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.PAP_SMEAR_SAMPLE_ID, false);
            String baseEntityId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
            String formSubmissionId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.ENTITY_ID, false);

            try {
                JSONObject jsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireContext(), Constants.FORMS.PAP_FINDINGS);
                assert jsonObject != null;
                jsonObject.getJSONObject(STEP1).getJSONArray(FIELDS).getJSONObject(0).put("value", sampleId);
                CecapTestResultsViewActivity.startResultsForm(getContext(), jsonObject.toString(), baseEntityId, formSubmissionId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


    }


    private void openEditResultsForm(CommonPersonObjectClient client) {
        String baseEntityId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
        String formSubmissionId = Utils.getValue(client.getColumnmaps(), "form_submission_id", false);
        try {
            JSONObject jsonObject=null;
            String testType = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.SCREENING_TEST_PERFORMED, false);
            if (testType.contains("hpv_dna")) {
                 jsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireContext(),  Constants.FORMS.HPV_DNA_FINDINGS);
            }else if (testType.contains("pap")) {
                jsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireContext(), Constants.FORMS.PAP_FINDINGS);
            }

            assert jsonObject != null;

            try {
                JSONObject testResultDateObject = org.smartregister.util.JsonFormUtils.getFieldJSONObject(jsonObject.getJSONObject(STEP1).getJSONArray(FIELDS), "test_results_date");
                assert testResultDateObject != null;
                testResultDateObject.remove(CALCULATION);
            } catch (Exception e) {
                Timber.e(e);
            }

            jsonObject.put(FORM_SUBMISSION_ID, Utils.getValue(client.getColumnmaps(), FORM_SUBMISSION_ID, false));

            CoreJsonFormUtils.populateJsonForm(jsonObject, client.getColumnmaps());

            CecapTestResultsViewActivity.startResultsForm(getContext(), jsonObject.toString(), baseEntityId, formSubmissionId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}