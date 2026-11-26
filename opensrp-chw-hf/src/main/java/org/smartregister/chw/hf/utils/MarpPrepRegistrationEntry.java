package org.smartregister.chw.hf.utils;

public enum MarpPrepRegistrationEntry {
    HIDDEN,
    PENDING,
    CONSENT_DECLINED,
    REGISTERED;

    /**
     * Evaluate the MARP profile row using recorded answers and evidence of a closed,
     * successfully processed local visit.
     * Consent is true for an existing registration, false for a recorded decline, and null when absent.
     */
    public static MarpPrepRegistrationEntry evaluate(boolean hasClosedAndProcessedVisit, int age,
                                                     String recordedHtsResult, String screeningHivStatus,
                                                     Boolean recordedPrepConsent) {
        if (Boolean.TRUE.equals(recordedPrepConsent)) {
            return REGISTERED;
        }
        if (!hasClosedAndProcessedVisit || age < 15
                || "positive".equalsIgnoreCase(screeningHivStatus)
                || MarpHts.isPositiveResult(recordedHtsResult)
                || "inconclusive".equalsIgnoreCase(recordedHtsResult)
                || "refused_results".equalsIgnoreCase(recordedHtsResult)) {
            return HIDDEN;
        }
        return Boolean.FALSE.equals(recordedPrepConsent) ? CONSENT_DECLINED : PENDING;
    }

    /** Use this decision for both the REGISTER button and its action. */
    public boolean canRegister() {
        return this == PENDING || this == CONSENT_DECLINED;
    }
}
