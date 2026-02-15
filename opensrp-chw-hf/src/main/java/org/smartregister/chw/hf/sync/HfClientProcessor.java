package org.smartregister.chw.hf.sync;

import static org.smartregister.chw.anc.util.Constants.EVENT_TYPE.DELETE_EVENT;
import static org.smartregister.chw.core.utils.CoreConstants.EventType.ANC_FOLLOWUP_CLIENT_REGISTRATION;
import static org.smartregister.chw.core.utils.CoreConstants.EventType.ANC_PARTNER_TESTING;
import static org.smartregister.chw.core.utils.CoreConstants.EventType.ANC_PREGNANCY_CONFIRMATION;
import static org.smartregister.chw.hf.interactor.FpRegistrationDetailsInteractor.FP_REGISTRATION_EVENT;
import static org.smartregister.chw.hf.utils.Constants.Events.ANC_FIRST_FACILITY_VISIT;
import static org.smartregister.chw.hf.utils.Constants.Events.ANC_RECURRING_FACILITY_VISIT;
import static org.smartregister.chw.hf.utils.Constants.Events.HEI_FOLLOWUP;
import static org.smartregister.chw.hf.utils.Constants.Events.HEI_NEGATIVE_INFANT;
import static org.smartregister.chw.hf.utils.Constants.Events.HEI_POSITIVE_INFANT;
import static org.smartregister.chw.hf.utils.Constants.Events.LD_ACTIVE_MANAGEMENT_OF_3RD_STAGE_OF_LABOUR;
import static org.smartregister.chw.hf.utils.Constants.Events.LD_GENERAL_EXAMINATION;
import static org.smartregister.chw.hf.utils.Constants.Events.LD_PARTOGRAPHY;
import static org.smartregister.chw.hf.utils.Constants.Events.LD_POST_DELIVERY_MOTHER_MANAGEMENT;
import static org.smartregister.chw.hf.utils.Constants.Events.LD_REGISTRATION;
import static org.smartregister.chw.hf.utils.Constants.Events.PNC_CHILD_FOLLOWUP;
import static org.smartregister.chw.hf.utils.Constants.Events.PNC_VISIT;
import static org.smartregister.chw.hf.utils.Constants.FormConstants.FormSubmissionFields.CTC_NUMBER;
import static org.smartregister.chw.hf.utils.Constants.FormConstants.FormSubmissionFields.HIV_TEST_RESULT;
import static org.smartregister.chw.hf.utils.Constants.FormConstants.FormSubmissionFields.HIV_TEST_RESULT_DATE;
import static org.smartregister.chw.hf.utils.Constants.FormConstants.FormSubmissionFields.TYPE_OF_HIV_TEST;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.anc.util.NCUtils;
import org.smartregister.chw.core.dao.EventDao;
import org.smartregister.chw.core.sync.CoreClientProcessor;
import org.smartregister.chw.fp.util.FamilyPlanningConstants;
import org.smartregister.chw.hf.dao.FamilyDao;
import org.smartregister.chw.hf.dao.HeiDao;
import org.smartregister.chw.hf.dao.HfPmtctDao;
import org.smartregister.chw.hf.domain.hps_reports.HpsAnnualCensusRegister;
import org.smartregister.chw.hf.repository.HpsAnnualCensorReportsRepository;
import org.smartregister.chw.pmtct.util.Constants;
import org.smartregister.domain.Event;
import org.smartregister.domain.Obs;
import org.smartregister.domain.db.EventClient;
import org.smartregister.domain.jsonmapping.ClientClassification;
import org.smartregister.domain.jsonmapping.Table;
import org.smartregister.sync.ClientProcessorForJava;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

public class HfClientProcessor extends CoreClientProcessor {

    private static final String HPS_FIELD_YEAR = "year";
    private static final String HPS_FIELD_SELECT_AGE_GROUP = "select_age_group";
    private static final String ANTIBODY_TEST = "Antibody Test";

    private static final List<String> HTS_EVENT_TYPES = Collections.unmodifiableList(Arrays.asList(
            org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_SERVICES,
            org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_FIRST_HIV_TEST,
            org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_SECOND_HIV_TEST,
            org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_UNIGOLD_HIV_TEST,
            org.smartregister.chw.hts.util.Constants.EVENT_TYPE.REPEAT_FIRST_HIV_TEST
    ));

    private static final Set<String> VISIT_EVENTS_TO_PROCESS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ANC_PREGNANCY_CONFIRMATION,
            ANC_FOLLOWUP_CLIENT_REGISTRATION,
            ANC_FIRST_FACILITY_VISIT,
            ANC_RECURRING_FACILITY_VISIT,
            PNC_VISIT,
            PNC_CHILD_FOLLOWUP,
            LD_PARTOGRAPHY,
            LD_REGISTRATION,
            LD_ACTIVE_MANAGEMENT_OF_3RD_STAGE_OF_LABOUR,
            LD_GENERAL_EXAMINATION,
            LD_POST_DELIVERY_MOTHER_MANAGEMENT,
            ANC_PARTNER_TESTING,
            org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BEHAVIORAL_SERVICE_VISIT,
            org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_BIO_MEDICAL_SERVICE_VISIT,
            org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_STRUCTURAL_SERVICE_VISIT,
            org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.KVP_OTHER_SERVICE_VISIT,
            org.smartregister.chw.kvp.util.Constants.EVENT_TYPE.PrEP_FOLLOWUP_VISIT,
            org.smartregister.chw.vmmc.util.Constants.EVENT_TYPE.VMMC_SERVICES,
            org.smartregister.chw.vmmc.util.Constants.EVENT_TYPE.VMMC_PROCEDURE,
            org.smartregister.chw.vmmc.util.Constants.EVENT_TYPE.VMMC_DISCHARGE,
            org.smartregister.chw.vmmc.util.Constants.EVENT_TYPE.VMMC_FOLLOW_UP_VISIT,
            org.smartregister.chw.vmmc.util.Constants.EVENT_TYPE.VMMC_NOTIFIABLE_EVENTS,
            org.smartregister.chw.cecap.util.Constants.EVENT_TYPE.CECAP_FOLLOW_UP_VISIT,
            Constants.EVENT_TYPE.PMTCT_FOLLOWUP,
            FamilyPlanningConstants.EVENT_TYPE.FP_POINT_OF_SERVICE_DELIVERY,
            FamilyPlanningConstants.EVENT_TYPE.FP_COUNSELING,
            FamilyPlanningConstants.EVENT_TYPE.FP_PROVIDE_METHOD,
            FamilyPlanningConstants.EVENT_TYPE.FP_OTHER_SERVICES,
            org.smartregister.chw.sbc.util.Constants.EVENT_TYPE.SBC_FOLLOW_UP_VISIT,
            FP_REGISTRATION_EVENT,
            FamilyPlanningConstants.EVENT_TYPE.FP_ECP_PROVISION,
            FamilyPlanningConstants.EVENT_TYPE.FP_ECP_SCREENING
    )));

    private static final Set<String> HPS_REAL_COLUMNS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "amount_of_solid_waste_generated_annually_tons",
            "amount_of_solid_waste_disposed_at_a_designated_site_annually_tons"
    )));

    private static final Set<String> HPS_TEXT_COLUMNS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            HPS_FIELD_SELECT_AGE_GROUP,
            "select_centers_category",
            "types_of_pesticides_used_ponds",
            "types_of_pesticides_used_cans",
            "types_of_pesticides_used_drums",
            "types_of_pesticides_used_barrels",
            "types_of_pesticides_used_coconut_shells",
            "amount_of_pesticide_used_ponds",
            "amount_of_pesticide_used_cans",
            "amount_of_pesticide_used_drums",
            "amount_of_pesticide_used_barrels",
            "amount_of_pesticide_used_coconut_shells"
    )));

    private static final List<String> PMTCT_FOLLOWUP_TABLES = Collections.unmodifiableList(Arrays.asList(
            "ec_ld_partograph",
            "ec_pmtct_followup",
            "ec_pmtct_hvl_results",
            "ec_pmtct_cd4_results",
            "ec_hei_followup",
            "ec_hei_hiv_results",
            "ec_anc_followup",
            "ec_pnc_followup",
            "ec_prep_followup",
            "ec_cecap_test_results",
            "ec_kvp_hepatitis_test_results"
    ));

    private HfClientProcessor(Context context) {
        super(context);
    }

    public static ClientProcessorForJava getInstance(Context context) {
        if (instance == null) {
            instance = new HfClientProcessor(context);
        }
        return instance;
    }

    @Override
    public boolean processHfReportEvents() {
        return true;
    }

    @Override
    public boolean saveReportDateSent() {
        return false;
    }

    @Override
    protected void processEvents(ClientClassification clientClassification, Table vaccineTable, Table serviceTable, EventClient eventClient, Event event, String eventType) throws Exception {
        super.processEvents(clientClassification, vaccineTable, serviceTable, eventClient, event, eventType);

        String matchedHtsEventType = findMatchingHtsEventType(eventType);
        if (matchedHtsEventType != null) {
            if (!processHtsEvent(eventClient, clientClassification, matchedHtsEventType)) {
                return;
            }
        } else if (!processNonHtsEvent(eventType, eventClient, event, clientClassification)) {
            return;
        }

        // Used to fix instances where clients were registered without a DOB on past app version leading to app crashes.
        FamilyDao.fixClientsWithNullDob();
    }

    static String findMatchingHtsEventType(String eventType) {
        for (String htsEventType : HTS_EVENT_TYPES) {
            if (eventType.contains(htsEventType)) {
                return htsEventType;
            }
        }
        return null;
    }

    private boolean processHtsEvent(EventClient eventClient, ClientClassification clientClassification, String htsEventType) throws Exception {
        Event currentEvent = eventClient.getEvent();
        if (currentEvent == null) {
            return false;
        }

        processVisitEvent(eventClient);
        currentEvent.setEventType(htsEventType);
        processEvent(currentEvent, eventClient.getClient(), clientClassification);
        return true;
    }

    private boolean processNonHtsEvent(String eventType, EventClient eventClient, Event event, ClientClassification clientClassification) throws Exception {
        if (VISIT_EVENTS_TO_PROCESS.contains(eventType)) {
            return processVisitAndClientEvent(eventClient, clientClassification);
        }

        switch (eventType) {
            case org.smartregister.chw.hf.utils.Constants.Events.SEND_MONTHLY_MTUHA_BOOK_3_TO_DHIS2:
            case org.smartregister.chw.hf.utils.Constants.Events.SEND_ANNUAL_REPORTS_TO_DHIS2:
                saveDhis2History(event);
                break;
            case HEI_FOLLOWUP:
            case HEI_POSITIVE_INFANT:
            case HEI_NEGATIVE_INFANT:
                processHeiEvent(eventClient, clientClassification);
                break;
            case org.smartregister.chw.ld.util.Constants.EVENT_TYPE.VOID_EVENT:
            case DELETE_EVENT:
                processDeleteEvent(eventClient.getEvent());
                // Intentional fall-through to preserve existing behavior for delete/void events.
            case org.smartregister.chw.hps.util.Constants.EVENT_TYPE.HPS_ANNUAL_CENSUS:
                processHpsAnnualCensusRegisterEvent(eventClient.getEvent());
                break;
            default:
                break;
        }

        return true;
    }

    private boolean processVisitAndClientEvent(EventClient eventClient, ClientClassification clientClassification) throws Exception {
        Event currentEvent = eventClient.getEvent();
        if (currentEvent == null) {
            return false;
        }

        processVisitEvent(eventClient);
        processEvent(currentEvent, eventClient.getClient(), clientClassification);
        return true;
    }

    private void processHeiEvent(EventClient eventClient, ClientClassification clientClassification) throws Exception {
        processVisitEvent(eventClient);
        processEvent(eventClient.getEvent(), eventClient.getClient(), clientClassification);
        processHeiFollowupCEvent(eventClient.getEvent());
    }

    private void saveDhis2History(Event event) {
        try {
            new org.smartregister.chw.hf.repository.Dhis2ReportHistoryRepository().saveFromEvent(event);
        } catch (Exception e) {
            Timber.e(e, "Failed to persist DHIS2 history from event");
        }
    }

    private void processHpsAnnualCensusRegisterEvent(Event event) {
        try {
            HpsAnnualCensusRegister annualCensusRegister = createAnnualCensusRegister(event);
            populateAnnualCensusIndicators(annualCensusRegister, event.getObs());

            if (isMissingAnnualCensusIdentifiers(annualCensusRegister)) {
                Timber.w("Skipping HPS annual census save: missing year/provider_id (year=%s, provider_id=%s)",
                        String.valueOf(annualCensusRegister.getYear()), annualCensusRegister.getProviderId());
                return;
            }

            new HpsAnnualCensorReportsRepository().save(annualCensusRegister);
        } catch (Exception e) {
            Timber.e(e, "Error processing HPS Annual Census register event");
        }
    }

    private HpsAnnualCensusRegister createAnnualCensusRegister(Event event) {
        HpsAnnualCensusRegister annualCensusRegister = new HpsAnnualCensusRegister();
        annualCensusRegister.setBaseEntityId(event.getBaseEntityId());
        annualCensusRegister.setProviderId(event.getProviderId());
        annualCensusRegister.setLastInteractedWith(event.getVersion());
        return annualCensusRegister;
    }

    private void populateAnnualCensusIndicators(HpsAnnualCensusRegister annualCensusRegister, List<Obs> censusObs) {
        if (censusObs == null || censusObs.isEmpty()) {
            return;
        }

        for (Obs obs : censusObs) {
            applyAnnualCensusObservation(annualCensusRegister, obs);
        }
    }

    static void applyAnnualCensusObservation(HpsAnnualCensusRegister annualCensusRegister, Obs obs) {
        String field = obs.getFormSubmissionField();
        String stringValue = extractObservationStringValue(obs);

        if (HPS_FIELD_YEAR.equals(field)) {
            if (hasText(stringValue)) {
                try {
                    annualCensusRegister.setYear(Integer.valueOf(stringValue.trim()));
                } catch (NumberFormatException ignored) {
                    // Leave year as null when the value cannot be parsed.
                }
            }
            return;
        }

        if (HPS_FIELD_SELECT_AGE_GROUP.equals(field)) {
            String value = getObservationTextValue(obs, stringValue);
            annualCensusRegister.setSelectAgeGroup(value);
            annualCensusRegister.putString(field, value);
            return;
        }

        if (HPS_TEXT_COLUMNS.contains(field)) {
            annualCensusRegister.putString(field, getObservationTextValue(obs, stringValue));
            return;
        }

        if (HPS_REAL_COLUMNS.contains(field)) {
            if (hasText(stringValue)) {
                try {
                    annualCensusRegister.putReal(field, Double.valueOf(stringValue.trim()));
                } catch (NumberFormatException ignored) {
                    // Skip invalid double values.
                }
            }
            return;
        }

        if (hasText(stringValue)) {
            try {
                annualCensusRegister.putInteger(field, Integer.valueOf(stringValue.trim()));
            } catch (NumberFormatException e) {
                // Preserve non-integer values as text to avoid data loss.
                annualCensusRegister.putString(field, stringValue);
            }
        }
    }

    static String extractObservationStringValue(Obs obs) {
        Object rawValue = obs.getValue();
        return rawValue != null ? String.valueOf(rawValue) : null;
    }

    private static String getObservationTextValue(Obs obs, String fallbackValue) {
        return (obs.getValues() != null && !obs.getValues().isEmpty()) ? obs.getValues().toString() : fallbackValue;
    }

    private static boolean hasText(String value) {
        return value != null && value.trim().length() > 0;
    }

    private boolean isMissingAnnualCensusIdentifiers(HpsAnnualCensusRegister annualCensusRegister) {
        return annualCensusRegister.getYear() == null
                || annualCensusRegister.getProviderId() == null
                || annualCensusRegister.getProviderId().trim().isEmpty();
    }

    private void processVisitEvent(EventClient eventClient) {
        try {
            NCUtils.processHomeVisit(eventClient); // save locally
        } catch (Exception e) {
            String formID = (eventClient != null && eventClient.getEvent() != null) ? eventClient.getEvent().getFormSubmissionId() : "no form id";
            Timber.e("Form id " + formID + ". " + e.toString());
        }
    }

    @Override
    protected String getHumanReadableConceptResponse(String value, Object object) {
        try {
            if (StringUtils.isBlank(value) || (object != null && !(object instanceof Obs))) {
                return value;
            }
            // Skip human readable values and just get values which would aid in translations.
            final String valuesKey = "values";
            List<?> values = new ArrayList<>();

            Object valueObject = getValue(object, valuesKey);
            if (valueObject instanceof List) {
                values = (List<?>) valueObject;
            }
            if (object == null || values.isEmpty()) {
                return value;
            }

            return values.size() == 1 ? String.valueOf(values.get(0)) : values.toString();

        } catch (Exception e) {
            Timber.e(e);
        }
        return value;
    }

    private void processHeiFollowupCEvent(Event event) {
        if (event == null) {
            return;
        }

        List<Obs> heiFollowupObs = event.getObs();
        if (heiFollowupObs == null || heiFollowupObs.isEmpty()) {
            return;
        }

        String typeOfHivTest = null;
        String hivTestResult = null;
        String hivTestResultDate = null;
        String ctcNumber = null;

        for (Obs obs : heiFollowupObs) {
            if (TYPE_OF_HIV_TEST.equals(obs.getFormSubmissionField())) {
                typeOfHivTest = (String) obs.getValue();
            } else if (HIV_TEST_RESULT.equals(obs.getFormSubmissionField())) {
                hivTestResult = (String) obs.getValue();
            } else if (HIV_TEST_RESULT_DATE.equals(obs.getFormSubmissionField())) {
                hivTestResultDate = (String) obs.getValue();
            } else if (CTC_NUMBER.equals(obs.getFormSubmissionField())) {
                ctcNumber = (String) obs.getValue();
            }
        }

        if (ANTIBODY_TEST.equals(typeOfHivTest)) {
            HeiDao.saveAntiBodyTestResults(event.getBaseEntityId(), event.getFormSubmissionId(), hivTestResult, hivTestResultDate, ctcNumber);
        }
    }

    @Override
    public void processDeleteEvent(Event event) {
        try {
            boolean hasDeleteFormSubmissionId = event.getDetails().containsKey(org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID);
            String formSubmissionId = hasDeleteFormSubmissionId
                    ? event.getDetails().get(org.smartregister.chw.anc.util.Constants.JSON_FORM_EXTRA.DELETE_FORM_SUBMISSION_ID)
                    : event.getFormSubmissionId();

            if (hasDeleteFormSubmissionId) {
                EventDao.deleteVaccineByFormSubmissionId(formSubmissionId);
                EventDao.deleteVisitByFormSubmissionId(formSubmissionId);
                EventDao.deleteServiceByFormSubmissionId(formSubmissionId);
            } else {
                super.processDeleteEvent(event);
            }

            deleteFromPmtctFollowupTables(formSubmissionId);
            Timber.d("Ending processDeleteEvent: %s", event.getEventId());
        } catch (Exception e) {
            Timber.e(e);
        }
    }

    private void deleteFromPmtctFollowupTables(String formSubmissionId) {
        for (String tableName : PMTCT_FOLLOWUP_TABLES) {
            try {
                HfPmtctDao.deleteEntryFromTableByFormSubmissionId(tableName, formSubmissionId);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
    }
}
