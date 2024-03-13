package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.hf.utils.Constants;


public class AsrhReportsViewActivity extends HfReportsViewActivity {
    public static void startMe(Activity activity, String reportPath, int reportTitle, String reportDate) {
        Intent intent = new Intent(activity, AsrhReportsViewActivity.class);
        intent.putExtra(ARG_REPORT_PATH, reportPath);
        intent.putExtra(ARG_REPORT_DATE, reportDate);
        intent.putExtra(ARG_REPORT_TITLE, reportTitle);
        intent.putExtra(ARG_REPORT_TYPE, Constants.ReportConstants.ReportTypes.ASRH_REPORT);
        activity.startActivity(intent);
    }
}
