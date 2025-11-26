package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONObject;
import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.domain.dhis2_reports.Dhis2ReportHistory;
import org.smartregister.chw.hf.repository.Dhis2ReportHistoryRepository;
import org.smartregister.chw.hf.utils.Constants;
import org.smartregister.chw.hf.utils.HfWebAppInterface;
import org.smartregister.chw.hf.utils.ReportUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

/**
 * WebView-based DHIS2 Summary that reuses the HPS HTML views but feeds data
 * from persisted history (report_data_json) instead of recomputing.
 */
public class Dhis2SummaryViewActivity extends HfReportsViewActivity {

    public static final String EXTRA_EVENT_ID = "EXTRA_EVENT_ID";

    public static void startMe(Activity activity, String eventId) {
        Intent intent = new Intent(activity, Dhis2SummaryViewActivity.class);
        intent.putExtra(EXTRA_EVENT_ID, eventId);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Prepare override and intent extras before base onCreate loads the WebView
        try {
            String eventId = getIntent().getStringExtra(EXTRA_EVENT_ID);
            Dhis2ReportHistoryRepository repo = new Dhis2ReportHistoryRepository();
            Dhis2ReportHistory h = !TextUtils.isEmpty(eventId) ? repo.getByEventId(eventId) : null;

            String reportCategory = (h != null && !TextUtils.isEmpty(h.reportType)) ? h.reportType : "hps_monthly";
            boolean isMonthly = "hps_monthly".equalsIgnoreCase(reportCategory);
            String reportPath = isMonthly ? Constants.ReportConstants.ReportPaths.HPS_MONTHLY_REPORT_PATH : Constants.ReportConstants.ReportPaths.HPS_ANNUAL_REPORT_PATH;
            int reportTitle = isMonthly ? R.string.hps_monthly_reports_title : R.string.hps_annual_reports_title;

            // Compute MM-yyyy for ReportUtils based on stored period (likely yyyyMM)
            String mmYYYY = null;
            if (h != null && !TextUtils.isEmpty(h.period)) {
                String p = h.period;
                if (p.length() == 6) {
                    // yyyyMM -> MM-yyyy
                    mmYYYY = p.substring(4, 6) + "-" + p.substring(0, 4);
                } else if (p.length() == 7 && p.contains("-")) {
                    // already like MM-yyyy
                    mmYYYY = p;
                } else if (p.length() == 4) {
                    // yyyy -> set Jan of that year for display
                    mmYYYY = "01-" + p;
                }
            }
            if (TextUtils.isEmpty(mmYYYY)) {
                // Fallback from eventDate
                if (h != null && h.eventDate != null) {
                    mmYYYY = new SimpleDateFormat("MM-yyyy", Locale.getDefault()).format(new Date(h.eventDate));
                } else {
                    mmYYYY = ReportUtils.getDefaultReportPeriod();
                }
            }

            // Build override data map for the appropriate report key
            Map<String, String> override = new HashMap<>();
            String key = isMonthly ? Constants.ReportConstants.HpsReportKeys.HPS_MONTHLY_REPORT : Constants.ReportConstants.HpsReportKeys.HPS_ANNUAL_REPORT;
            if (h != null && !TextUtils.isEmpty(h.reportDataJson)) {
                // Ensure valid JSON
                try { new JSONObject(h.reportDataJson); } catch (Exception ignore) { /* leave as-is */ }
                override.put(key, h.reportDataJson);
            }
            HfWebAppInterface.setOverride(Constants.ReportConstants.ReportTypes.HPS_REPORT, override);

            // Populate intent args expected by base activity
            Intent i = getIntent();
            i.putExtra(ARG_REPORT_PATH, reportPath);
            i.putExtra(ARG_REPORT_DATE, mmYYYY);
            i.putExtra(ARG_REPORT_TITLE, reportTitle);
            i.putExtra(ARG_REPORT_TYPE, Constants.ReportConstants.ReportTypes.HPS_REPORT);
        } catch (Exception e) {
            Timber.e(e);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        HfWebAppInterface.clearOverride();
    }
}

