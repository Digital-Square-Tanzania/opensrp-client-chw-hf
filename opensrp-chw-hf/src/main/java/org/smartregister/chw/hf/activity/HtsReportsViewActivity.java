package org.smartregister.chw.hf.activity;

import android.app.Activity;
import android.content.Intent;

import org.smartregister.chw.hf.R;
import org.smartregister.chw.hf.utils.Constants;


public class HtsReportsViewActivity extends HfReportsViewActivity{
    public static void startMe(Activity activity, String reportPath, String reportDate) {
        Intent intent = new Intent(activity, HtsReportsViewActivity.class);
        intent.putExtra(ARG_REPORT_PATH, reportPath);
        intent.putExtra(ARG_REPORT_DATE, reportDate);
        intent.putExtra(ARG_REPORT_TITLE, R.string.hts_reports_subtitle);
        intent.putExtra(ARG_REPORT_TYPE, Constants.ReportConstants.ReportTypes.HTS_REPORT);
        activity.startActivity(intent);
    }
}
