package org.smartregister.chw.hf.fragment;

import android.view.View;

import org.smartregister.chw.core.fragment.CoreHtsRegisterFragment;
import org.smartregister.chw.hf.activity.HivTestingServicesMemberProfileActivity;
import org.smartregister.chw.hf.activity.HivTestingServicesRegisterActivity;
import org.smartregister.chw.hf.model.HivTestingServicesRegisterFragmentModel;
import org.smartregister.chw.hts.presenter.BaseHtsRegisterFragmentPresenter;

public class HivTestingServicesRegisterFragment extends CoreHtsRegisterFragment {
    @Override
    protected void openProfile(String baseEntityId) {
        HivTestingServicesMemberProfileActivity.startMe(getActivity(), baseEntityId);
    }

    @Override
    protected void initializePresenter() {
        if (getActivity() == null) {
            return;
        }
        presenter = new BaseHtsRegisterFragmentPresenter(this, new HivTestingServicesRegisterFragmentModel(), null);
    }

    @Override
    protected void startSampleRegistration(View view) {
        HivTestingServicesRegisterActivity.startSampleRegistration(getActivity());
    }
}
