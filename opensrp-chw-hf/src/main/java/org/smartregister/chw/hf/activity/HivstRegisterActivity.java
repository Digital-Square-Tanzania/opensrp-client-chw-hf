package org.smartregister.chw.hf.activity;

import static com.vijay.jsonwizard.constants.JsonFormConstants.FIELDS;
import static org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1;

import android.app.Activity;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.core.activity.CoreHivstRegisterActivity;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.dao.HfKvpDao;
import org.smartregister.chw.hf.fragment.HivstMobilizationFragment;
import org.smartregister.chw.hf.fragment.HivstRegisterFragment;
import org.smartregister.chw.hivst.util.Constants;
import org.smartregister.chw.kvp.dao.KvpDao;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.view.fragment.BaseRegisterFragment;

import timber.log.Timber;

public class HivstRegisterActivity extends CoreHivstRegisterActivity {


    public static void startHivstRegistrationActivity(Activity activity, String memberBaseEntityID, String gender) {
        Intent intent = new Intent(activity, HivstRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.HIVST_FORM_NAME, Constants.FORMS.HIVST_REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GENDER, gender);
        activity.startActivity(intent);
    }

    //overloaded function for age global only in all member (hivst registration)
    public static void startHivstRegistrationActivity(Activity activity, String memberBaseEntityID, String gender, int age) {
        Intent intent = new Intent(activity, HivstRegisterActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, memberBaseEntityID);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.ACTION, Constants.ACTIVITY_PAYLOAD_TYPE.REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.HIVST_FORM_NAME, Constants.FORMS.HIVST_REGISTRATION);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.GENDER, gender);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.AGE, age);
        activity.startActivity(intent);
    }

    @Override
    protected BaseRegisterFragment getRegisterFragment() {
        return new HivstRegisterFragment();
    }

    @Override
    protected Fragment[] getOtherFragments() {
        return new Fragment[]{
                new HivstMobilizationFragment()
        };
    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {

        String kvpGroup = KvpDao.getDominantKVPGroup(BASE_ENTITY_ID);

        if (StringUtils.isNotBlank(kvpGroup)) {
            JSONArray fields = null;
            try {
                fields = jsonForm.getJSONObject(STEP1).getJSONArray(FIELDS);
                JSONObject clientGroupFemale15To24 = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "client_group_female_15_24");
                JSONObject clientGroupFemale = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "client_group_female");
                JSONObject clientGroupMale = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "client_group_male");

                MemberObject memberObject = HfKvpDao.getKvpMember(BASE_ENTITY_ID);

                if (memberObject.getGender().equalsIgnoreCase("male")) {
                    clientGroupMale.put("value", kvpGroup);
                } else {
                    if (memberObject.getAge() >= 15 && memberObject.getAge() <= 24) {
                        clientGroupFemale15To24.put("value", kvpGroup);
                    } else {
                        clientGroupFemale.put("value", kvpGroup);
                    }
                }

            } catch (JSONException e) {
                Timber.e(e);
            }

        }

        startActivityForResult(FormUtils.getStartFormActivity(jsonForm, this.getString(org.smartregister.chw.core.R.string.hivst), this), JsonFormUtils.REQUEST_CODE_GET_JSON);
    }

}
