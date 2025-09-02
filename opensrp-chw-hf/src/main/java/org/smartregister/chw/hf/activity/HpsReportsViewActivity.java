package org.smartregister.chw.hf.activity;

import static org.smartregister.chw.hf.utils.HfWebAppInterface.HFR_CODE;
import static org.smartregister.util.Utils.getAllSharedPreferences;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.dao.ChwNotificationDao;
import org.smartregister.chw.core.utils.CoreConstants;
import org.smartregister.chw.hf.BuildConfig;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.adapter.CHWAdapter;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.CHW;
import org.smartregister.chw.hf.domain.dhis2_reports.Dhis2Report;
import org.smartregister.chw.hf.domain.dhis2_reports.DhisDataValues;
import org.smartregister.chw.hf.domain.hps_reports.HpsAnnualReportObject;
import org.smartregister.chw.hf.domain.hps_reports.HpsMonthlyReportObject;
import org.smartregister.chw.hf.utils.Constants;
import org.smartregister.chw.hf.utils.JsonFormUtils;
import org.smartregister.chw.hf.utils.ReportUtils;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.clientandeventmodel.Obs;
import org.smartregister.family.util.Utils;
import org.smartregister.repository.AllSharedPreferences;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import timber.log.Timber;


public class HpsReportsViewActivity extends HfReportsViewActivity {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public HpsMonthlyReportObject hpsMonthlyReportObject;
    public HpsAnnualReportObject hpsAnnualReportObject;
    private int reportTittle;
    private Future<Dhis2Report> reportFuture;

    public static void startMe(Activity activity, String reportPath, int reportTitle, String reportDate) {
        Intent intent = new Intent(activity, HpsReportsViewActivity.class);
        intent.putExtra(ARG_REPORT_PATH, reportPath);
        intent.putExtra(ARG_REPORT_DATE, reportDate);
        intent.putExtra(ARG_REPORT_TITLE, reportTitle);
        intent.putExtra(ARG_REPORT_TYPE, Constants.ReportConstants.ReportTypes.HPS_REPORT);
        activity.startActivity(intent);
    }

    /**
     * Builds a Dhis2Report from the current HpsMonthlyReportObject.
     */
    private Dhis2Report buildDhis2Report() throws Exception {
        JSONObject jsonObject;
        if (reportTittle == R.string.hps_monthly_reports_title) {
            hpsMonthlyReportObject = new HpsMonthlyReportObject(ReportUtils.getReportDate());
            jsonObject = hpsMonthlyReportObject.getIndicatorData();
        } else {
            hpsAnnualReportObject = new HpsAnnualReportObject(ReportUtils.getReportDate());
            jsonObject = hpsAnnualReportObject.getIndicatorData();
        }
        List<DhisDataValues> dhisDataValues = ReportDao.getDhisDataValues(jsonObject);

        Dhis2Report dhis2Report = new Dhis2Report();
        dhis2Report.setDataValues(dhisDataValues);
        dhis2Report.setCompleteDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        dhis2Report.setPeriod(new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(ReportUtils.getReportDate()));
        dhis2Report.setOrgUnit(getAllSharedPreferences().getPreference(HFR_CODE).replace("HFR Code: ", ""));
        dhis2Report.setDataSet("AV47sHdUAav");
        return dhis2Report;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        reportTittle = getIntent().getIntExtra(ARG_REPORT_TITLE, 0);
        reportFuture = executor.submit(() -> {
            try {
                Thread.sleep(2000); // wait for 5 seconds
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                Timber.e(ie);
            }
            return buildDhis2Report();
        });
    }

    public void generateSendToDhis2Event(String baseEntityId, Context context, int reportTittle) throws JSONException {
        Dhis2Report dhis2Report;
        if (reportFuture != null) {
            try {
                dhis2Report = reportFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                Timber.e(e);
                dhis2Report = getReport();
            }
        } else {
            dhis2Report = getReport();
        }
        AllSharedPreferences sharedPreferences = getAllSharedPreferences();
        String eventType = reportTittle == R.string.hps_monthly_reports_title ? Constants.Events.SEND_MONTHLY_MTUHA_BOOK_3_TO_DHIS2 : Constants.Events.SEND_ANNUAL_REPORTS_TO_DHIS2;
        Event baseEvent = (Event) new Event()
                .withBaseEntityId(baseEntityId)
                .withEventDate(new Date())
                .withEventType(eventType)
                .withFormSubmissionId(org.smartregister.util.JsonFormUtils.generateRandomUUIDString())
                .withProviderId(sharedPreferences.fetchRegisteredANM())
                .withLocationId(ChwNotificationDao.getSyncLocationId(baseEntityId))
                .withTeamId(sharedPreferences.fetchDefaultTeamId(sharedPreferences.fetchRegisteredANM()))
                .withTeam(sharedPreferences.fetchDefaultTeam(sharedPreferences.fetchRegisteredANM()))
                .withClientDatabaseVersion(BuildConfig.DATABASE_VERSION)
                .withClientApplicationVersion(BuildConfig.VERSION_CODE)
                .withDateCreated(new Date());

        String indicatorData = reportTittle == R.string.hps_monthly_reports_title ? hpsMonthlyReportObject.getIndicatorData().toString() : hpsAnnualReportObject.getIndicatorData().toString();
        baseEvent.addObs((new Obs())
                .withFormSubmissionField(Constants.FormConstants.FormSubmissionFields.REPORT_DATA)
                .withValue(indicatorData)
                .withFieldCode(Constants.FormConstants.FormSubmissionFields.REPORT_DATA)
                .withFieldType(CoreConstants.FORMSUBMISSION_FIELD).withFieldDataType(CoreConstants.TEXT).withParentCode("")
                .withHumanReadableValues(new ArrayList<>()));

        baseEvent.addObs((new Obs())
                .withFormSubmissionField(Constants.FormConstants.FormSubmissionFields.REPORT_DHIS_PAYLOAD)
                .withValue(new Gson().toJson(dhis2Report))
                .withFieldCode(Constants.FormConstants.FormSubmissionFields.REPORT_DHIS_PAYLOAD)
                .withFieldType(CoreConstants.FORMSUBMISSION_FIELD).withFieldDataType(CoreConstants.TEXT).withParentCode("")
                .withHumanReadableValues(new ArrayList<>()));


        JsonFormUtils.tagSyncMetadata(Utils.context().allSharedPreferences(), baseEvent);
        try {
            NCUtils.processEvent(baseEvent.getBaseEntityId(), new JSONObject(org.smartregister.chw.anc.util.JsonFormUtils.gson.toJson(baseEvent)));
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    public Dhis2Report getReport() {
        try {
            return buildDhis2Report();
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_view_menu, menu);
        menu.findItem(R.id.action_view_chw_sync_status).setVisible(true);


        // menu.findItem(R.id.action_upload_to_dhis2).setVisible(true);
        Calendar current = Calendar.getInstance();
        Calendar report = Calendar.getInstance();
        report.setTime(ReportUtils.getReportDate());

        boolean isPastMonth = report.get(Calendar.YEAR) < current.get(Calendar.YEAR) ||
                (report.get(Calendar.YEAR) == current.get(Calendar.YEAR) &&
                        report.get(Calendar.MONTH) < current.get(Calendar.MONTH));

        if (isPastMonth) {
            menu.findItem(R.id.action_upload_to_dhis2).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        int itemId = item.getItemId();
        if (itemId == R.id.action_view_chw_sync_status) {
            showCHWPopup(this, ReportDao.getChwsLastSyncDate());
            return true;
        } else if (itemId == R.id.action_upload_to_dhis2) {
            showConfirmationPopupWithProgress(this, ReportDao.getChwsLastSyncDate());
            return true;
        }
        return true;
    }

    private void showCHWPopup(Context context, List<CHW> chwList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("CHWs Sync Status");

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(android.R.layout.list_content, null);

        ListView listView = new ListView(context);
        CHWAdapter adapter = new CHWAdapter(context, chwList);
        listView.setAdapter(adapter);

        builder.setView(listView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void showConfirmationPopupWithProgress(Context context, List<CHW> chwList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("CHWs Sync Status");
        builder.setMessage("Please confirm that all data is complete and up-to-date based on the CHWs' last sync dates before proceeding.");

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(android.R.layout.list_content, null);

        ListView listView = new ListView(context);
        CHWAdapter adapter = new CHWAdapter(context, chwList);
        listView.setAdapter(adapter);

        builder.setView(listView);
        builder.setPositiveButton("Proceed", (dialog, which) -> {
            AlertDialog progressDialog = new AlertDialog.Builder(context)
                    .setTitle("Uploading")
                    .setMessage("Please wait while data to be sent to DHIS2 is being generated...")
                    .setCancelable(false)
                    .create();
            progressDialog.show();

            executor.execute(() -> {
                try {
                    // Will block here if not yet complete
                    Dhis2Report dhis2ReportReady = reportFuture != null ? reportFuture.get() : buildDhis2Report();
                    // Use the ready report
                    generateSendToDhis2Event(UUID.randomUUID().toString(), context, reportTittle);
                    ((Activity) context).runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Data successfully generated", Toast.LENGTH_LONG).show();
                    });
                } catch (Exception e) {
                    Timber.e(e);
                    ((Activity) context).runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Failed to generate data to be sent to DHIS2", Toast.LENGTH_LONG).show();
                    });
                }
            });
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}
