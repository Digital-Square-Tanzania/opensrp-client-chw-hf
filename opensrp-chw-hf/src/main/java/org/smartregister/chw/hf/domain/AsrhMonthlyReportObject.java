package org.smartregister.chw.hf.domain;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AsrhMonthlyReportObject extends ReportObject {

    private final List<String> indicatorCodesWithAgeGroups = new ArrayList<>();

    private final String[] indicatorCodes = new String[]{
            "asrh-1",
            "asrh-2a", "asrh-2b", "asrh-2c", "asrh-2d", "asrh-2e", "asrh-2f", "asrh-2g", "asrh-2h",
            "asrh-3",
            "asrh-4a", "asrh-4b", "asrh-4c", "asrh-4d", "asrh-4e", "asrh-4f", "asrh-4g",
            "asrh-5a", "asrh-5b",
            "asrh-6a", "asrh-6b",
            "asrh-7a", "asrh-7b",
            "asrh-8a", "asrh-8b",
            "asrh-9a", "asrh-9b",
            "asrh-10"
    };

    private final String[] clientType = new String[]{"male", "female"};

    private final String[] indicatorAgeGroups = new String[]{"10-14", "15-19", "20-24"};

    private final Date reportDate;

    public AsrhMonthlyReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        setIndicatorCodesWithAgeGroups(indicatorCodesWithAgeGroups);
    }

    public static int calculateAsrhSpecificTotal(HashMap<String, Integer> indicators, String specificKey) {
        int total = 0;

        for (Map.Entry<String, Integer> entry : indicators.entrySet()) {
            String key = entry.getKey().toLowerCase();
            Integer value = entry.getValue();

            if (key.startsWith(specificKey.toLowerCase())) {
                total += value;
            }
        }

        return total;
    }

    public void setIndicatorCodesWithAgeGroups(List<String> indicatorCodesWithAgeGroups) {
        for (String indicatorCode : indicatorCodes) {
            for (String clientType : clientType) {
                for (String indicatorKey : indicatorAgeGroups) {
                    indicatorCodesWithAgeGroups.add(indicatorCode + "-" + clientType + "-" + indicatorKey);
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


        // Calculate and add total values
        for (String indicatorCode : indicatorCodes) {
            int maleTotal = calculateAsrhSpecificTotal(indicatorsValues, indicatorCode+ "-male");
            int femaleTotal = calculateAsrhSpecificTotal(indicatorsValues, indicatorCode + "-female");
            indicatorDataObject.put(indicatorCode + "-male-total", maleTotal);
            indicatorDataObject.put(indicatorCode + "-female-total", femaleTotal);
            indicatorDataObject.put(indicatorCode+ "-grand-total", maleTotal + femaleTotal);
        }

        return indicatorDataObject;
    }

}
