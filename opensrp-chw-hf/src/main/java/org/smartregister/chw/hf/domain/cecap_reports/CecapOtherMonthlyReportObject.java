package org.smartregister.chw.hf.domain.cecap_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CecapOtherMonthlyReportObject extends ReportObject {

    private final List<String> indicatorCodesWithAgeGroups = new ArrayList<>();

    private final String[] indicatorCodes = new String[]{
            "cecap-other-1", "cecap-other-2", "cecap-other-3"
    };

    private final String[] hivStatus = new String[]{"hiv-positive", "hiv-negative", "hiv-unknown"};

    private final String[] indicatorAgeGroups = new String[]{"15-19", "20-24", "25-29", "30-34", "35-39", "40-44", "45-49", "50+"};

    private final Date reportDate;

    public CecapOtherMonthlyReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        setIndicatorCodesWithAgeGroups(indicatorCodesWithAgeGroups);
    }

    public static int calculateAsrhSpecificTotal(HashMap<String, Integer> indicators, String specificKey) {
        int total = 0;

        for (Map.Entry<String, Integer> entry : indicators.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Integer value = entry.getValue();

            if (key.contains(specificKey.toLowerCase())) {
                total += value;
            }
        }

        return total;
    }

    public void setIndicatorCodesWithAgeGroups(List<String> indicatorCodesWithAgeGroups) {
        for (String indicatorCode : indicatorCodes) {
            for (String hivStatus : hivStatus) {
                for (String ageGroup : indicatorAgeGroups) {
                    indicatorCodesWithAgeGroups.add(indicatorCode + "-" + hivStatus + "-" + ageGroup);
                }
            }
        }

    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        HashMap<String, Integer> indicatorsValues = new HashMap<>();
        JSONObject indicatorDataObject = new JSONObject();
        for (String indicatorCode : indicatorCodesWithAgeGroups) {
            int value = ReportDao.getReportPerIndicatorCode(indicatorCode, reportDate);
            indicatorsValues.put(indicatorCode, value);
            indicatorDataObject.put(indicatorCode, value);
        }

        // Calculate and add total values for "totals"
        for (String indicatorCode : indicatorCodes) {
            int hivPositiveTotal = calculateAsrhSpecificTotal(indicatorsValues, indicatorCode + "-hiv-positive");
            int hivNegativeTotal = calculateAsrhSpecificTotal(indicatorsValues, indicatorCode + "-hiv-negative");
            int hivUnknownTotal = calculateAsrhSpecificTotal(indicatorsValues, indicatorCode + "-hiv-unknown");
            indicatorDataObject.put(indicatorCode + "-hiv-positive-total", hivPositiveTotal);
            indicatorDataObject.put(indicatorCode + "-hiv-negative-total", hivNegativeTotal);
            indicatorDataObject.put(indicatorCode + "-hiv-unknown-total", hivUnknownTotal);
            indicatorDataObject.put(indicatorCode + "-grand-total", hivPositiveTotal + hivNegativeTotal + hivUnknownTotal);
        }

        return indicatorDataObject;
    }

}