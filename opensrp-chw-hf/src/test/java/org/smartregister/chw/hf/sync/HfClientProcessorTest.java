package org.smartregister.chw.hf.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.smartregister.chw.hf.domain.hps_reports.HpsAnnualCensusRegister;
import org.smartregister.domain.Obs;

import java.util.Arrays;

public class HfClientProcessorTest {

    @Test
    public void findMatchingHtsEventTypeShouldReturnMatchingType() {
        String eventType = "prefix_"
                + org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_FIRST_HIV_TEST
                + "_suffix";

        String matchedType = HfClientProcessor.findMatchingHtsEventType(eventType);

        assertEquals(org.smartregister.chw.hts.util.Constants.EVENT_TYPE.HTS_FIRST_HIV_TEST, matchedType);
    }

    @Test
    public void findMatchingHtsEventTypeShouldReturnNullWhenNoMatchExists() {
        assertNull(HfClientProcessor.findMatchingHtsEventType("random-event-type"));
    }

    @Test
    public void applyAnnualCensusObservationShouldSetYearWhenYearFieldIsValid() {
        HpsAnnualCensusRegister annualCensusRegister = new HpsAnnualCensusRegister();
        Obs obs = mock(Obs.class);

        when(obs.getFormSubmissionField()).thenReturn("year");
        when(obs.getValue()).thenReturn("2025");

        HfClientProcessor.applyAnnualCensusObservation(annualCensusRegister, obs);

        assertEquals(Integer.valueOf(2025), annualCensusRegister.getYear());
    }

    @Test
    public void applyAnnualCensusObservationShouldUseValuesListForSelectAgeGroup() {
        HpsAnnualCensusRegister annualCensusRegister = new HpsAnnualCensusRegister();
        Obs obs = mock(Obs.class);

        when(obs.getFormSubmissionField()).thenReturn("select_age_group");
        when(obs.getValue()).thenReturn("fallback");
        when(obs.getValues()).thenReturn(Arrays.asList("15_19", "20_24"));

        HfClientProcessor.applyAnnualCensusObservation(annualCensusRegister, obs);

        assertEquals("[15_19, 20_24]", annualCensusRegister.getSelectAgeGroup());
        assertEquals("[15_19, 20_24]", annualCensusRegister.getStringIndicators().get("select_age_group"));
    }

    @Test
    public void applyAnnualCensusObservationShouldStoreConfiguredRealFieldAsDouble() {
        HpsAnnualCensusRegister annualCensusRegister = new HpsAnnualCensusRegister();
        Obs obs = mock(Obs.class);
        String realField = "amount_of_solid_waste_generated_annually_tons";

        when(obs.getFormSubmissionField()).thenReturn(realField);
        when(obs.getValue()).thenReturn("12.75");

        HfClientProcessor.applyAnnualCensusObservation(annualCensusRegister, obs);

        assertEquals(Double.valueOf(12.75), annualCensusRegister.getRealIndicators().get(realField));
    }

    @Test
    public void applyAnnualCensusObservationShouldFallbackToStringForNonIntegerValues() {
        HpsAnnualCensusRegister annualCensusRegister = new HpsAnnualCensusRegister();
        Obs obs = mock(Obs.class);
        String field = "custom_numeric_field";

        when(obs.getFormSubmissionField()).thenReturn(field);
        when(obs.getValue()).thenReturn("not-a-number");

        HfClientProcessor.applyAnnualCensusObservation(annualCensusRegister, obs);

        assertTrue(annualCensusRegister.getIntegerIndicators().isEmpty());
        assertEquals("not-a-number", annualCensusRegister.getStringIndicators().get(field));
    }
}
