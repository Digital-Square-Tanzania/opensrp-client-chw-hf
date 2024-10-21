package org.smartregister.chw.hf.activity;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;

import org.smartregister.chw.hf.fragment.KvpScreeningJsonFormFragment;
import org.smartregister.family.activity.FamilyWizardFormActivity;

public class KvpScreeningJsonWizardFormActivity extends FamilyWizardFormActivity {
    @Override
    public synchronized void initializeFormFragment() {
        KvpScreeningJsonFormFragment formFragment = KvpScreeningJsonFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
//        JsonWizardFormFragment formFragment = JsonWizardFormFragment.getFormFragment("step1");
        getSupportFragmentManager().beginTransaction()
                .add(com.vijay.jsonwizard.R.id.container, formFragment).commitAllowingStateLoss();
    }
}
