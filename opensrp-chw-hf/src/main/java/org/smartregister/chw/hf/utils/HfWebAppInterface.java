package org.smartregister.chw.hf.utils;

import android.content.Context;
import android.webkit.JavascriptInterface;

import static org.smartregister.chw.hf.utils.Constants.ReportConstants.PMTCTReportKeys.EID_MONTHLY;
import static org.smartregister.chw.hf.utils.Constants.ReportConstants.PMTCTReportKeys.THREE_MONTHS;
import static org.smartregister.chw.hf.utils.Constants.ReportConstants.PMTCTReportKeys.TWELVE_MONTHS;
import static org.smartregister.chw.hf.utils.Constants.ReportConstants.PMTCTReportKeys.TWENTY_FOUR_MONTHS;
import static org.smartregister.util.Utils.getAllSharedPreferences;

public class HfWebAppInterface {
    Context mContext;

    String reportType;


    public HfWebAppInterface(Context c, String reportType) {
        mContext = c;
        this.reportType = reportType;
    }

    @JavascriptInterface
    public String getData(String key) {
        if (reportType.equalsIgnoreCase(Constants.ReportConstants.ReportTypes.PMTCT_REPORT)) {
            switch (key) {
                case THREE_MONTHS:
                    ReportUtils.setPrintJobName("report_ya_miezi_mitatu-" + ReportUtils.getReportPeriod() + ".pdf");
                    return ReportUtils.PMTCTReports.computeThreeMonths(ReportUtils.getReportDate());
                case TWELVE_MONTHS:
                    ReportUtils.setPrintJobName("report_ya_miezi_kumi_na_mbili-" + ReportUtils.getReportPeriod() + ".pdf");
                    return ReportUtils.PMTCTReports.computeTwelveMonths(ReportUtils.getReportDate());
                case TWENTY_FOUR_MONTHS:
                    ReportUtils.setPrintJobName("report_ya_miaka_miwili-" + ReportUtils.getReportPeriod() + ".pdf");
                    return ReportUtils.PMTCTReports.computeTwentyFourMonths(ReportUtils.getReportDate());
                case EID_MONTHLY:
                    ReportUtils.setPrintJobName("report_ya_mwezi-" + ReportUtils.getReportPeriod() + ".pdf");
                    return ReportUtils.PMTCTReports.computeEIDMonthly(ReportUtils.getReportDate());
                default:
                    return "";
            }
        }
        if (reportType.equalsIgnoreCase(Constants.ReportConstants.ReportTypes.PNC_REPORT)) {
            ReportUtils.setPrintJobName("pnc_report_ya_mwezi-" + ReportUtils.getReportPeriod() + ".pdf");
            return ReportUtils.PNCReports.computePncReport(ReportUtils.getReportDate());
        }
        if (reportType.equalsIgnoreCase(Constants.ReportConstants.ReportTypes.ANC_REPORT)) {
            ReportUtils.setPrintJobName("anc_report_ya_mwezi-" + ReportUtils.getReportPeriod() + ".pdf");
            return ReportUtils.ANCReports.computeAncReport(ReportUtils.getReportDate());
        }

        return "";
    }

    @JavascriptInterface
    public String getDataPeriod() {
        return ReportUtils.getReportPeriod();
    }

    @JavascriptInterface
    public String getReportingFacility() {
        return getAllSharedPreferences().fetchCurrentLocality();
    }
}