package org.smartregister.chw.hf.utils;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.ld.LDLibrary;
import org.smartregister.chw.ld.domain.Visit;
import org.smartregister.chw.ld.repository.VisitDetailsRepository;
import org.smartregister.chw.ld.repository.VisitRepository;
import org.smartregister.chw.ld.util.Constants;
import org.smartregister.chw.ld.util.VisitUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by Kassim Sheghembe on 2022-05-10
 */
public class LDVisitUtils extends VisitUtils {

    public static void processVisits(String baseEntityId) throws Exception {
        processVisits(LDLibrary.getInstance().visitRepository(), LDLibrary.getInstance().visitDetailsRepository(), baseEntityId);
    }

    public static void processVisits(VisitRepository visitRepository, VisitDetailsRepository visitDetailsRepository, String baseEntityId) throws Exception {
        Calendar calendar = Calendar.getInstance();

        List<Visit> visits = StringUtils.isNotBlank(baseEntityId) ?
                visitRepository.getAllUnSynced(calendar.getTime().getTime(), baseEntityId) :
                visitRepository.getAllUnSynced(calendar.getTime().getTime());

        List<Visit> ldVisits = new ArrayList<>();

        for (Visit visit: visits) {
            if (visit.getVisitType().equalsIgnoreCase(Constants.EVENT_TYPE.LD_GENERAL_EXAMINATION)) {
                JSONObject visitJson = new JSONObject(visit.getJson());
                JSONArray obs = visitJson.getJSONArray("obs");

                boolean isGeneralConditionDone = computeCompletionStatus(obs, "general_condition");
                boolean isPulseRateDone = computeCompletionStatus(obs, "pulse_rate");
                boolean isRespiratoryRateDone = computeCompletionStatus(obs, "respiratory_rate");
                boolean isTemperatureDone = computeCompletionStatus(obs, "temperature");
                boolean isSystolicDone = computeCompletionStatus(obs, "systolic");
                boolean isDiastolicDone = computeCompletionStatus(obs, "diastolic");
                boolean isUrineDone = computeCompletionStatus(obs, "urine");
                boolean isFundalHeightDone = computeCompletionStatus(obs, "fundal_height");
                boolean isPresentationDone = computeCompletionStatus(obs, "presentation");
                boolean isContractionInTenMinutesDone = computeCompletionStatus(obs, "contraction_in_ten_minutes");
                boolean isFetalHeartRateDone = computeCompletionStatus(obs, "fetal_heart_rate");

                boolean isVaginalExamDateDone = computeCompletionStatus(obs, "vaginal_exam_date");
                boolean isVaginalExamTimeDone = computeCompletionStatus(obs, "vaginal_exam_time");
                boolean isCervixStateDone = computeCompletionStatus(obs, "cervix_state");
                boolean isCervixDilationDone = computeCompletionStatus(obs, "cervix_dilation");
                boolean isPresentingPartDone = computeCompletionStatus(obs, "presenting_part");
                boolean isOcciputPositionDone = computeCompletionStatus(obs, "occiput_position");
                boolean isMouldingDone = computeCompletionStatus(obs, "moulding");
                boolean isStationDone = computeCompletionStatus(obs, "station");
                boolean isAmnioticFluidDone = computeCompletionStatus(obs, "amniotic_fluid");
                boolean isDecisionDone = computeCompletionStatus(obs, "decision");

                if (isGeneralConditionDone &&
                        isPulseRateDone &&
                        isRespiratoryRateDone &&
                        isTemperatureDone &&
                        isSystolicDone &&
                        isDiastolicDone &&
                        isUrineDone &&
                        isFundalHeightDone &&
                        isPresentationDone &&
                        isContractionInTenMinutesDone &&
                        isFetalHeartRateDone &&
                        isVaginalExamDateDone &&
                        isVaginalExamTimeDone &&
                        isCervixStateDone &&
                        isCervixDilationDone &&
                        isPresentingPartDone &&
                        isOcciputPositionDone &&
                        isMouldingDone &&
                        isStationDone &&
                        isAmnioticFluidDone &&
                        isDecisionDone) {
                    ldVisits.add(visit);
                }
            } else {
                ldVisits.add(visit);
            }
        }

        if (ldVisits.size() > 0) {
            processVisits(ldVisits, visitRepository, visitDetailsRepository);
        }
    }

    public static boolean computeCompletionStatus(JSONArray obs, String checkString) throws JSONException {
        int size = obs.length();
        for (int i = 0; i < size; i++) {
            JSONObject checkObj = obs.getJSONObject(i);
            if (checkObj.getString("fieldCode").equalsIgnoreCase(checkString)) {
                return true;
            }
        }
        return false;
    }

}
