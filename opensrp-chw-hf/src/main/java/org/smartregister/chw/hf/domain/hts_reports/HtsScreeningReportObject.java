package org.smartregister.chw.hf.domain.hts_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.Date;

import timber.log.Timber;

public class HtsScreeningReportObject extends ReportObject {

    private final String[] htsQuestionsGroups = new String[]{"1", "2", "3", "4", "5", "6"};
    private final String[] htsAgeGroups = new String[]{"2-9", "10-14", ">=15"};
    private final String[] htsGenderGroups = new String[]{"ME", "KE"};
    private final Date reportDate;
    private JSONObject jsonObject;

    public HtsScreeningReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        jsonObject = new JSONObject();
        for (String questionGroup : htsQuestionsGroups) {   //rows
            for (String ageGroup : htsAgeGroups) {  //columns
                for (String genderGroup : htsGenderGroups) {  //concstenate rows columns and gendergroup
                    Timber.tag("anganaili").d(""+ReportDao.getReportPerIndicatorCode("hts" + "-" + questionGroup + "-" + ageGroup + "-" + genderGroup, reportDate));
                    jsonObject.put("hts" + "-" + questionGroup + "-" + ageGroup + "-" + genderGroup,
                            ReportDao.getReportPerIndicatorCode("hts" + "-" + questionGroup + "-" + ageGroup + "-" + genderGroup, reportDate));
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
        for (String age : htsAgeGroups) {
            totalOfGenderGiven += (ReportDao.getReportPerIndicatorCode("hts" + "-"
                    + question + "-" + age + "-" + "ME", reportDate)
                    + ReportDao.getReportPerIndicatorCode("hts" + "-"
                    + question + "-" + age + "-" + "KE", reportDate));
            jsonObject.put("hts" + "-" + question + "-" + "jumla-both-ME-KE", totalOfGenderGiven);  //display the total for both gender
            returnedValue = totalOfGenderGiven;
        }
        return returnedValue;
    }


    private void funcGetTotal() throws JSONException {
        int totalofthewholehtsgroup = 0;
        for (String question : htsQuestionsGroups) {
            totalofthewholehtsgroup += getTotalPerEachIndicator(question);
            jsonObject.put("hts" + "-" + question + "-jumla-kuu", totalofthewholehtsgroup); //total for all hts groups
            totalofthewholehtsgroup = 0;
        }
    }

}
