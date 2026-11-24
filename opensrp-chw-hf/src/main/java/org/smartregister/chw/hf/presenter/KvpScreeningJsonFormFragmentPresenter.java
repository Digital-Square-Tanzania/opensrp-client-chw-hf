package org.smartregister.chw.hf.presenter;

import static com.vijay.jsonwizard.utils.FormUtils.NATIIVE_FORM_DATE_FORMAT_PATTERN;
import static org.smartregister.chw.core.utils.CoreJsonFormUtils.ENCOUNTER_TYPE;
import static org.smartregister.chw.hf.utils.JsonFormUtils.clearFormValue;
import static org.smartregister.client.utils.constants.JsonFormConstants.V_MAX;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.OPTIONS;
import static org.smartregister.util.JsonFormUtils.FIELDS;
import static org.smartregister.util.JsonFormUtils.KEY;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.fragment.KvpScreeningJsonFormFragment;
import org.smartregister.chw.hf.utils.MarpGroupLabels;
import org.smartregister.chw.hf.utils.MarpPregnancyScreening;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.opd.utils.OpdConstants;

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
            JSONObject currentJsonState = new JSONObject(getView().getCurrentJsonState());

            if (currentJsonState.getString(ENCOUNTER_TYPE).equalsIgnoreCase(Constants.EVENT_TYPE.KVP_REGISTRATION)) {
                JSONObject clientGroup = JsonFormUtils.getFieldJSONObject(step.optJSONArray(FIELDS), "client_group");
                if (clientGroup != null && MarpPregnancyScreening.ENROLLMENT_STEP.equals(mStepName)) {
                    clientGroup.put(VALUE, MarpPregnancyScreening.GROUP_CODE);
                } else if (clientGroup != null) {
                    JSONArray clientGroupArray = clientGroup.getJSONArray(OPTIONS);
                    updateDrugGroupOptions(currentJsonState, clientGroupArray);

                    //Male KVP Screening Form
                    if (mStepName.equals("step8")) {
                        JSONObject isMsm = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step4").getJSONArray(FIELDS), "is_msm");
                        if (!isMsm.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isMsm.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("msm")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "msm")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"msm\",\"text\":\"MHR\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"msm\"}"));
                        }


                        JSONObject isSdc = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step5").getJSONArray(FIELDS), "is_sdc");
                        if (!isSdc.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isSdc.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("serodiscordant_couple")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "serodiscordant_couple")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"serodiscordant_couple\",\"text\":\"Serodiscordant couple\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"serodiscordant_couple\"}"));
                        }

                        JSONObject isPrisoner = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step6").getJSONArray(FIELDS), "is_prisoner");
                        if (!isPrisoner.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isPrisoner.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("prisoner")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "prisoner")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"prisoner\",\"text\":\"Prisoners\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"prisoner\"}"));
                        }

                        JSONObject isRumandee = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step6").getJSONArray(FIELDS), "is_rumandee");
                        if (!isRumandee.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isRumandee.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("rumandee")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "rumandee")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"rumandee\",\"text\":\"Rumandee\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"rumandee\"}"));
                        }

                        JSONObject isMobilePopulation = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step6").getJSONArray(FIELDS), "is_mobile_population");
                        if (!isMobilePopulation.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isMobilePopulation.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("mobile_population")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "mobile_population")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"mobile_population\",\"text\":\"Mobile population\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"mobile_population\"}"));
                        }


                        JSONObject isOtherGroupAtRisk = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step6").getJSONArray(FIELDS), "is_ovp_kvp");
                        if (!isOtherGroupAtRisk.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isOtherGroupAtRisk.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("other_vulnerable_population")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "other_vulnerable_population")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"other_vulnerable_population\",\"text\":\"Other vulnerable population\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"other_vulnerable_population\"}"));
                        }

                    } else if (mStepName.equals("step9")) {  //Female KVP Screening Form

                        JSONObject isFsw = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step5").getJSONArray(FIELDS), "is_fsw");
                        if (!isFsw.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isFsw.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("fsw")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "fsw")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"fsw\",\"text\":\"WHR\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"fsw\"}"));
                        }

                        JSONObject isAgwy = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step6").getJSONArray(FIELDS), "is_agyw");
                        if (!isAgwy.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isAgwy.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("agyw")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "agyw")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"agyw\",\"text\":\"vAGYW\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"agyw\"}"));
                        }


                        JSONObject isSdc = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step4").getJSONArray(FIELDS), "is_sdc");
                        if (!isSdc.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isSdc.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("serodiscordant_couple")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "serodiscordant_couple")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"serodiscordant_couple\",\"text\":\"Serodiscordant couple\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"serodiscordant_couple\"}"));
                        }

                        JSONObject isPrisoner = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step7").getJSONArray(FIELDS), "is_prisoner");
                        if (!isPrisoner.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isPrisoner.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("prisoner")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "prisoner")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"prisoner\",\"text\":\"Prisoners\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"prisoner\"}"));
                        }

                        JSONObject isRumandee = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step7").getJSONArray(FIELDS), "is_rumandee");
                        if (!isRumandee.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isRumandee.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("rumandee")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "rumandee")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"rumandee\",\"text\":\"Rumandee\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"rumandee\"}"));
                        }

                        JSONObject isMobilePopulation = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step7").getJSONArray(FIELDS), "is_mobile_population");
                        if (!isMobilePopulation.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isMobilePopulation.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("mobile_population")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "mobile_population")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"mobile_population\",\"text\":\"Mobile population\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"mobile_population\"}"));
                        }

                        JSONObject isOtherGroupAtRisk = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step7").getJSONArray(FIELDS), "is_ovp_kvp");
                        if (!isOtherGroupAtRisk.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isOtherGroupAtRisk.getString(VALUE).equals("yes")) {
                            for (int i = 0; i < clientGroupArray.length(); i++) {
                                if (clientGroupArray.getJSONObject(i).getString(KEY).equals("other_vulnerable_population")) {
                                    clientGroupArray.remove(i);
                                    break;
                                }
                            }
                        } else if (!containsKey(clientGroupArray, "other_vulnerable_population")) {
                            clientGroupArray.put(new JSONObject("{\"key\":\"other_vulnerable_population\",\"text\":\"Other vulnerable population\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"other_vulnerable_population\"}"));
                        }

                    }
                    MarpGroupLabels.applyLongLabels(formFragment.getContext().getResources(), clientGroupArray);
                    preselectRiskGroup(clientGroup, clientGroupArray);
                }
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
        return super.onNextClick(mainViews);
    }

    @Override
    public void onCheckedChanged(CompoundButton button, boolean isChecked) {
        super.onCheckedChanged(button, isChecked);
        if (MarpPregnancyScreening.HIGH_RISK_STEP.equals(mStepName) && isChecked
                && MarpPregnancyScreening.CHECKLIST_FIELD.equals(button.getTag(com.vijay.jsonwizard.R.id.key))) {
            formFragment.dismissHighRiskChecklistError();
        }
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
            return super.executeRefreshLogicForNextStep();
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

    private void updateDrugGroupOptions(JSONObject form, JSONArray options) throws JSONException {
        JSONObject drugUse = JsonFormUtils.getFieldJSONObject(form.getJSONObject("step1").getJSONArray(FIELDS), "is_pwud");
        JSONObject drugInjection = JsonFormUtils.getFieldJSONObject(form.getJSONObject("step2").getJSONArray(FIELDS), "is_pwid");
        // The existing is_pwud calculation records any recent drug use.
        boolean usedDrugs = "yes".equals(drugUse.optString(VALUE));
        boolean injectedDrugs = "yes".equals(drugInjection.optString(VALUE));
        updateDrugGroupOption(options, "pwud", R.string.marp_pwud_enrollment_option, usedDrugs && !injectedDrugs);
        updateDrugGroupOption(options, "pwid", R.string.marp_pwid_enrollment_option, usedDrugs && injectedDrugs);
    }

    private void updateDrugGroupOption(JSONArray options, String key, int labelResId, boolean eligible) throws JSONException {
        for (int i = 0; i < options.length(); i++) {
            JSONObject option = options.getJSONObject(i);
            if (key.equals(option.getString(KEY))) {
                if (eligible) {
                    option.put(JsonFormConstants.TEXT, formFragment.getContext().getString(labelResId));
                } else {
                    options.remove(i);
                }
                return;
            }
        }
        if (eligible) {
            options.put(new JSONObject()
                    .put(KEY, key)
                    .put(JsonFormConstants.TEXT, formFragment.getContext().getString(labelResId))
                    .put("openmrs_entity_parent", "")
                    .put("openmrs_entity", "concept")
                    .put("openmrs_entity_id", key));
        }
    }

    private void preselectRiskGroup(JSONObject clientGroup, JSONArray options) throws JSONException {
        String selectedKey = Utils.getValueAfterTranslation(clientGroup);
        if (containsKey(options, selectedKey)) {
            return;
        }
        if ("msm".equals(selectedKey) || "fsw".equals(selectedKey)
                || "pwud".equals(selectedKey) || "pwid".equals(selectedKey)) {
            clearFormValue(formFragment.getJsonApi(), mStepName, clientGroup);
        }
        if (options.length() == 1) {
            String onlyKey = options.getJSONObject(0).getString(KEY);
            if ("msm".equals(onlyKey) || "fsw".equals(onlyKey)) {
                clientGroup.put(VALUE, onlyKey);
            }
        }
    }

    // Function to check if a specified key exists in the options array
    private boolean containsKey(JSONArray options, String searchKey) {
        try {
            for (int i = 0; i < options.length(); i++) {
                JSONObject option = options.getJSONObject(i);
                if (option.has("key") && option.getString("key").equals(searchKey)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Timber.e(e);
            return false;
        }
    }
}
