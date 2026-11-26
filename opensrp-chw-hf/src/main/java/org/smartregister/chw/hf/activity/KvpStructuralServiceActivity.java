package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.task.RunnableTask;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.interactor.KvpStructuralServiceInteractor;
import org.smartregister.chw.hf.schedulers.HfScheduleTaskExecutor;
import org.smartregister.chw.hf.utils.MarpPep;
import org.smartregister.chw.kvp.activity.BaseKvpVisitActivity;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.model.BaseKvpVisitAction;
import org.smartregister.chw.kvp.presenter.BaseKvpVisitPresenter;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;
import org.smartregister.util.LangUtils;

import java.util.Date;
import java.util.LinkedHashMap;

import timber.log.Timber;

public class KvpStructuralServiceActivity extends BaseKvpVisitActivity {
    private KvpStructuralServiceInteractor structuralInteractor;

    public static void startKvpStructuralServiceActivity(Activity activity, String baseEntityId, Boolean editMode) {
        Intent intent = new Intent(activity, KvpStructuralServiceActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.EDIT_MODE, editMode);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROFILE_TYPE, Constants.PROFILE_TYPES.KVP_PROFILE);
        activity.startActivity(intent);
    }

    @Override
    protected void registerPresenter() {
        structuralInteractor = new KvpStructuralServiceInteractor();
        presenter = new BaseKvpVisitPresenter(memberObject, this, structuralInteractor);
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        if (MarpPep.ENCOUNTER_TYPE.equals(jsonForm.optString("encounter_type"))) {
            try {
                jsonForm = structuralInteractor.preparePepForm(jsonForm);
            } catch (JSONException e) {
                Timber.e(e, "Unable to display the saved MARP PEP assessment");
            }
        }
        Form form = new Form();
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(false);

        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        intent.putExtra(org.smartregister.family.util.Constants.WizardFormActivity.EnableOnCloseDialog, false);
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

    @Override
    public void initializeActions(LinkedHashMap<String, BaseKvpVisitAction> actions) {
        actionList.clear();
        super.initializeActions(actions);
        redrawVisitUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != Constants.REQUEST_CODE_GET_JSON || resultCode != RESULT_OK || data == null) {
            return;
        }
        if (getString(R.string.kvp_gbv_analysis).equals(current_action)) {
            initializeActions(structuralInteractor.onGbvFormSaved());
        } else if (getString(R.string.kvp_pep_assesment).equals(current_action)) {
            structuralInteractor.onPepFormSaved();
            redrawVisitUI();
        }
    }

    @Override
    public void submittedAndClose() {
        Runnable runnable = () -> HfScheduleTaskExecutor.getInstance().execute(memberObject.getBaseEntityId(), Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT, new Date());
        Utils.startAsyncTask(new RunnableTask(runnable), null);
        super.submittedAndClose();
    }

    @Override
    protected void attachBaseContext(Context base) {
        // get language from prefs
        String lang = LangUtils.getLanguage(base.getApplicationContext());
        super.attachBaseContext(LangUtils.setAppLocale(base, lang));
    }

    @Override
    public void redrawHeader(MemberObject memberObject) {
        tvTitle.setText(R.string.structural_services);
    }
}
