package org.smartregister.chw.hf.domain.hts_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.Date;

public class HtsMonthlyReportObject extends ReportObject {


    private final String[] htsQuestionsGroups = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "14-a",
            "14-b", "14-c", "14-d", "14-e", "14-f", "14-g", "14-h",
            "15-a", "15-b", "16", "17", "18", "19"
    };
    private final String[] htsGroups = new String[]{
            "KP1-i", "KP1-ii", "KP2", "KP3"
    };
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
//                for (String htsGroup : htsGroups) {
//                    for (String genderGroup : htsGenderGroups) {  //concstenate rows columns and gendergroup
//                        jsonObject.put("hts-monthly" + "-" + questionGroup + "-" + ageGroup + "-" + htsGroup + "-" + genderGroup,
//                                ReportDao.getReportPerIndicatorCode("hts-monthly" + "-" + questionGroup + "-" + ageGroup + "-" + htsGroup + "-" + genderGroup, reportDate));
//                    }
//                }
            }
        }
        // Question 11-13
        jsonObject.put("hts-monthly-11", ReportDao.getReportPerIndicatorCode("hts-monthly-11", reportDate));
        jsonObject.put("hts-monthly-12", ReportDao.getReportPerIndicatorCode("hts-monthly-12", reportDate));
        jsonObject.put("hts-monthly-13", ReportDao.getReportPerIndicatorCode("hts-monthly-13", reportDate));


        // get total of all Male & Female in Qn 2 & 7
        //and the whole total for both of them
        funcGetTotal();

        return jsonObject;
    }

    private int getTotalPerEachIndicator(String question, String htsgroup) throws JSONException {
        int totalOfGenderGiven = 0;
        int returnedValue = 0;
        for (String age : htsAgeGroups) {
            totalOfGenderGiven += (ReportDao.getReportPerIndicatorCode("hts-monthly" + "-"
                    + question + "-" + age + "-" + htsgroup + "-" + "ME", reportDate)
                    + ReportDao.getReportPerIndicatorCode("hts-monthly" + "-"
                    + question + "-" + age + "-" + htsgroup + "-" + "KE", reportDate));
            jsonObject.put("hts-monthly" + "-" + question + "-" + htsgroup + "-jumla-both-ME-KE", totalOfGenderGiven);  //display the total for both gender
            returnedValue = totalOfGenderGiven;
        }
        return returnedValue;
    }


    private void funcGetTotal() throws JSONException {
        int totalofthewholehtsgroup = 0;
        for (String question : htsQuestionsGroups) {
            for (String htsGroup : htsGroups) {
                totalofthewholehtsgroup += getTotalPerEachIndicator(question, htsGroup);
                jsonObject.put("hts-monthly" + "-" + question + "-jumla-both-ME-KE", totalofthewholehtsgroup); //total for all hts groups
            }
            totalofthewholehtsgroup = 0;
        }
    }

}
