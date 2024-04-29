package org.smartregister.chw.hf.activity;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.FragmentTransaction;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.fragment.LabTestResultsRegisterFragment;
import org.smartregister.chw.pmtct.util.Constants;
import org.smartregister.pmtct.R;
import org.smartregister.view.activity.SecuredActivity;

import timber.log.Timber;

public class LabHeidResultsViewActivity extends SecuredActivity  implements View.OnClickListener  {
    private String baseEntityId;

    @Override
    protected void onCreation() {
        setContentView(R.layout.base_hvl_results_view_activity);

        TextView title =  findViewById(R.id.textview_title);
        title.setText("HEID Results");

        String jsonString = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.PMTCT_FORM);
        String baseEntityId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID);
        String parentFormSubmissionId = getIntent().getStringExtra(Constants.ACTIVITY_PAYLOAD.PARENT_FORM_ENTITY_ID);

        this.baseEntityId = baseEntityId;

        if (StringUtils.isBlank(jsonString)) {
            ImageView backImageView = findViewById(org.smartregister.chw.hf.R.id.back);
            backImageView.setOnClickListener(this);
        } else {
            try {
                JSONObject form = new JSONObject(jsonString);
                startFormActivity(form);
            } catch (JSONException e) {
                Timber.e(e);
            }
        }

        loadFragment();
    }


    @Override
    protected void onResumption() {
        //overridden
    }

    public LabTestResultsRegisterFragment getBaseFragment() {
        return LabTestResultsRegisterFragment.newInstance(baseEntityId);
    }

    private void loadFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        // Replace the contents of the container with the new fragment
        ft.replace(R.id.fragment_placeholder, getBaseFragment());
        ft.commit();
    }

    public void startFormActivity(JSONObject jsonObject) {
        //implement
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == org.smartregister.chw.hf.R.id.back) {
            finish();
        }
    }


}
