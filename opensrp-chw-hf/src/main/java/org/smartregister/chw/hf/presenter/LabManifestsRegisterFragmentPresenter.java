package org.smartregister.chw.hf.presenter;

import org.smartregister.chw.cdp.util.Constants;
import org.smartregister.chw.cdp.util.DBConstants;
import org.smartregister.chw.lab.contract.BaseLabRegisterFragmentContract;
import org.smartregister.chw.lab.presenter.BaseLabManifestsRegisterFragmentPresenter;

public class LabManifestsRegisterFragmentPresenter extends BaseLabManifestsRegisterFragmentPresenter {
    public LabManifestsRegisterFragmentPresenter(BaseLabRegisterFragmentContract.View view, BaseLabRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        String providerId = org.smartregister.Context.getInstance().allSharedPreferences().fetchRegisteredANM();
        String userLocationId = org.smartregister.Context.getInstance().allSharedPreferences().fetchUserLocalityId(providerId);

        return super.getMainCondition() + " AND " + getMainTable() + "." + DBConstants.KEY.REQUEST_TYPE + " = '" + Constants.ORDER_TYPES.FACILITY_TO_FACILITY_ORDER + "'" +
                " AND " + getMainTable() + "." + DBConstants.KEY.LOCATION_ID + " = " + "'" + userLocationId + "'";
    }

}
