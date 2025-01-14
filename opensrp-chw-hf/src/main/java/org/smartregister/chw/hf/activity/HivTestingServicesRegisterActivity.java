package org.smartregister.chw.hf.activity;

import static org.smartregister.client.utils.constants.JsonFormConstants.JSON_FORM_KEY.GLOBAL;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.MenuRes;
import androidx.fragment.app.Fragment;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hts.util.Constants;
import org.smartregister.chw.core.activity.CoreHtsRegisterActivity;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.fragment.HivTestingServicesRegisterFragment;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class HivTestingServicesRegisterActivity extends CoreHtsRegisterActivity {

    public static void startRegistration(Activity activity, String baseEntityId, int clientAge, String sex) {
        Intent intent = new Intent(activity, HivTestingServicesRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);

        if(clientAge >=15){
            //Open 15 and above form
            intent.putExtra(Constants.ACTIVITY_PAYLOAD.HTS_FORM_NAME, Constants.FORMS.HTS_SCREENING_15_AND_ABOVE);
            intent.putExtra(Constants.ACTIVITY_PAYLOAD.SEX, sex);
        }

        if(clientAge >=10 && clientAge <=14){
            //Open form for clients aged >=10 and less than or equal to 14
            intent.putExtra(Constants.ACTIVITY_PAYLOAD.HTS_FORM_NAME, Constants.FORMS.HTS_SCREENING_10_TO_14);
        }

        if(clientAge >= 2 && clientAge <=9){
            //Open form for clients aged >=2 and less than or equal to 9
            intent.putExtra(Constants.ACTIVITY_PAYLOAD.HTS_FORM_NAME, Constants.FORMS.HTS_SCREENING_2_TO_9);
        }

        activity.startActivity(intent);
    }

    @Override
    public Form getFormConfig() {
        Form form = new Form();
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setWizard(true);
        form.setName(getString(R.string.hts_screening));
        form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
        form.setNextLabel(this.getResources().getString(org.smartregister.chw.core.R.string.next));
        form.setPreviousLabel(this.getResources().getString(org.smartregister.chw.core.R.string.back));
        form.setSaveLabel(this.getResources().getString(org.smartregister.chw.core.R.string.save));
        return form;
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new HivTestingServicesRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{};
    }

    @MenuRes
    public int getMenuResource() {
        return R.menu.bottom_nav_cecap_menu;
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        if(FORM_NAME.equals(Constants.FORMS.HTS_SCREENING_15_AND_ABOVE)){
            try {
                jsonForm.getJSONObject(GLOBAL).put("sex",sex);
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        Intent intent = FormUtils.getStartFormActivity(jsonForm, null, this);
        if (getFormConfig() != null) {
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, getFormConfig());
        }
        startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

}
