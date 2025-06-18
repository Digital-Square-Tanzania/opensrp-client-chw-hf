package org.smartregister.chw.hf.domain.dhis2_reports;

import java.util.List;

public class Dhis2Report {
    private String dataSet;
    private String completeDate;
    private String period;
    private String orgUnit;
    private String attributeOptionCombo;
    private List<DhisDataValues> dataValues;

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getCompleteDate() {
        return completeDate;
    }

    public void setCompleteDate(String completeDate) {
        this.completeDate = completeDate;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getOrgUnit() {
        return orgUnit;
    }

    public void setOrgUnit(String orgUnit) {
        this.orgUnit = orgUnit;
    }

    public String getAttributeOptionCombo() {
        return attributeOptionCombo;
    }

    public void setAttributeOptionCombo(String attributeOptionCombo) {
        this.attributeOptionCombo = attributeOptionCombo;
    }

    public List<DhisDataValues> getDataValues() {
        return dataValues;
    }

    public void setDataValues(List<DhisDataValues> dataValues) {
        this.dataValues = dataValues;
    }
}
