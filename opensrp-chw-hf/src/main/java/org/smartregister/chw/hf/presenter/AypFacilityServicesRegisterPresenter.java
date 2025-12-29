package org.smartregister.chw.hf.presenter;

import org.smartregister.chw.ayp.contract.AypRegisterFragmentContract;
import org.smartregister.chw.ayp.presenter.BaseAypRegisterFragmentPresenter;
import org.smartregister.chw.ayp.util.Constants;
import org.smartregister.chw.ayp.util.DBConstants;

public class AypFacilityServicesRegisterPresenter extends BaseAypRegisterFragmentPresenter {

    public AypFacilityServicesRegisterPresenter(AypRegisterFragmentContract.View view, AypRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        return " " + getMainTable() + ".is_closed = 0 ";
    }

    @Override
    public String getDefaultSortQuery() {
        return getMainTable() + "." + DBConstants.KEY.LAST_INTERACTED_WITH + " DESC ";
    }

    @Override
    public String getMainTable() {
        return Constants.TABLES.AYP_FACILITY_SCREENING;
    }
}
