package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.hf.utils.Constants;


public class VmmcReportsViewActivity extends HfReportsViewActivity{

    public static void startMe(Activity activity, String reportPath, int reportTitle, String reportDate, String startDate, String endDate) {
        Intent intent = new Intent(activity, VmmcReportsViewActivity.class);
        intent.putExtra(ARG_REPORT_PATH, reportPath);
        intent.putExtra(ARG_REPORT_TITLE, reportTitle);
        intent.putExtra(ARG_REPORT_DATE, reportDate);
        intent.putExtra(ARG_REPORT_START_DATE, startDate);
        intent.putExtra(ARG_REPORT_END_DATE, endDate);
        intent.putExtra(ARG_REPORT_TYPE, Constants.ReportConstants.ReportTypes.VMMC_REPORT);
        activity.startActivity(intent);
    }
}
