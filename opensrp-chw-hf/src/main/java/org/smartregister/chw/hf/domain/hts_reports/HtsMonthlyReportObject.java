package org.smartregister.chw.hf.domain.hts_reports;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.Date;

import timber.log.Timber;

public class HtsMonthlyReportObject extends ReportObject {


    private final String[] htsQuestionsGroups = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    private final String[] htsQuestions14Groups = new String[]{"14-a", "14-b", "14-c", "14-d", "14-e", "14-f", "14-g", "14-h"};
    private final String[] htsQuestions1619Groups = new String[]{
            "16-a","16-b","16-c",
            "17-a", "17-b", "17-c",
            "18-a", "18-b", "18-c",
            "19-a", "19-b", "19-c"
    };
    private final String[] htsGroups = new String[]{
            "KP1-i", "KP1-ii", "KP2", "KP3"};
    private final String[] htsAgeGroups = new String[]{
            "<1", "1-4", "5-9", "10-14", "15-19", "20-24", "25-29", "30-34", "35-39", "40-44", "45-49", ">50"
    };
    private final String[] htsGenderGroups = new String[]{
            "ME", "KE"
    };

    private final Date reportDate;
    private JSONObject jsonObject;

    public HtsMonthlyReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        jsonObject = new JSONObject();
        for (String questionGroup : htsQuestionsGroups) {   //rows
            for (String ageGroup : htsAgeGroups) {  //columns
                for (String genderGroup : htsGenderGroups) {  //concatenate rows columns and gendergroup
                    jsonObject.put("hts-monthly" + "-" + questionGroup + "-" + ageGroup + "-" + genderGroup,
                            ReportDao.getReportPerIndicatorCode("hts-monthly" + "-" + questionGroup + "-" + ageGroup + "-" + genderGroup, reportDate));
                }
            }
        }
        // Question 11-13
        jsonObject.put("hts-monthly-11", ReportDao.getReportPerIndicatorCode("hts-monthly-11", reportDate));
        jsonObject.put("hts-monthly-12", ReportDao.getReportPerIndicatorCode("hts-monthly-12", reportDate));
        jsonObject.put("hts-monthly-13", ReportDao.getReportPerIndicatorCode("hts-monthly-13", reportDate));
        // Question 14
        for (String questionGroup : htsQuestions14Groups) {   //rows
            for (String htsGroup : htsGroups) {  //columns
                jsonObject.put("hts-monthly" + "-" + questionGroup + "-" + htsGroup,
                        ReportDao.getReportPerIndicatorCode("hts-monthly" + "-" + questionGroup + "-" + htsGroup, reportDate));
            }
        }
        // Question 15
        jsonObject.put("hts-monthly-15-a", ReportDao.getReportPerIndicatorCode("hts-monthly-15-a", reportDate));
        jsonObject.put("hts-monthly-15-b", ReportDao.getReportPerIndicatorCode("hts-monthly-15-b", reportDate));
        // Question 16-19
        for (String questionGroup : htsQuestions1619Groups) {   //rows
            jsonObject.put("hts-monthly" + "-" + questionGroup ,
                    ReportDao.getReportPerIndicatorCode("hts-monthly" + "-" + questionGroup, reportDate));
        }

        // get total of all Male & Female in Qn 1 to 10
        //and the whole total for both of them
        funcGetTotalQnOneTen();
        funcGetTotalQn1619();
        return jsonObject;
    }

    private int getTotalQnOneTenPerEachIndicator(String question) throws JSONException {
        int totalOfGenderGiven = 0;
        int jumlaMe = 0;
        int jumlaKe = 0;
        for (String age : htsAgeGroups) {
            jumlaMe += ReportDao.getReportPerIndicatorCode("hts-monthly" + "-"
                    + question + "-" + age + "-" + "ME", reportDate);
            jumlaKe += ReportDao.getReportPerIndicatorCode("hts-monthly" + "-"
                    + question + "-" + age + "-" + "KE", reportDate);
            totalOfGenderGiven = jumlaKe + jumlaMe;
            jsonObject.put("hts-monthly" + "-" + question + "-jumla-ME", jumlaMe);
            jsonObject.put("hts-monthly" + "-" + question + "-jumla-KE", jumlaKe);
        }
        return totalOfGenderGiven;
    }


    private void funcGetTotalQnOneTen() throws JSONException {
        int totalofthewholehtsgroup = 0;
        for (String question : htsQuestionsGroups) {
                totalofthewholehtsgroup += getTotalQnOneTenPerEachIndicator(question);
                jsonObject.put("hts-monthly" + "-" + question + "-jumla-both-ME-KE", totalofthewholehtsgroup); //total for all hts groups
                totalofthewholehtsgroup = 0;
        }
    }

    private void funcGetTotalQn1619() throws JSONException {
        int totalofthewholehtsgroup = 0;
        for (String questionGroup : htsQuestions1619Groups) {
            totalofthewholehtsgroup += ReportDao.getReportPerIndicatorCode("hts-monthly" + "-"
                    + questionGroup, reportDate);
            jsonObject.put("hts-monthly" + "-" + questionGroup.substring(0,
                    questionGroup.indexOf("-")) + "-jumla", totalofthewholehtsgroup); //total for all hts groups
            totalofthewholehtsgroup = 0;
        }
    }

}
