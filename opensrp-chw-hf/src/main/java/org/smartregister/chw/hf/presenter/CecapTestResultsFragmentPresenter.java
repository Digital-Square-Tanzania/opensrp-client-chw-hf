package org.smartregister.chw.hf.presenter;

import org.smartregister.chw.cecap.contract.TestResultsFragmentContract;
import org.smartregister.chw.cecap.presenter.BaseTestResultsFragmentPresenter;
import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.chw.cecap.util.DBConstants;

public class CecapTestResultsFragmentPresenter extends BaseTestResultsFragmentPresenter {
    private String baseEntityId;

    public CecapTestResultsFragmentPresenter(String baseEntityId, TestResultsFragmentContract.View view, TestResultsFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
        this.baseEntityId = baseEntityId;
    }

    @Override
    public String getMainCondition() {
        return " (" + Constants.TABLES.CECAP_FOLLOW_UP + "." + DBConstants.KEY.HPV_DNA_SAMPLE_ID + " IS NOT NULL  OR " + Constants.TABLES.CECAP_FOLLOW_UP + "." + DBConstants.KEY.PAP_SMEAR_SAMPLE_ID + " IS NOT NULL )" +
                " AND " + Constants.TABLES.CECAP_FOLLOW_UP + "." + DBConstants.KEY.ENTITY_ID + " = '" + baseEntityId + "'";
    }
}
