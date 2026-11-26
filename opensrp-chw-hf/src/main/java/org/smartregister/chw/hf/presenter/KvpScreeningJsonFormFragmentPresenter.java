package org.smartregister.chw.hf.presenter;

import static com.vijay.jsonwizard.utils.FormUtils.NATIIVE_FORM_DATE_FORMAT_PATTERN;
import static org.smartregister.chw.core.utils.CoreJsonFormUtils.ENCOUNTER_TYPE;
import static org.smartregister.client.utils.constants.JsonFormConstants.V_MAX;
import static org.smartregister.util.JsonFormUtils.FIELDS;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.app.ProgressDialog;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.interfaces.JsonApi;
import com.vijay.jsonwizard.presenters.JsonWizardFormFragmentPresenter;
import com.vijay.jsonwizard.rules.RulesEngineDateUtil;
import com.vijay.jsonwizard.utils.Utils;
import com.vijay.jsonwizard.utils.ValidationStatus;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.fragment.KvpScreeningJsonFormFragment;
import org.smartregister.chw.hf.utils.MarpAgywScreening;
import org.smartregister.chw.hf.utils.MarpDiscordantCoupleScreening;
import org.smartregister.chw.hf.utils.MarpEnrollmentScreening;
import org.smartregister.chw.hf.utils.MarpOtherGroupsScreening;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.smartregister.chw.hf.utils.MarpSexualBehaviourScreening;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.family.util.JsonFormUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class KvpScreeningJsonFormFragmentPresenter extends JsonWizardFormFragmentPresenter {
    private final KvpScreeningJsonFormFragment formFragment;
    private boolean cleanupAndExit = false;

    public KvpScreeningJsonFormFragmentPresenter(KvpScreeningJsonFormFragment formFragment, JsonFormInteractor jsonFormInteractor) {
        super(formFragment, jsonFormInteractor);
        this.formFragment = formFragment;
    }

    public static int monthsBetween(Date date) {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.setTime(date);

        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(new Date());

        int yearsDifference = currentCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
        int monthsDifference = currentCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);

        return (yearsDifference * 12) + monthsDifference;
    }

    @Override
    public void addFormElements() {
        addFormElements(getView().getArguments().getString(JsonFormConstants.STEPNAME));
    }

    void addFormElements(String stepName) {
        final ProgressDialog dialog = new ProgressDialog(formFragment.getContext());
        dialog.setCancelable(false);
        dialog.setTitle(formFragment.getContext().getString(com.vijay.jsonwizard.R.string.loading));
        dialog.setMessage(formFragment.getContext().getString(com.vijay.jsonwizard.R.string.loading_form_message));
        dialog.show();
        mStepName = stepName;

        JSONObject step = getView().getStep(mStepName);
        if (step == null) {
            return;
        }

        try {
            if (MarpAgywScreening.ENROLLMENT_STEP.equals(mStepName)) {
                MarpAgywScreening.refreshEligibility(formFragment.getJsonApi());
            }
            JSONObject currentJsonState = new JSONObject(getView().getCurrentJsonState());

            if (MarpEnrollmentScreening.isEnrollmentStep(currentJsonState, mStepName)) {
                MarpEnrollmentScreening.prepareStep(formFragment.getJsonApi(), currentJsonState, mStepName,
                        step, formFragment.getContext());
            } else if (Constants.EVENT_TYPE.KVP_REGISTRATION.equalsIgnoreCase(currentJsonState.getString(ENCOUNTER_TYPE))
                    && MarpPregnancyScreening.ENROLLMENT_STEP.equals(mStepName)) {
                JSONObject clientGroup = JsonFormUtils.getFieldJSONObject(step.optJSONArray(FIELDS), "client_group");
                clientGroup.put(VALUE, MarpPregnancyScreening.GROUP_CODE);
            } else if (currentJsonState.getString(ENCOUNTER_TYPE).equalsIgnoreCase(formFragment.getString(R.string.prep_initiation)) && mStepName.equals("step2")) {
                    JSONObject prepStatusObj = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step1").getJSONArray(FIELDS), "prep_status");
                    String prepStatus = "";
                    if (prepStatusObj.has(VALUE)) {
                        prepStatus = prepStatusObj.getString(VALUE);
                    }
                    JSONObject originalPrepInitiationDateObj = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step1").getJSONArray(FIELDS), "original_prep_initiation_date_for_continuing_clients");
                    String originalPrepInitiationDate = "";
                    if (originalPrepInitiationDateObj.has(VALUE)) {
                        originalPrepInitiationDate = originalPrepInitiationDateObj.getString(VALUE);
                    }

                    JSONObject prepInitiationDateObj = JsonFormUtils.getFieldJSONObject(step.getJSONArray(FIELDS), "prep_initiation_date");
                    if (prepStatus.equalsIgnoreCase("initiated")) {
                        prepInitiationDateObj.put(VALUE, new RulesEngineDateUtil().getDateToday());
                    } else if (prepStatus.equalsIgnoreCase("continuing") || prepStatus.equalsIgnoreCase("re_start")) {
                        prepInitiationDateObj.put(VALUE, originalPrepInitiationDate);
                    } else {
                        prepInitiationDateObj.remove(VALUE);
                    }

                    try {
                        JSONObject numberOfPillsObj = JsonFormUtils.getFieldJSONObject(step.getJSONArray(FIELDS), "prep_pills_number");
                        if (prepInitiationDateObj.has(VALUE)) {
                            int monthsBetween = monthsBetween(new SimpleDateFormat(
                                    NATIIVE_FORM_DATE_FORMAT_PATTERN, Locale.ENGLISH).parse(prepInitiationDateObj.getString(VALUE)));
                            if (monthsBetween >= 6) {
                                numberOfPillsObj.put(V_MAX, new JSONObject("{\"value\":\"90\",\"err\":\"Number of pills must be equal to or less than  90\"}"));
                            } else {
                                numberOfPillsObj.put(V_MAX, new JSONObject("{\"value\":\"30\",\"err\":\"Number of pills must be equal to or less than  30\"}"));
                            }
                        }
                    } catch (Exception e) {
                        Timber.e(e);
                    }
            }

        } catch (JSONException e) {
            Timber.e(e);
        }

        try {
            mStepDetails = new JSONObject(step.toString());
            formFragment.getJsonApi().setNextStep(mStepName);
        } catch (JSONException e) {
            Timber.e(e);
        }
        formFragment.getJsonApi().getAppExecutors().diskIO().execute(new Runnable() {
            @Override
            public void run() {
                //fragment has been detached when skipping steps
                if (getView() == null || cleanupAndExit) {
                    dismissDialog(dialog);
                    return;
                }
                final List<View> views = getInteractor()
                        .fetchFormElements(mStepName, formFragment, mStepDetails, getView().getCommonListener(),
                                false);
                if (cleanupAndExit) {
                    dismissDialog(dialog);
                    return;
                }
                formFragment.getJsonApi().initializeDependencyMaps();

                formFragment.getJsonApi().getAppExecutors().mainThread().execute(new Runnable() {
                    @Override
                    public void run() {
                        dismissDialog(dialog);
                        if (getView() != null && !cleanupAndExit) {
                            getView().addFormElements(views);
                            formFragment.getJsonApi().invokeRefreshLogic(null, false, null, null, mStepName, false);
                            if (formFragment.getJsonApi().skipBlankSteps()) {
                                Utils.checkIfStepHasNoSkipLogic(formFragment);
                                if (mStepName.equals(JsonFormConstants.STEP1) && !formFragment.getJsonApi().isPreviousPressed()) {
                                    formFragment.getJsonApi().getAppExecutors().diskIO().execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            formFragment.skipLoadedStepsOnNextPressed();
                                        }
                                    });
                                }
                            }
                            String next = mStepDetails.optString(JsonFormConstants.NEXT);
                            formFragment.getJsonApi().setNextStep(next);
                        }
                    }
                });
            }
        });
    }

    private void dismissDialog(ProgressDialog dialog) {
        if (dialog.isShowing()) {
            dialog.dismiss();
        }

    }

    @Override
    public void cleanUp() {
        cleanupAndExit = true;
        super.cleanUp();
    }

    @Override
    public boolean onNextClick(LinearLayout mainViews) {
        if (MarpPregnancyScreening.HIGH_RISK_STEP.equals(mStepName)) {
            String address = mStepName + ":" + MarpPregnancyScreening.CHECKLIST_FIELD;
            View checklist = formFragment.getJsonApi().getFormDataView(address);
            if (checklist instanceof LinearLayout) {
                ValidationStatus status = validate(formFragment, checklist, true);
                if (!status.isValid()) {
                    formFragment.showHighRiskChecklistError((LinearLayout) checklist, status.getErrorMessage());
                    return false;
                }
            }
        }
        String checklistKey = getRequiredChecklistKey();
        if (checklistKey != null) {
            View checklist = formFragment.getJsonApi().getFormDataView(mStepName + ":" + checklistKey);
            if (checklist instanceof LinearLayout && checklist.isShown()) {
                ValidationStatus status = validate(formFragment, checklist, false);
                if (!status.isValid()) {
                    formFragment.showChecklistError((LinearLayout) checklist, status.getErrorMessage());
                    return false;
                }
            }
        }
        return super.onNextClick(mainViews);
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
        super.onCheckedChanged(button, isChecked);
        String checklistKey = getRequiredChecklistKey();
        Object changedKey = button.getTag(com.vijay.jsonwizard.R.id.key);
        boolean changedSchool = MarpAgywScreening.CHECKLIST_FIELD.equals(checklistKey)
                && MarpAgywScreening.SCHOOL_FIELD.equals(changedKey);
        if (isChecked && checklistKey != null && (checklistKey.equals(changedKey) || changedSchool)) {
            formFragment.dismissChecklistError();
        }
        if (MarpPregnancyScreening.HIGH_RISK_STEP.equals(mStepName) && isChecked
                && MarpPregnancyScreening.CHECKLIST_FIELD.equals(button.getTag(com.vijay.jsonwizard.R.id.key))) {
            formFragment.dismissHighRiskChecklistError();
        }
    }

    private String getRequiredChecklistKey() {
        JSONObject form = formFragment.getJsonApi().getmJSONObject();
        if (MarpAgywScreening.isScreeningStep(form, mStepName)) {
            return MarpAgywScreening.CHECKLIST_FIELD;
        }
        if (MarpOtherGroupsScreening.isScreeningStep(form, mStepName)) {
            return MarpOtherGroupsScreening.CHECKLIST_FIELD;
        }
        if (MarpDiscordantCoupleScreening.isScreeningStep(form, mStepName)) {
            return MarpDiscordantCoupleScreening.PARTNER_PARAMETERS;
        }
        return null;
    }

    @Override
    public boolean executeRefreshLogicForNextStep() {
        JsonApi api = formFragment.getJsonApi();
        JSONObject form = api.getmJSONObject();
        String next;
        if (MarpPregnancyScreening.isIntakeStep(form, mStepName)) {
            next = MarpPregnancyScreening.nextStep(form);
        } else if (MarpPregnancyScreening.HIGH_RISK_STEP.equals(mStepName)
                && MarpPregnancyScreening.isIntakeStep(form, MarpPregnancyScreening.INTAKE_STEP)) {
            next = MarpPregnancyScreening.highRiskOutcome(form);
        } else {
            try {
                next = MarpSexualBehaviourScreening.nextStepAfterNo(api, mStepName);
            } catch (JSONException e) {
                Timber.e(e, "Could not prepare the MARP Sexual Behaviour route");
                return true;
            }
            if (next.isEmpty()) {
                return super.executeRefreshLogicForNextStep();
            }
        }
        if (next.isEmpty() || !form.has(next)) {
            return true;
        }
        api.setNextStep(next);
        return super.executeRefreshLogicForNextStep();
    }

    @Override
    protected boolean moveToNextWizardStep() {
        final String nextStep = getFormFragment().getJsonApi().nextStep();
        if (!"".equals(nextStep)) {
            JsonFormFragment next = KvpScreeningJsonFormFragment.getFormFragment(nextStep);
            getView().hideKeyBoard();
            getView().transactThis(next);
        }
        return false;
    }
}
