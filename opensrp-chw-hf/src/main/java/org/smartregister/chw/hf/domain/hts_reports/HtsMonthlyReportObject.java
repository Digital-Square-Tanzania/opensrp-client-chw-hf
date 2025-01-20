package org.smartregister.chw.hf.domain.hts_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.Date;

public class HtsMonthlyReportObject extends ReportObject {


    private final String[] kvpQuestionsGroups = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14-a",
            "14-b", "14-c", "14-d", "14-e", "14-f", "14-g", "14-h",
            "15-a", "15-b", "16", "17", "18", "19"
    };
    private final String[] kvpAgeGroups = new String[]{
            "<1", "1-4", "5-9", "10-14", "15-19", "20-24", "25-29", "30-34", "35-39", "40-44", "45-49", ">=50"
    };
    private final String[] kvpGenderGroups = new String[]{
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
        for (String questionGroup : kvpQuestionsGroups) {   //rows
            for (String ageGroup : kvpAgeGroups) {  //columns
                for (String genderGroup : kvpGenderGroups) {  //concstenate rows columns and gendergroup
                    jsonObject.put("kvp" + "-" + questionGroup + "-" + ageGroup + "-" + genderGroup,
                            ReportDao.getReportPerIndicatorCode("kvp" + "-" + questionGroup + "-" + ageGroup + "-" + genderGroup, reportDate));
                }
            }
        }
        // get total of all Male & Female in Qn 2 & 7
        //and the whole total for both of them
        funcGetTotal();

        return jsonObject;
    }

    private int getTotalPerEachIndicator(String question) throws JSONException {
        int totalOfGenderGiven = 0;
        int returnedValue = 0;
        for (String age : kvpAgeGroups) {
            totalOfGenderGiven += (ReportDao.getReportPerIndicatorCode("kvp" + "-"
                    + question + "-" + age  + "-" + "ME", reportDate)
                    + ReportDao.getReportPerIndicatorCode("kvp" + "-"
                    + question + "-" + age + "-" + "KE", reportDate));
            jsonObject.put("kvp"  + "-jumla-both-ME-KE", totalOfGenderGiven);  //display the total for both gender
            returnedValue = totalOfGenderGiven;
        }
        return returnedValue;
    }


    private void funcGetTotal() throws JSONException {
        int totalofthewholekvpgroup = 0;
        for (String question : kvpQuestionsGroups) {
            totalofthewholekvpgroup += getTotalPerEachIndicator(question);
            jsonObject.put("kvp" + "-" + question + "-jumla-kuu", totalofthewholekvpgroup); //total for all kvp
            totalofthewholekvpgroup = 0;
        }
    }

}
