package org.smartregister.chw.hf.presenter;

import org.smartregister.chw.kvp.contract.TestResultsFragmentContract;
import org.smartregister.chw.kvp.presenter.BaseTestResultsFragmentPresenter;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.chw.kvp.util.DBConstants;

public class KvpHepatitisTestResultsFragmentPresenter extends BaseTestResultsFragmentPresenter {
    private String baseEntityId;

    public KvpHepatitisTestResultsFragmentPresenter(String baseEntityId, TestResultsFragmentContract.View view, TestResultsFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
        this.baseEntityId = baseEntityId;
    }

    @Override
    public String getMainCondition() {
        return Constants.TABLES.KVP_HEPATITIS_TEST_RESULTS + "." + DBConstants.KEY.ENTITY_ID + " = '" + baseEntityId + "'";
    }
}
