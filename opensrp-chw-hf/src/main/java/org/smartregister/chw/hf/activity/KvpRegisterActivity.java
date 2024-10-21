package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.core.utils.CoreConstants.JSON_FORM.isMultiPartForm;

import android.app.Activity;
import android.content.Intent;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreKvpRegisterActivity;
import org.smartregister.chw.core.custom_views.NavigationMenu;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.fragment.KvpRegisterFragment;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.view.fragment.BaseRegisterFragment;

public class KvpRegisterActivity extends CoreKvpRegisterActivity {

    public static void startKvpScreeningMale(Activity activity, String memberBaseEntityId, String gender, int age) {
        Intent intent = new Intent(activity, KvpRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.KVP_FORM_NAME, Constants.FORMS.KVP_SCREENING_MALE);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GENDER, gender);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.AGE, age);

        activity.startActivity(intent);
    }

    public static void startKvpScreeningFemale(Activity activity, String memberBaseEntityId, String gender, int age) {

        Intent intent = new Intent(activity, KvpRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.KVP_FORM_NAME, Constants.FORMS.KVP_SCREENING_FEMALE);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GENDER, gender);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.AGE, age);
        activity.startActivity(intent);
    }

    @Override
    public Form getFormConfig() {
        Form form = new Form();
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(true);
        form.setName(getString(R.string.kvp_screening));
        form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
        form.setNextLabel(this.getResources().getString(org.smartregister.chw.core.R.string.next));
        form.setPreviousLabel(this.getResources().getString(org.smartregister.chw.core.R.string.back));
        form.setSaveLabel(this.getResources().getString(org.smartregister.chw.core.R.string.save));
        return form;
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new KvpRegisterFragment();
    }

    @Override
    protected void onResumption() {
        super.onResumption();
        NavigationMenu menu = NavigationMenu.getInstance(this, null, null);
        if (menu != null) {
            menu.getNavigationAdapter().setSelectedView(CoreConstants.DrawerMenu.KVP);
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {

        Intent intent = new Intent(this, KvpScreeningJsonWizardFormActivity.class);
        intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());
        Form form = new Form();
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(true);
        form.setHomeAsUpIndicator(org.smartregister.chw.core.R.mipmap.ic_cross_white);
        form.setSaveLabel(getResources().getString(org.smartregister.chw.core.R.string.save));

        if (isMultiPartForm(jsonForm)) {
            form.setWizard(true);
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
            form.setName(null);
            form.setNextLabel(getResources().getString(org.smartregister.chw.core.R.string.next));
            form.setPreviousLabel(getResources().getString(org.smartregister.chw.core.R.string.back));
        }
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }
}
