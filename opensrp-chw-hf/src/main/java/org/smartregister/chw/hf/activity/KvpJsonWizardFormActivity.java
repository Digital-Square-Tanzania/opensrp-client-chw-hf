package org.smartregister.chw.hf.activity;

import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.vijay.jsonwizard.R;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.utils.Utils;

import org.smartregister.chw.hf.fragment.KvpScreeningJsonFormFragment;
import org.smartregister.family.activity.FamilyWizardFormActivity;

import java.util.List;

public class KvpJsonWizardFormActivity extends FamilyWizardFormActivity {
    @Override
    public synchronized void initializeFormFragment() {
        KvpScreeningJsonFormFragment formFragment = KvpScreeningJsonFormFragment.getFormFragment(JsonFormConstants.FIRST_STEP_NAME);
        getSupportFragmentManager().beginTransaction()
                .add(com.vijay.jsonwizard.R.id.container, formFragment).commitAllowingStateLoss();
    }

    @Override
    protected void toggleViewVisibility(View view, boolean visible, boolean popup) {
        super.toggleViewVisibility(view, visible, popup);
        if (visible && !popup && view instanceof ViewGroup && "client_group".equals(view.getTag(R.id.key))) {
            selectSoleEnrollmentOption((ViewGroup) view);
        }
    }

    static void selectSoleEnrollmentOption(ViewGroup group) {
        List<RadioButton> options = Utils.getRadioButtons(group);
        if (options.size() == 1) {
            RadioButton option = options.get(0);
            Object key = option.getTag(R.id.childKey);
            // Use the widget after relevance makes it available so Save validates the checked radio.
            if (option.isEnabled() && !option.isChecked() && ("msm".equals(key) || "fsw".equals(key))) {
                option.setChecked(true);
            }
        }
    }
}
