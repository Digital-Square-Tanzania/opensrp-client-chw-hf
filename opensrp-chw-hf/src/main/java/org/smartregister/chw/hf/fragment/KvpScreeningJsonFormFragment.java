package org.smartregister.chw.hf.fragment;

import static com.vijay.jsonwizard.constants.JsonFormConstants.STEPNAME;
import static org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.google.android.material.snackbar.Snackbar;
import com.vijay.jsonwizard.fragments.JsonWizardFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.presenters.JsonFormFragmentPresenter;
import com.vijay.jsonwizard.widgets.CheckBoxFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.presenter.KvpScreeningJsonFormFragmentPresenter;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;

import timber.log.Timber;

public class KvpScreeningJsonFormFragment extends JsonWizardFormFragment {
    private Snackbar highRiskChecklistError;

    public static KvpScreeningJsonFormFragment getFormFragment(String stepName) {
        KvpScreeningJsonFormFragment jsonFormFragment = new KvpScreeningJsonFormFragment();
        Bundle bundle = new Bundle();
        bundle.putString(STEPNAME, stepName);
        jsonFormFragment.setArguments(bundle);
        return jsonFormFragment;
    }

    @Override
    protected JsonFormFragmentPresenter createPresenter() {
        return new KvpScreeningJsonFormFragmentPresenter(this, JsonFormInteractor.getInstance());
    }

    public void showHighRiskChecklistError(LinearLayout checklist, String message) {
        getJsonApi().getAppExecutors().mainThread().execute(() -> {
            if (getView() == null || !checklist.isAttachedToWindow() || CheckBoxFactory.isValid(checklist)) {
                return;
            }
            if (highRiskChecklistError == null || !highRiskChecklistError.isShownOrQueued()) {
                highRiskChecklistError = Snackbar.make(getMainView(), message, Snackbar.LENGTH_LONG);
                highRiskChecklistError.show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        dismissHighRiskChecklistError();
        super.onDestroyView();
    }

    public void dismissHighRiskChecklistError() {
        if (highRiskChecklistError != null) {
            highRiskChecklistError.dismiss();
            highRiskChecklistError = null;
        }
    }

    @Override
    public void finishWithResult(Intent result) {
        String step = requireArguments().getString(STEPNAME);
        boolean isHighRiskOutcome = MarpPregnancyScreening.ENROLLMENT_STEP.equals(step)
                || MarpPregnancyScreening.NO_GROUP_STEP.equals(step);
        if (isHighRiskOutcome && MarpPregnancyScreening.isIntakeStep(
                getJsonApi().getmJSONObject(), MarpPregnancyScreening.INTAKE_STEP)) {
            try {
                JSONObject form = new JSONObject(result.getStringExtra(JSON));
                if (!MarpPregnancyScreening.prepareEnrollmentResult(form, step)) {
                    showSnackBar(getString(R.string.marp_enrollment_result_invalid));
                    return;
                }
                result.putExtra(JSON, form.toString());
            } catch (JSONException e) {
                Timber.e("Unable to prepare the MARP high-risk enrollment result");
                showSnackBar(getString(R.string.marp_enrollment_result_invalid));
                return;
            }
        }
        super.finishWithResult(result);
    }
}
