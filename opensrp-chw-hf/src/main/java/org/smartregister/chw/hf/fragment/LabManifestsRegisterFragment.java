package org.smartregister.chw.hf.fragment;

import static org.smartregister.util.Utils.showShortToast;

import android.content.Intent;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.core.fragment.CoreLabManifestsRegisterFragment;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.activity.LabManifestDetailsActivity;
import org.smartregister.chw.hf.activity.LabRegisterActivity;
import org.smartregister.chw.lab.activity.CreateManifestActivity;
import org.smartregister.chw.lab.dao.LabDao;
import org.smartregister.chw.lab.model.BaseLabManifestsRegisterFragmentModel;
import org.smartregister.chw.lab.presenter.BaseLabManifestsRegisterFragmentPresenter;
import org.smartregister.chw.lab.util.Constants;

import java.util.UUID;

public class LabManifestsRegisterFragment extends CoreLabManifestsRegisterFragment {
    @Override
    protected void initializePresenter() {
        presenter = new BaseLabManifestsRegisterFragmentPresenter(this, new BaseLabManifestsRegisterFragmentModel(), null);
    }

    @Override
    public void createHvlManifest() {
        if (LabDao.getTestSamplesRequestsNotInManifests(Constants.MANIFEST_TYPE.HVL).isEmpty()) {
            showShortToast(getActivity(), getActivity().getString(R.string.no_test_samples));
        } else if (StringUtils.isNotBlank(LabDao.getDestinationHubName())) {
            Intent intent = new Intent(getActivity(), CreateManifestActivity.class);

            intent.putExtra(Constants.ACTIVITY_PAYLOAD.MANIFEST_TYPE, Constants.MANIFEST_TYPE.HVL);
            getActivity().startActivity(intent);
        } else {
            showShortToast(getActivity(), getActivity().getString(R.string.set_destination_hub_lab));
        }
    }

    @Override
    public void createHeidManifest() {
        if (LabDao.getTestSamplesRequestsNotInManifests(Constants.MANIFEST_TYPE.HEID).isEmpty()) {
            showShortToast(getActivity(), getActivity().getString(R.string.no_test_samples));
        } else if (StringUtils.isNotBlank(LabDao.getDestinationHubName())) {
            Intent intent = new Intent(getActivity(), CreateManifestActivity.class);

            intent.putExtra(Constants.ACTIVITY_PAYLOAD.MANIFEST_TYPE, Constants.MANIFEST_TYPE.HEID);
            getActivity().startActivity(intent);
        } else {
            showShortToast(getActivity(), getActivity().getString(R.string.set_destination_hub_lab));
        }
    }

    @Override
    public void setManifestSettings() {
        LabRegisterActivity.startLabRegisterActivity(getActivity(), UUID.randomUUID().toString(), org.smartregister.chw.lab.util.Constants.FORMS.LAB_MANIFEST_SETTINGS);
    }

    protected void openProfile(String batchNumber) {
        LabManifestDetailsActivity.startProfileActivity(getActivity(), batchNumber);
    }

}
