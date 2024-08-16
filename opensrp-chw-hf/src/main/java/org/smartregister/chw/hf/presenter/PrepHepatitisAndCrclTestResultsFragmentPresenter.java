package org.smartregister.chw.hf.presenter;

import org.smartregister.chw.kvp.contract.TestResultsFragmentContract;
import org.smartregister.chw.kvp.presenter.BaseTestResultsFragmentPresenter;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.DBConstants;

public class PrepHepatitisAndCrclTestResultsFragmentPresenter extends BaseTestResultsFragmentPresenter {
    private String baseEntityId;

    public PrepHepatitisAndCrclTestResultsFragmentPresenter(String baseEntityId, TestResultsFragmentContract.View view, TestResultsFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
        this.baseEntityId = baseEntityId;
    }

    @Override
    public String getMainCondition() {
        return Constants.TABLES.PREP_HEPATITIS_AND_CRCL_TEST_RESULTS + "." + DBConstants.KEY.ENTITY_ID + " = '" + baseEntityId + "'";
    }

    public String getMainTable() {
        return Constants.TABLES.PREP_HEPATITIS_AND_CRCL_TEST_RESULTS;
    }
}
