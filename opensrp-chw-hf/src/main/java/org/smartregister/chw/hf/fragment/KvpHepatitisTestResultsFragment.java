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
import org.smartregister.chw.core.utils.CoreJsonFormUtils;
import org.smartregister.chw.hf.activity.KvpTestResultsViewActivity;
import org.smartregister.chw.hf.model.KvpHepatitisTestResultsFragmentModel;
import org.smartregister.chw.hf.presenter.KvpHepatitisTestResultsFragmentPresenter;
import org.smartregister.chw.kvp.fragment.BaseTestResultsFragment;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.DBConstants;
import org.smartregister.commonregistry.CommonPersonObjectClient;
import org.smartregister.util.Utils;

import timber.log.Timber;

public class KvpHepatitisTestResultsFragment extends BaseTestResultsFragment {

    public static final String BASE_ENTITY_ID = "BASE_ENTITY_ID";
    private long mLastClickTime = 0;
    private String baseEntityId;

    public static KvpHepatitisTestResultsFragment newInstance(String baseEntityId) {
        KvpHepatitisTestResultsFragment hvlResultsFragment = new KvpHepatitisTestResultsFragment();
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
        presenter = new KvpHepatitisTestResultsFragmentPresenter(baseEntityId, this, new KvpHepatitisTestResultsFragmentModel(), null);
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
        String testType = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.HEPATITIS_TEST_TYPE, false);

        if (testType.contains("hep_b")) {
            String baseEntityId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
            String entityId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.ENTITY_ID, false);

            try {
                JSONObject jsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireContext(), Constants.FORMS.HEPATITIS_B_RESULTS);
                assert jsonObject != null;
                KvpTestResultsViewActivity.startResultsForm(getContext(), jsonObject.toString(), baseEntityId, entityId);
            } catch (JSONException e) {
                Timber.e(e);
            }
        } else if (testType.contains("hep_c")) {
            String baseEntityId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
            String entityId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.ENTITY_ID, false);

            try {
                JSONObject jsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireContext(), Constants.FORMS.HEPATITIS_C_RESULTS);
                assert jsonObject != null;
                KvpTestResultsViewActivity.startResultsForm(getContext(), jsonObject.toString(), baseEntityId, entityId);
            } catch (JSONException e) {
                Timber.e(e);
            }
        }


    }


    private void openEditResultsForm(CommonPersonObjectClient client) {
        String baseEntityId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.BASE_ENTITY_ID, false);
        String formSubmissionId = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.ENTITY_ID, false);
        try {
            JSONObject jsonObject = null;
            String testType = Utils.getValue(client.getColumnmaps(), DBConstants.KEY.HEPATITIS_TEST_TYPE, false);
            if (testType.contains("hep_b")) {
                jsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireContext(), Constants.FORMS.HEPATITIS_B_RESULTS);
            } else if (testType.contains("hep_c")) {
                jsonObject = (new FormUtils()).getFormJsonFromRepositoryOrAssets(requireContext(), Constants.FORMS.HEPATITIS_C_RESULTS);
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

            KvpTestResultsViewActivity.startResultsForm(getContext(), jsonObject.toString(), baseEntityId, formSubmissionId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
