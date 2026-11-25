package org.smartregister.chw.hf.utils;

import static org.smartregister.chw.hf.utils.MarpEnrollmentDate.observation;

import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.clientandeventmodel.Event;

public final class MarpPregnancyStatus {
    public static final String FIELD = "pregnancy_breastfeeding_status";
    public static final String PREGNANT = "pregnant";
    public static final String NEITHER = "not_pregnant_not_breastfeeding";
    public static final String BREASTFEEDING = "breastfeeding";

    private MarpPregnancyStatus() {
    }

    public static boolean appliesTo(String gender, String dominantGroup) {
        return Constants.FEMALE.equalsIgnoreCase(gender)
                && MarpPregnancyScreening.GROUP_CODE.equals(dominantGroup);
    }

    public static String fromScreening(Event screening) {
        if ("yes".equals(observation(screening, MarpPregnancyScreening.PREGNANCY_FIELD))) {
            return PREGNANT;
        }
        if ("yes".equals(observation(screening, MarpPregnancyScreening.BREASTFEEDING_FIELD))) {
            return BREASTFEEDING;
        }
        return "";
    }

    public static boolean isAnswer(String value) {
        return PREGNANT.equals(value) || NEITHER.equals(value) || BREASTFEEDING.equals(value);
    }
}
