package org.smartregister.chw.hf.presenter;

import org.smartregister.chw.cdp.contract.BaseOrdersRegisterFragmentContract;
import org.smartregister.chw.cdp.presenter.BaseOrdersRegisterFragmentPresenter;
import org.smartregister.chw.cdp.util.Constants;
import org.smartregister.chw.cdp.util.DBConstants;
import org.smartregister.chw.lab.contract.BaseLabRegisterFragmentContract;
import org.smartregister.chw.lab.presenter.BaseLabManifestsRegisterFragmentPresenter;
import org.smartregister.chw.lab.presenter.BaseLabRequestsRegisterFragmentPresenter;

public class LabTestRequestsRegisterFragmentPresenter extends BaseLabRequestsRegisterFragmentPresenter {
    public LabTestRequestsRegisterFragmentPresenter(BaseLabRegisterFragmentContract.View view, BaseLabRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        String providerId = org.smartregister.Context.getInstance().allSharedPreferences().fetchRegisteredANM();
        String userLocationId = org.smartregister.Context.getInstance().allSharedPreferences().fetchUserLocalityId(providerId);

        return super.getMainCondition();
    }

}
