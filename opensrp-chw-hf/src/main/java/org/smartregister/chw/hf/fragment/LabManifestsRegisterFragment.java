package org.smartregister.chw.hf.fragment;

import org.smartregister.chw.core.fragment.CoreLabManifestsRegisterFragment;
import org.smartregister.chw.lab.model.BaseLabManifestsRegisterFragmentModel;
import org.smartregister.chw.lab.presenter.BaseLabManifestsRegisterFragmentPresenter;

public class LabManifestsRegisterFragment extends CoreLabManifestsRegisterFragment {

    @Override
    protected void initializePresenter() {
        presenter = new BaseLabManifestsRegisterFragmentPresenter(this, new BaseLabManifestsRegisterFragmentModel(), null);
    }
}
