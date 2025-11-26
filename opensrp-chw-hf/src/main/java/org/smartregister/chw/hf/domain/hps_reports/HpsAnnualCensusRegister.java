package org.smartregister.chw.hf.domain.hps_reports;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for rows in ec_hps_annual_census_register.
 *
 * This keeps core identifying fields explicitly, and allows the
 * large set of remaining indicators to be supplied via typed maps.
 */
public class HpsAnnualCensusRegister {

    private String baseEntityId;
    private String providerId;
    private Integer year;
    private String selectAgeGroup;
    private Long lastInteractedWith; // epoch millis

    // Remaining columns by type
    private final Map<String, Integer> integerIndicators = new HashMap<>();
    private final Map<String, String> stringIndicators = new HashMap<>();
    private final Map<String, Double> realIndicators = new HashMap<>();

    public String getBaseEntityId() {
        return baseEntityId;
    }

    public void setBaseEntityId(String baseEntityId) {
        this.baseEntityId = baseEntityId;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getSelectAgeGroup() {
        return selectAgeGroup;
    }

    public void setSelectAgeGroup(String selectAgeGroup) {
        this.selectAgeGroup = selectAgeGroup;
    }

    public Long getLastInteractedWith() {
        return lastInteractedWith;
    }

    public void setLastInteractedWith(Long lastInteractedWith) {
        this.lastInteractedWith = lastInteractedWith;
    }

    public Map<String, Integer> getIntegerIndicators() {
        return integerIndicators;
    }

    public Map<String, String> getStringIndicators() {
        return stringIndicators;
    }

    public Map<String, Double> getRealIndicators() {
        return realIndicators;
    }

    // Convenience builder-style helpers
    public HpsAnnualCensusRegister putInteger(String column, Integer value) {
        if (value != null) integerIndicators.put(column, value);
        return this;
    }

    public HpsAnnualCensusRegister putString(String column, String value) {
        if (value != null) stringIndicators.put(column, value);
        return this;
    }

    public HpsAnnualCensusRegister putReal(String column, Double value) {
        if (value != null) realIndicators.put(column, value);
        return this;
    }
}

