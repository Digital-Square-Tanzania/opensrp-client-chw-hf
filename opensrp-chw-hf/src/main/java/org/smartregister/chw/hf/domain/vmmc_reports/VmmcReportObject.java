//package org.smartregister.chw.hf.domain.vmmc_reports;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//import org.smartregister.chw.hf.dao.ReportDao;
//import org.smartregister.chw.hf.domain.ReportObject;
//
//import java.util.Date;
//
//public class VmmcReportObject extends ReportObject {
//
//    private final String[] vmmcQuestionsGroups = new String[]{"1", "2", "3-i", "3-ii", "4", "5", "6-a-i", "6-a-ii", "6-a-iii", "6-a-iv", "6-a-v", "6-a-vi", "6-a-vii",
//            "6-a-viii", "6-a-ix", "6-a-x", "6-a-xi", "6-a-xii", "6-b-i", "6-b-ii", "6-b-iii", "6-b-iv", "6-b-v", "6-b-vi", "6-b-vii", "6-b-viii", "6-b-ix", "6-b-x",
//            "6-b-xi", "6-b-xii", "7-i", "7-ii", "7-iii", "7-iv", "7-v", "7-vi", "7-vii", "7-viii",
//            "8-i", "8-ii", "8-iii", "8-iv", "8-v"
//    };
//
//    private final String[] vmmcAgeGroups = new String[]{
//            "1", "1-9", "10-14", "15-19", "20-24", "25-29", "30-34", "35-39", "40-44", "45-49", "50"
//    };
//
//    private final String[] vmmcGroups = new String[]{
//            "a", "cm", "dm"
//    };
//
//    private final Date reportDate;
//
//    private JSONObject jsonObject;
//
//    public VmmcReportObject(Date reportDate) {
//        super(reportDate);
//        this.reportDate = reportDate;
//    }
//
//    @Override
//    public JSONObject getIndicatorData() throws JSONException {
//
//        jsonObject = new JSONObject();
//
//
//        for (String questionGroup : vmmcQuestionsGroups) {   //rows
//            for (String ageGroup : vmmcAgeGroups) {  //columns
//                for (String vmmcGroup : vmmcGroups) {  //concstenate rows columns and gendergroup
//                    jsonObject.put("vmmc" + "-" + questionGroup + "-" + ageGroup + "-" + vmmcGroup,
//                            ReportDao.getReportPerIndicatorCode("vmmc" + "-" + questionGroup + "-" + ageGroup + "-" + vmmcGroup, reportDate));
//                }
//            }
//        }
//
//        funcGetTotal();
//
//        //total for qn 2 and 4
//        jsonObject.put("vmmc-2a-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-2a-grandtotal", reportDate));
//        jsonObject.put("vmmc-2b-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-2b-grandtotal", reportDate));
//        jsonObject.put("vmmc-2c-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-2c-grandtotal", reportDate));
//        jsonObject.put("vmmc-2d-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-2d-grandtotal", reportDate));
//        jsonObject.put("vmmc-2e-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-2e-grandtotal", reportDate));
//        jsonObject.put("vmmc-2f-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-2f-grandtotal", reportDate));
//        jsonObject.put("vmmc-2g-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-2g-grandtotal", reportDate));
//        jsonObject.put("vmmc-2h-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-2h-grandtotal", reportDate));
//        jsonObject.put("vmmc-4a-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-4a-grandtotal", reportDate));
//        jsonObject.put("vmmc-4b-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-4b-grandtotal", reportDate));
//        jsonObject.put("vmmc-4c-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-4c-grandtotal", reportDate));
//        jsonObject.put("vmmc-4d-grandtotal", ReportDao.getReportPerIndicatorCode("vmmc-4d-grandtotal", reportDate));
//
//
//        return jsonObject;
//    }
//
//    private int getTotalPerEachIndicator(String question) throws JSONException {
//        int totalOfDeviceGroup = 0;
//        int totalOfConvectionalGroup = 0;
//        int totalOfAgeGroup = 0;
//        int returnedValue = 0;
//        for (String age : vmmcAgeGroups) {
//            totalOfAgeGroup += (ReportDao.getReportPerIndicatorCode("vmmc" + "-"
//                    + question + "-" + age + "-" + "a", reportDate)
//                    + ReportDao.getReportPerIndicatorCode("vmmc" + "-"
//                    + question + "-" + age + "-" + "cm", reportDate)
//                    + ReportDao.getReportPerIndicatorCode("vmmc" + "-"
//                    + question + "-" + age + "-" + "dm", reportDate)
//            );
//
//
//            //Total of Device
//            totalOfDeviceGroup += ReportDao.getReportPerIndicatorCode("vmmc" + "-"
//                    + question + "-" + age + "-" + "cm", reportDate);
//
//            //Total of Device
//            totalOfConvectionalGroup += ReportDao.getReportPerIndicatorCode("vmmc" + "-"
//                    + question + "-" + age + "-" + "dm", reportDate);
//
//
//            //vmmc-8-v-TOTAL-dm
//            jsonObject.put("vmmc" + "-" + question + "-total-" + "cm", totalOfDeviceGroup);  //display the total for cm
//            jsonObject.put("vmmc" + "-" + question + "-total-" + "dm", totalOfConvectionalGroup);  //display the total for cm
//
//
//            returnedValue = totalOfAgeGroup;
//        }
//        return returnedValue;
//    }
//
//
//    private void funcGetTotal() throws JSONException {
//        int grandTotal = 0;
//        for (String question : vmmcQuestionsGroups) {
//            //vmmc-8-v-grandTotal
//            grandTotal += getTotalPerEachIndicator(question);
//            jsonObject.put("vmmc" + "-" + question + "-grandtotal", grandTotal); //total for all vmmc groups (grand_total)
//
//            grandTotal = 0;
//        }
//    }
//
//}

package org.smartregister.chw.hf.domain.vmmc_reports;

import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class VmmcReportObject extends ReportObject {

    private final String[] vmmcQuestionsGroups = new String[]{
            "1", "2", "3-i", "3-ii", "4", "5", "6-a-i", "6-a-ii", "6-a-iii", "6-a-iv", "6-a-v", "6-a-vi", "6-a-vii",
            "6-a-viii", "6-a-ix", "6-a-x", "6-a-xi", "6-a-xii", "6-b-i", "6-b-ii", "6-b-iii", "6-b-iv", "6-b-v", "6-b-vi", "6-b-vii", "6-b-viii", "6-b-ix", "6-b-x",
            "6-b-xi", "6-b-xii", "7-i", "7-ii", "7-iii", "7-iv", "7-v", "7-vi", "7-vii", "7-viii",
            "8-i", "8-ii", "8-iii", "8-iv", "8-v"
    };

    private final String[] vmmcAgeGroups = new String[]{
            "1", "1-9", "10-14", "15-19", "20-24", "25-29", "30-34", "35-39", "40-44", "45-49", "50"
    };

    private final String[] vmmcGroups = new String[]{
            "a", "cm", "dm"
    };

    private final Date reportDate;
    private JSONObject jsonObject;
    private Map<String, Integer> cachedData;

    public VmmcReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
        this.cachedData = new HashMap<>();
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        jsonObject = new JSONObject();

        // Fetch all data in one go and cache it
        fetchAllData();

        // Populate jsonObject with cached data
        for (String questionGroup : vmmcQuestionsGroups) {   //rows
            for (String ageGroup : vmmcAgeGroups) {  //columns
                for (String vmmcGroup : vmmcGroups) {  //concatenate rows columns and gender group
                    String key = "vmmc" + "-" + questionGroup + "-" + ageGroup + "-" + vmmcGroup;
                    jsonObject.put(key, cachedData.getOrDefault(key, 0));
                }
            }
        }

        funcGetTotal();

        // Total for qn 2 and 4
        for (String totalKey : new String[]{
                "vmmc-2a-grandtotal", "vmmc-2b-grandtotal", "vmmc-2c-grandtotal",
                "vmmc-2d-grandtotal", "vmmc-2e-grandtotal", "vmmc-2f-grandtotal",
                "vmmc-2g-grandtotal", "vmmc-2h-grandtotal", "vmmc-4a-grandtotal",
                "vmmc-4b-grandtotal", "vmmc-4c-grandtotal", "vmmc-4d-grandtotal"
        }) {
            jsonObject.put(totalKey, cachedData.getOrDefault(totalKey, 0));
        }

        return jsonObject;
    }

    private void fetchAllData() {
        // Cache all required indicator data
        for (String questionGroup : vmmcQuestionsGroups) {
            for (String ageGroup : vmmcAgeGroups) {
                for (String vmmcGroup : vmmcGroups) {
                    String key = "vmmc" + "-" + questionGroup + "-" + ageGroup + "-" + vmmcGroup;
                    cachedData.put(key, ReportDao.getReportPerIndicatorCode(key, reportDate));
                }
            }
        }

        // Cache totals for specific questions
        for (String totalKey : new String[]{
                "vmmc-2a-grandtotal", "vmmc-2b-grandtotal", "vmmc-2c-grandtotal",
                "vmmc-2d-grandtotal", "vmmc-2e-grandtotal", "vmmc-2f-grandtotal",
                "vmmc-2g-grandtotal", "vmmc-2h-grandtotal", "vmmc-4a-grandtotal",
                "vmmc-4b-grandtotal", "vmmc-4c-grandtotal", "vmmc-4d-grandtotal"
        }) {
            cachedData.put(totalKey, ReportDao.getReportPerIndicatorCode(totalKey, reportDate));
        }
    }

    private int getTotalPerEachIndicator(String question) throws JSONException {
        int totalOfDeviceGroup = 0;
        int totalOfConventionalGroup = 0;
        int totalOfAgeGroup = 0;

        for (String age : vmmcAgeGroups) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                totalOfAgeGroup += cachedData.getOrDefault("vmmc" + "-" + question + "-" + age + "-" + "a", 0)
                        + cachedData.getOrDefault("vmmc" + "-" + question + "-" + age + "-" + "cm", 0)
                        + cachedData.getOrDefault("vmmc" + "-" + question + "-" + age + "-" + "dm", 0);
                // Total of Device
                totalOfDeviceGroup += cachedData.getOrDefault("vmmc" + "-" + question + "-" + age + "-" + "cm", 0);

                // Total of Conventional
                totalOfConventionalGroup += cachedData.getOrDefault("vmmc" + "-" + question + "-" + age + "-" + "dm", 0);
            }


        }

        // Store totals in jsonObject
        jsonObject.put("vmmc" + "-" + question + "-total-" + "cm", totalOfDeviceGroup);
        jsonObject.put("vmmc" + "-" + question + "-total-" + "dm", totalOfConventionalGroup);

        return totalOfAgeGroup;
    }

    private void funcGetTotal() throws JSONException {
        int grandTotal;
        for (String question : vmmcQuestionsGroups) {
            grandTotal = getTotalPerEachIndicator(question);
            jsonObject.put("vmmc" + "-" + question + "-grandtotal", grandTotal);
        }
    }
}
