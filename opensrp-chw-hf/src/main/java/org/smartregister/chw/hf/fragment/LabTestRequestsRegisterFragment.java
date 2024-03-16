package org.smartregister.chw.hf.fragment;

import org.smartregister.chw.core.fragment.CoreLabRequestsRegisterFragment;
import org.smartregister.chw.hf.presenter.LabTestRequestsRegisterFragmentPresenter;
import org.smartregister.chw.lab.model.BaseLabRequestsRegisterFragmentModel;

public class LabTestRequestsRegisterFragment extends CoreLabRequestsRegisterFragment {
    @Override
    protected void initializePresenter() {
        presenter = new LabTestRequestsRegisterFragmentPresenter(this, new BaseLabRequestsRegisterFragmentModel(), null);
    }

}
