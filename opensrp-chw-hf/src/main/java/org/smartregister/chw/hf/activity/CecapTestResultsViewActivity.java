package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.pmtct.util.DBConstants.KEY.FORM_SUBMISSION_ID;
import static org.smartregister.client.utils.constants.JsonFormConstants.JSON_FORM_KEY.GLOBAL;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.dao.HomeVisitDao;
import org.smartregister.chw.cecap.activity.BaseTestResultsViewActivity;
import org.smartregister.chw.cecap.dao.CecapDao;
import org.smartregister.chw.cecap.fragment.BaseTestResultsFragment;
import org.smartregister.chw.cecap.util.Constants;
import org.smartregister.chw.cecap.util.DBConstants;
import org.smartregister.chw.cecap.util.NCUtils;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.fragment.CecapTestResultsFragment;
import org.smartregister.chw.hf.utils.PmtctVisitUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;

import java.util.ArrayList;

import timber.log.Timber;

public class CecapTestResultsViewActivity extends BaseTestResultsViewActivity implements View.OnClickListener {

    private String baseEntityId;
    private String parentFormSubmissionId;

    public static void startResultsForm(Context context, String jsonString, String baseEntityId, String parentFormSubmissionId) {
        Intent intent = new Intent(context, CecapTestResultsViewActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.CECAP_FORM, jsonString);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PARENT_FORM_ENTITY_ID, parentFormSubmissionId);
        context.startActivity(intent);
    }

    @Override
    public BaseTestResultsFragment getBaseFragment() {
        return CecapTestResultsFragment.newInstance(baseEntityId);
    }

    @Override
    protected void onCreation() {
        String jsonString = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.CECAP_FORM);
        String baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        String parentFormSubmissionId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.PARENT_FORM_ENTITY_ID);

        this.baseEntityId = baseEntityId;
        this.parentFormSubmissionId = parentFormSubmissionId;

        if (StringUtils.isBlank(jsonString)) {
            super.onCreation();
            ImageView backImageView = findViewById(R.id.back);
            backImageView.setOnClickListener(this);
        } else {
            try {
                JSONObject form = new JSONObject(jsonString);
                form.getJSONObject(GLOBAL).put("hiv_status",CecapDao.getMember(baseEntityId).getHivStatus());
                startFormActivity(form);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.back) {
            finish();
        }
    }

    @Override
    public void startFormActivity(JSONObject jsonObject) {
        Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
        intent.putExtra(Constants.JSON_FORM_EXTRA.JSON, jsonObject.toString());

        Form form = new Form();
        form.setName(getString(R.string.test_results));
        form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
        form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
        form.setHomeAsUpIndicator(org.smartregister.chw.core.R.mipmap.ic_cross_white);
        form.setPreviousLabel(getResources().getString(org.smartregister.chw.core.R.string.back));
        intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

        startActivityForResult(intent, Constants.REQUEST_CODE_GET_JSON);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_CANCELED) {
            //handle form close
            finish();
        }

        if (requestCode == Constants.REQUEST_CODE_GET_JSON && resultCode == Activity.RESULT_OK) {
            //handle form saving
            String jsonString = data.getStringExtra(Constants.JSON_FORM_EXTRA.JSON);
            try {
                AllSharedPreferences allSharedPreferences = org.smartregister.util.Utils.getAllSharedPreferences();
                Event baseEvent = org.smartregister.chw.pmtct.util.JsonFormUtils.processJsonForm(allSharedPreferences, jsonString, Constants.TABLES.CECAP_TEST_RESULTS);
                org.smartregister.chw.pmtct.util.JsonFormUtils.tagEvent(allSharedPreferences, baseEvent);
                baseEvent.setBaseEntityId(baseEntityId);
                baseEvent.addObs((new Obs()).withFormSubmissionField(DBConstants.KEY.CECAP_TEST_RESULTS_FOLLOWUP_FORM_SUBMISSION_ID).withValue(parentFormSubmissionId).withFieldCode(DBConstants.KEY.CECAP_TEST_RESULTS_FOLLOWUP_FORM_SUBMISSION_ID).withFieldType("formsubmissionField").withFieldDataType("text").withParentCode("").withHumanReadableValues(new ArrayList<>()));

                JSONObject jsonObject = new JSONObject(jsonString);
                if (jsonObject.has(FORM_SUBMISSION_ID)) {
                    Event processedEvent = HomeVisitDao.getEventByFormSubmissionId(jsonObject.getString(FORM_SUBMISSION_ID));
                    baseEvent.setEventDate(processedEvent.getEventDate());
                    PmtctVisitUtils.deleteSavedEvent(allSharedPreferences, processedEvent.getBaseEntityId(), processedEvent.getEventId(), processedEvent.getFormSubmissionId(), "event");
                }

                NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.pmtct.util.JsonFormUtils.gson.toJson(baseEvent)));

            } catch (Exception e) {
                Timber.e(e);
            }
            //handles going back to activity after save
            finish();
        }

    }
}