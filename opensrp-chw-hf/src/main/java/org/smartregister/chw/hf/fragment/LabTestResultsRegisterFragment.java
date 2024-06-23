package org.smartregister.chw.hf.fragment;

import android.os.Bundle;
import android.view.View;

import org.smartregister.chw.core.fragment.CoreLabRequestsRegisterFragment;
import org.smartregister.chw.hf.activity.LabTestRequestDetailsActivity;
import org.smartregister.chw.hf.presenter.LabTestRequestsRegisterFragmentPresenter;
import org.smartregister.chw.lab.model.BaseLabRequestsRegisterFragmentModel;

import timber.log.Timber;

public class LabTestResultsRegisterFragment extends CoreLabRequestsRegisterFragment {
    public static final String BASE_ENTITY_ID = "BASE_ENTITY_ID";

    private String baseEntityId;

    public static LabTestResultsRegisterFragment newInstance(String baseEntityId) {
        LabTestResultsRegisterFragment labTestResultsRegisterFragment = new LabTestResultsRegisterFragment();
        Bundle b = new Bundle();
        b.putString(BASE_ENTITY_ID, baseEntityId);
        labTestResultsRegisterFragment.setArguments(b);
        return labTestResultsRegisterFragment;
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
        presenter = new LabTestRequestsRegisterFragmentPresenter(this, new BaseLabRequestsRegisterFragmentModel(), null);
    }

    protected void openProfile(String baseEntityId, String sampleId) {
        LabTestRequestDetailsActivity.startProfileActivity(getActivity(), baseEntityId.toLowerCase(), sampleId, true);
    }

    public void setupViews(View view) {
        try {
            super.setupViews(view);
        } catch (Exception e) {
            Timber.e(e);
        }

        toolbar.setVisibility(View.GONE);
        view.findViewById(org.smartregister.lab.R.id.tab_layout).setVisibility(View.GONE);
        view.findViewById(org.smartregister.R.id.search_bar_layout).setVisibility(View.GONE);

        customGroupFilter = ((LabTestRequestsRegisterFragmentPresenter) presenter()).getTestSamplesWithResultsQuery(baseEntityId.toLowerCase());
        filterandSortExecute();
    }

}
