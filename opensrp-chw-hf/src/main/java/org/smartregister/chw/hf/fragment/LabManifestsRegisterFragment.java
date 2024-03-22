package org.smartregister.chw.hf.fragment;

import android.content.Intent;

import org.smartregister.chw.core.fragment.CoreLabManifestsRegisterFragment;
import org.smartregister.chw.lab.activity.CreateManifestActivity;
import org.smartregister.chw.lab.model.BaseLabManifestsRegisterFragmentModel;
import org.smartregister.chw.lab.presenter.BaseLabManifestsRegisterFragmentPresenter;
import org.smartregister.chw.lab.util.Constants;

public class LabManifestsRegisterFragment extends CoreLabManifestsRegisterFragment {

    @Override
    protected void initializePresenter() {
        presenter = new BaseLabManifestsRegisterFragmentPresenter(this, new BaseLabManifestsRegisterFragmentModel(), null);
    }

    @Override
    public void createHvlManifest() {
        Intent intent = new Intent(getActivity(), CreateManifestActivity.class);

        intent.putExtra(Constants.ACTIVITY_PAYLOAD.MANIFEST_TYPE, Constants.MANIFEST_TYPE.HVL);
        getActivity().startActivity(intent);
    }

    @Override
    public void createHeidManifest() {
        Intent intent = new Intent(getActivity(), CreateManifestActivity.class);

        intent.putExtra(Constants.ACTIVITY_PAYLOAD.MANIFEST_TYPE, Constants.MANIFEST_TYPE.HEID);
        getActivity().startActivity(intent);
    }

    @Override
    public void setManifestSettings() {
        Intent intent = new Intent(getActivity(), CreateManifestActivity.class);

        intent.putExtra(Constants.ACTIVITY_PAYLOAD.MANIFEST_TYPE, Constants.MANIFEST_TYPE.HEID);
        getActivity().startActivity(intent);
    }
}
