package org.smartregister.chw.hf.presenter;

import org.smartregister.chw.lab.contract.BaseLabRegisterFragmentContract;
import org.smartregister.chw.lab.presenter.BaseLabRequestsRegisterFragmentPresenter;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.chw.lab.util.DBConstants;

public class LabTestRequestsRegisterFragmentPresenter extends BaseLabRequestsRegisterFragmentPresenter {
    public LabTestRequestsRegisterFragmentPresenter(BaseLabRegisterFragmentContract.View view, BaseLabRegisterFragmentContract.Model model, String viewConfigurationIdentifier) {
        super(view, model, viewConfigurationIdentifier);
    }

    @Override
    public String getMainCondition() {
        return Constants.TABLES.LAB_TEST_REQUESTS + "." + DBConstants.KEY.PATIENT_ID + " IS NOT NULL AND " + Constants.TABLES.LAB_TEST_REQUESTS + "." + DBConstants.KEY.SAMPLE_ID + " IS NOT NULL AND " + Constants.TABLES.LAB_TEST_REQUESTS + "." + DBConstants.KEY.SAMPLE_ID + " <> '' ";
    }

    public String getTestSamplesWithResultsQuery(String baseEntityId) {
        return Constants.TABLES.LAB_TEST_REQUESTS + "." + DBConstants.KEY.PATIENT_ID + " IS NOT NULL AND " + Constants.TABLES.LAB_TEST_REQUESTS + "." + DBConstants.KEY.RESULTS + " IS NOT NULL AND " + Constants.TABLES.LAB_TEST_REQUESTS + "." + DBConstants.KEY.RESULTS + " <> ''  AND entity_id = '" + baseEntityId + "' ";
    }

}
