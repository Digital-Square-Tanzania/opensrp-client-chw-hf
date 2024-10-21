package org.smartregister.chw.hf.presenter;

import static org.smartregister.chw.core.utils.CoreJsonFormUtils.TITLE;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.OPTIONS;
import static org.smartregister.util.JsonFormUtils.FIELDS;
import static org.smartregister.util.JsonFormUtils.KEY;
import static org.smartregister.util.JsonFormUtils.VALUE;

import android.app.ProgressDialog;
import android.view.View;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.fragments.JsonFormFragment;
import com.vijay.jsonwizard.interactors.JsonFormInteractor;
import com.vijay.jsonwizard.presenters.JsonWizardFormFragmentPresenter;
import com.vijay.jsonwizard.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.fragment.KvpScreeningJsonFormFragment;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.opd.utils.OpdConstants;

import java.util.List;

import timber.log.Timber;

public class KvpScreeningJsonFormFragmentPresenter extends JsonWizardFormFragmentPresenter {
    private final JsonFormFragment formFragment;
    private boolean cleanupAndExit = false;

    public KvpScreeningJsonFormFragmentPresenter(JsonFormFragment formFragment, JsonFormInteractor jsonFormInteractor) {
        super(formFragment, jsonFormInteractor);
        this.formFragment = formFragment;
    }

    @Override
    public void addFormElements() {
        final ProgressDialog dialog = new ProgressDialog(formFragment.getContext());
        dialog.setCancelable(false);
        dialog.setTitle(formFragment.getContext().getString(com.vijay.jsonwizard.R.string.loading));
        dialog.setMessage(formFragment.getContext().getString(com.vijay.jsonwizard.R.string.loading_form_message));
        dialog.show();
        mStepName = getView().getArguments().getString(JsonFormConstants.STEPNAME);

        JSONObject step = getView().getStep(mStepName);
        if (step == null) {
            return;
        }

        try {
            if (step.getString(TITLE).contains("Enrollment")) {
                JSONObject currentJsonState = new JSONObject(getView().getCurrentJsonState());

                JSONObject isPwud = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step1").getJSONArray(FIELDS), "is_pwud");
                JSONObject isPwid = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step2").getJSONArray(FIELDS), "is_pwid");

                //Male KVP Screening Form
                if (mStepName.equals("step8")) {

                    JSONObject clientGroup = JsonFormUtils.getFieldJSONObject(step.getJSONArray(FIELDS), "client_group");
                    JSONArray clientGroupArray = clientGroup.getJSONArray(OPTIONS);

                    if (!isPwud.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isPwud.getString(VALUE).equals("yes")) {
                        for (int i = 0; i < clientGroupArray.length(); i++) {
                            if (clientGroupArray.getJSONObject(i).getString(KEY).equals("pwud")) {
                                clientGroupArray.remove(i);
                                break;
                            }
                        }
                    } else if (!containsKey(clientGroupArray,"pwud")) {
                        clientGroupArray.put(new JSONObject("{\"key\":\"pwud\",\"text\":\"PWUD\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"pwud\"}"));
                    }


                    if (!isPwid.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isPwid.getString(VALUE).equals("yes")) {
                        for (int i = 0; i < clientGroupArray.length(); i++) {
                            if (clientGroupArray.getJSONObject(i).getString(KEY).equals("pwid")) {
                                clientGroupArray.remove(i);
                                break;
                            }
                        }
                    } else if (!containsKey(clientGroupArray,"pwid")) {
                        clientGroupArray.put(new JSONObject("{\"key\":\"pwid\",\"text\":\"PWID\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"pwid\"}"));
                    }


                    JSONObject isMsm = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step4").getJSONArray(FIELDS), "is_msm");
                    if (!isMsm.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isMsm.getString(VALUE).equals("yes")) {
                        for (int i = 0; i < clientGroupArray.length(); i++) {
                            if (clientGroupArray.getJSONObject(i).getString(KEY).equals("msm")) {
                                clientGroupArray.remove(i);
                                break;
                            }
                        }
                    } else if (!containsKey(clientGroupArray,"msm")) {
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
                    } else if (!containsKey(clientGroupArray,"serodiscordant_couple")) {
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
                    } else if (!containsKey(clientGroupArray,"prisoner")) {
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
                    } else if (!containsKey(clientGroupArray,"rumandee")) {
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
                    } else if (!containsKey(clientGroupArray,"mobile_population")) {
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
                    } else if (!containsKey(clientGroupArray,"other_vulnerable_population")) {
                        clientGroupArray.put(new JSONObject("{\"key\":\"other_vulnerable_population\",\"text\":\"Other vulnerable population\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"other_vulnerable_population\"}"));
                    }

                } else if (mStepName.equals("step9")) {  //Female KVP Screening Form

                    JSONObject clientGroup = JsonFormUtils.getFieldJSONObject(step.getJSONArray(FIELDS), "client_group");
                    JSONArray clientGroupArray = clientGroup.getJSONArray(OPTIONS);


                    if (!isPwud.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isPwud.getString(VALUE).equals("yes")) {
                        for (int i = 0; i < clientGroupArray.length(); i++) {
                            if (clientGroupArray.getJSONObject(i).getString(KEY).equals("pwud")) {
                                clientGroupArray.remove(i);
                                break;
                            }
                        }
                    } else if (!containsKey(clientGroupArray,"pwud")) {
                        clientGroupArray.put(new JSONObject("{\"key\":\"pwud\",\"text\":\"PWUD\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"pwud\"}"));
                    }


                    if (!isPwid.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isPwid.getString(VALUE).equals("yes")) {
                        for (int i = 0; i < clientGroupArray.length(); i++) {
                            if (clientGroupArray.getJSONObject(i).getString(KEY).equals("pwid")) {
                                clientGroupArray.remove(i);
                                break;
                            }
                        }
                    } else if (!containsKey(clientGroupArray,"pwid")) {
                        clientGroupArray.put(new JSONObject("{\"key\":\"pwid\",\"text\":\"PWID\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"pwid\"}"));
                    }

                    JSONObject isFsw = JsonFormUtils.getFieldJSONObject(currentJsonState.getJSONObject("step5").getJSONArray(FIELDS), "is_fsw");
                    if (!isFsw.has(OpdConstants.JSON_FORM_KEY.VALUE) || !isFsw.getString(VALUE).equals("yes")) {
                        for (int i = 0; i < clientGroupArray.length(); i++) {
                            if (clientGroupArray.getJSONObject(i).getString(KEY).equals("fsw")) {
                                clientGroupArray.remove(i);
                                break;
                            }
                        }
                    } else if (!containsKey(clientGroupArray,"fsw")) {
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
                    } else if (!containsKey(clientGroupArray,"agyw")) {
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
                    } else if (!containsKey(clientGroupArray,"serodiscordant_couple")) {
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
                    } else if (!containsKey(clientGroupArray,"prisoner")) {
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
                    } else if (!containsKey(clientGroupArray,"rumandee")) {
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
                    } else if (!containsKey(clientGroupArray,"mobile_population")) {
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
                    } else if (!containsKey(clientGroupArray,"other_vulnerable_population")) {
                        clientGroupArray.put(new JSONObject("{\"key\":\"other_vulnerable_population\",\"text\":\"Other vulnerable population\",\"openmrs_entity_parent\":\"\",\"openmrs_entity\":\"concept\",\"openmrs_entity_id\":\"other_vulnerable_population\"}"));
                    }

                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
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

    protected boolean moveToNextWizardStep() {
        final String nextStep = getFormFragment().getJsonApi().nextStep();
        if (!"".equals(nextStep)) {
            JsonFormFragment next = KvpScreeningJsonFormFragment.getFormFragment(nextStep);
            getView().hideKeyBoard();
            getView().transactThis(next);
        }
        return false;
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
