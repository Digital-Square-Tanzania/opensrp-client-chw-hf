package org.smartregister.chw.hf.activity;

import static org.smartregister.AllConstants.DEFAULT_LOCALITY_NAME;
import static org.smartregister.chw.core.utils.CoreJsonFormUtils.TITLE;
import static org.smartregister.chw.hf.utils.Constants.JsonFormConstants.STEP1;
import static org.smartregister.client.utils.constants.JsonFormConstants.TYPE;
import static org.smartregister.family.util.JsonFormUtils.READ_ONLY;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.FIELDS;
import static org.smartregister.opd.utils.OpdConstants.JSON_FORM_KEY.VALUE;

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gkemon.XMLtoPDF.PdfGenerator;
import com.gkemon.XMLtoPDF.PdfGeneratorListener;
import com.gkemon.XMLtoPDF.model.FailureResponse;
import com.gkemon.XMLtoPDF.model.SuccessResponse;
import com.vijay.jsonwizard.constants.JsonFormConstants;
import com.vijay.jsonwizard.domain.Form;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.AllConstants;
import org.smartregister.Context;
import org.smartregister.chw.core.utils.FormUtils;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.dao.HeiDao;
import org.smartregister.chw.hf.dao.HfPmtctDao;
import org.smartregister.chw.lab.activity.BaseLabTestRequestDetailsActivity;
import org.smartregister.chw.lab.dao.LabDao;
import org.smartregister.chw.lab.domain.TestSample;
import org.smartregister.chw.lab.util.Constants;
import org.smartregister.chw.lab.util.LabJsonFormUtils;
import org.smartregister.chw.ld.LDLibrary;
import org.smartregister.chw.pmtct.dao.PmtctDao;
import org.smartregister.family.util.JsonFormUtils;
import org.smartregister.family.util.Utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;

import timber.log.Timber;

public class LabTestRequestDetailsActivity extends BaseLabTestRequestDetailsActivity {
    private RelativeLayout headerLayout;

    public static void startProfileActivity(Activity activity, String baseEntityId, String testSampleId, Boolean provideResultsToClient) {
        Intent intent = new Intent(activity, LabTestRequestDetailsActivity.class);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.BASE_ENTITY_ID, baseEntityId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.TEST_SAMPLE_ID, testSampleId);
        intent.putExtra(Constants.ACTIVITY_PAYLOAD.PROVIDE_RESULTS_TO_CLIENT, provideResultsToClient);
        activity.startActivity(intent);
    }

    @Override
    public void recordSampleRequestProcessing(String baseEntityId, String testRequestSampleId) {
        try {
            JSONObject sampleProcessingJson = FormUtils.getFormUtils().getFormJson(Constants.FORMS.LAB_HVL_PROCESSING);
            String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
            LabJsonFormUtils.getRegistrationForm(sampleProcessingJson, baseEntityId, locationId);


            JSONArray fields = sampleProcessingJson.getJSONObject(STEP1).getJSONArray(FIELDS);

            JSONObject sampleId = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "sample_id");
            sampleId.put(VALUE, testRequestSampleId);


            JSONObject sampleCollectionDate = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "sample_collection_date");
            sampleCollectionDate.put(VALUE, testSample.getSampleCollectionDate());

            JSONObject sampleCollectionTime = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "sample_collection_time");
            sampleCollectionTime.put(VALUE, testSample.getSampleCollectionTime());

            startFormActivity(sampleProcessingJson);
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    @Override
    public void recordSampleRequestResults(String baseEntityId, String testRequestSampleId) {
        try {
            JSONObject sampleResultsJson;
            if (testSample.getSampleType().equalsIgnoreCase("hvl")) {
                sampleResultsJson = FormUtils.getFormUtils().getFormJson(Constants.FORMS.LAB_HVL_RESULTS);
            } else {
                sampleResultsJson = FormUtils.getFormUtils().getFormJson(Constants.FORMS.LAB_HEID_RESULTS);
            }

            String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
            LabJsonFormUtils.getRegistrationForm(sampleResultsJson, baseEntityId, locationId);

            JSONArray fields = sampleResultsJson.getJSONObject(STEP1).getJSONArray(FIELDS);
            JSONObject sampleId = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "sample_id");
            sampleId.put(VALUE, testRequestSampleId);

            JSONObject dispatchDate = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "dispatch_date");
            dispatchDate.put(VALUE, LabDao.getManifestByTestSampleId(testRequestSampleId).getDispatchDate());

            startFormActivity(sampleResultsJson);
        } catch (Exception e) {
            Timber.e(e);
        }

    }

    @Override
    public void recordProvisionOfResultsToClient(String baseEntityId, String testRequestSampleId) {
        try {
            JSONObject jsonObject;
            TestSample testSample = LabDao.getTestSamplesRequestsBySampleId(testRequestSampleId).get(0);
            if (PmtctDao.isRegisteredForPmtct(baseEntityId)) {
                jsonObject = (new com.vijay.jsonwizard.utils.FormUtils()).getFormJsonFromRepositoryOrAssets(getBaseContext(), org.smartregister.chw.hf.utils.Constants.JsonForm.getHvlTestResultsForm());
                assert jsonObject != null;

                String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
                LabJsonFormUtils.getRegistrationForm(jsonObject, baseEntityId, locationId);

                JSONArray fields = jsonObject.getJSONObject(STEP1).getJSONArray(FIELDS);

                fields.getJSONObject(0).put("value", testRequestSampleId);

                fields.getJSONObject(1).put("value", testRequestSampleId);

                JSONObject global = jsonObject.getJSONObject("global");
                global.put("is_after_eac", HfPmtctDao.isAfterEAC(baseEntityId));


                JSONObject notifyTndResults = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "notify_tnd_results");
                notifyTndResults.put(TYPE, "hidden");

                JSONObject hvlResults = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "hvl_result");
                hvlResults.put(READ_ONLY, true);
                hvlResults.put(VALUE, testSample.getResults());

                JSONObject sampleResultsDate = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "sample_results_date");
                sampleResultsDate.put(VALUE, testSample.getResultsDate());
            } else {
                jsonObject = (new com.vijay.jsonwizard.utils.FormUtils()).getFormJsonFromRepositoryOrAssets(getBaseContext(), org.smartregister.chw.hf.utils.Constants.JsonForm.getHeiHivTestResults());
                assert jsonObject != null;

                String locationId = Context.getInstance().allSharedPreferences().getPreference(AllConstants.CURRENT_LOCATION_ID);
                LabJsonFormUtils.getRegistrationForm(jsonObject, baseEntityId, locationId);

                JSONArray fields = jsonObject.getJSONObject(STEP1).getJSONArray(FIELDS);

                fields.getJSONObject(0).put("value", testRequestSampleId);

                JSONObject hivResultsDate = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "hiv_test_result_date");
                hivResultsDate.put(TYPE, "hidden");
                hivResultsDate.put(VALUE, testSample.getResultsDate());

                JSONObject resultsProvidedToParents = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "results_provided_to_parents");
                resultsProvidedToParents.put(TYPE, "hidden");
                resultsProvidedToParents.put(VALUE, "yes");

                JSONObject hvlResults = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "hiv_test_result");
                hvlResults.put(TYPE, "hidden");
                hvlResults.put(VALUE, testSample.getResults());

                JSONObject sampleResultsDate = org.smartregister.util.JsonFormUtils.getFieldJSONObject(fields, "sample_results_date");
                sampleResultsDate.put(VALUE, testSample.getResultsDate());
            }


            startFormActivity(jsonObject);
        } catch (JSONException e) {
            Timber.e(e);
        }

    }

    @Override
    public void startFormActivity(JSONObject jsonForm) {
        try {
            Intent intent = new Intent(this, Utils.metadata().familyMemberFormActivity);
            intent.putExtra(org.smartregister.family.util.Constants.JSON_FORM_EXTRA.JSON, jsonForm.toString());

            Form form = new Form();
            form.setActionBarBackground(org.smartregister.chw.core.R.color.family_actionbar);
            form.setWizard(false);
            intent.putExtra(JsonFormConstants.JSON_FORM_KEY.FORM, form);

            form.setWizard(true);
            form.setNavigationBackground(org.smartregister.chw.core.R.color.family_navigation);
            form.setName(jsonForm.getJSONObject(STEP1).getString(TITLE));
            form.setNextLabel(this.getResources().getString(org.smartregister.chw.core.R.string.next));
            form.setPreviousLabel(this.getResources().getString(org.smartregister.chw.core.R.string.back));
            form.setSaveLabel(this.getResources().getString(org.smartregister.chw.core.R.string.save));


            startActivityForResult(intent, JsonFormUtils.REQUEST_CODE_GET_JSON);
        } catch (Exception e) {
            Timber.e(e);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(org.smartregister.lab.R.menu.menu_download_test_results, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == org.smartregister.lab.R.id.action_download_test_results) {
            downloadTestResults(mBaseEntityId);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private void downloadTestResults(String baseEntityId) {
        headerLayout = findViewById(R.id.header_layout);
        headerLayout.setVisibility(View.VISIBLE);
        TextView facilityName = findViewById(org.smartregister.lab.R.id.facility_name);
        TextView clientName = findViewById(org.smartregister.lab.R.id.client_name);

        String facilityNameString = LDLibrary.getInstance().context().allSharedPreferences().getPreference(DEFAULT_LOCALITY_NAME);

        if (StringUtils.isNotBlank(facilityNameString)) {
            facilityName.setText(facilityNameString);
        } else {
            facilityName.setVisibility(View.GONE);
        }



        int age = 0;
        String firstName = "";
        String middleName = "";
        String lastName = "";
        try {
            if (PmtctDao.isRegisteredForPmtct(baseEntityId)) {
                age = PmtctDao.getMember(baseEntityId).getAge();
                firstName = PmtctDao.getMember(baseEntityId).getFirstName();
                middleName = PmtctDao.getMember(baseEntityId).getMiddleName();
                lastName = PmtctDao.getMember(baseEntityId).getLastName();
            } else if (HeiDao.getMember(baseEntityId) != null) {
                age = HeiDao.getMember(baseEntityId).getAge();
                firstName = HeiDao.getMember(baseEntityId).getFirstName();
                middleName = HeiDao.getMember(baseEntityId).getMiddleName();
                lastName = HeiDao.getMember(baseEntityId).getLastName();
            }
            clientName.setText(MessageFormat.format(getString(org.smartregister.ld.R.string.partograph_client_name), firstName, middleName, lastName));


        } catch (Exception e) {
            Timber.e(e);
        }
        View mView = findViewById(R.id.main_layout);
        PdfGenerator.getBuilder()
                .setContext(LabTestRequestDetailsActivity.this)
                .fromViewSource()
                .fromView(mView)
                .setFileName(String.format(Locale.getDefault(), "%s %s %s, %d",
                        firstName,
                        middleName,
                        lastName,
                        age))
                .setFolderNameOrPath("MyFolder/MyDemoHorizontalText/")
                .actionAfterPDFGeneration(PdfGenerator.ActionAfterPDFGeneration.OPEN)
                .build(new PdfGeneratorListener() {
                    @Override
                    public void onFailure(FailureResponse failureResponse) {
                        super.onFailure(failureResponse);
                    }

                    @Override
                    public void showLog(String log) {
                        super.showLog(log);
                    }

                    @Override
                    public void onStartPDFGeneration() {
                        /*When PDF generation begins to start*/
                    }

                    @Override
                    public void onFinishPDFGeneration() {
                        /*When PDF generation is finished*/
                        headerLayout.setVisibility(View.GONE);
                    }

                    @Override
                    public void onSuccess(SuccessResponse response) {
                        super.onSuccess(response);
                    }
                });
    }
}
