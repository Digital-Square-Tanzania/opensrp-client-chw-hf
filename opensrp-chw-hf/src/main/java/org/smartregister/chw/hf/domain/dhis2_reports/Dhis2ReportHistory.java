package org.smartregister.chw.hf.domain.dhis2_reports;

public class Dhis2ReportHistory {
    public String eventId;
    public String baseEntityId;
    public Long eventDate;
    public String providerId;
    public String locationId;
    public String eventType;

    public String reportType; // hps_monthly | hps_annual | other
    public String period;     // yyyyMM or yyyy as applicable
    public String orgUnit;
    public String dataSet;
    public String completeDate;

    public String reportDataJson;
    public String dhis2PayloadJson;

    public Long createdAt;
    public Long lastInteractedWith;
}

