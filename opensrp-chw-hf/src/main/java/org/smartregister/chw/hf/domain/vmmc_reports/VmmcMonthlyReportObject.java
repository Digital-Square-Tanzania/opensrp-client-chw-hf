package org.smartregister.chw.hf.domain.vmmc_reports;

import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.dao.ReportDao;
import org.smartregister.chw.hf.domain.ReportObject;

import java.util.Date;

public class VmmcMonthlyReportObject extends ReportObject {


    private final String[] vmmcQuestionsGroups = new String[]{"1","2","3-i","3-ii","4","5","6-a-i","6-a-ii","6-a-iii","6-a-iv","6-a-v","6-a-vi","6-a-vii",
            "6-b-i","6-b-ii","6-b-iii","6-b-iv","6-b-v","6-b-vi","6-b-vii",
            "7-i","7-ii","7-iii","7-iv","7-v","7-vi","7-vii","7-viii",
            "8-i","8-ii","8-iii","8-iv","8-v"
    };
    private final String[] vmmcAgeGroups = new String[]{
            "1","1-9","10-14","15-19","20-24","25-29","30-34","35-39","40-44","45-49","50"
    };
    private final String[] vmmcGroups = new String[]{
            "a","cm","dm"
    };

    private final Date reportDate;
    private JSONObject jsonObject ;

    public VmmcMonthlyReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;
    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        jsonObject = new JSONObject();
        for (String questionGroup : vmmcQuestionsGroups) {   //rows
            for (String ageGroup : vmmcAgeGroups) {  //columns
                    for (String vmmcGroups : vmmcGroups) {  //concstenate rows columns and gendergroup
                        jsonObject.put("vmmc" + "-" + questionGroup + "-" + ageGroup + "-" + vmmcGroups,
                                ReportDao.getReportPerIndicatorCode("vmmc" + "-" + questionGroup + "-" + ageGroup +"-" + vmmcGroups, reportDate));
                    }
            }
        }
        // get total of all Male & Female in Qn 2 & 7
        //and the whole total for both of them
//        funcGetTotal();

        return jsonObject;
    }

//    private int getTotalPerEachIndicator(String question,String kvpgroup) throws JSONException {
//        int  totalOfGenderGiven = 0;
//        int returnedValue = 0;
//        for (String age: vmmcAgeGroups){
//                totalOfGenderGiven += (ReportDao.getReportPerIndicatorCode("kvp" + "-"
//                        + question + "-" + age + "-" + kvpgroup + "-" + "ME", reportDate)
//                +ReportDao.getReportPerIndicatorCode("kvp" + "-"
//                        + question + "-" + age + "-" + kvpgroup + "-" + "KE", reportDate));
//            jsonObject.put("kvp"+"-"+question+"-"+kvpgroup+"-jumla-both-ME-KE",totalOfGenderGiven);  //display the total for both gender
//            returnedValue = totalOfGenderGiven;
//        }
//        return returnedValue;
//    }
//
//
//    private void funcGetTotal() throws JSONException {
//        int totalofthewholekvpgroup = 0;
//        for (String question: vmmcQuestionsGroups) {
//                for (String kvpGroup : vmmcGroups) {
//                    totalofthewholekvpgroup+=getTotalPerEachIndicator(question,kvpGroup);
//                    jsonObject.put("kvp"+"-"+question+"-jumla-kuu",totalofthewholekvpgroup); //total for all kvp groups
//                }
//            totalofthewholekvpgroup = 0;
//        }
//    }

}