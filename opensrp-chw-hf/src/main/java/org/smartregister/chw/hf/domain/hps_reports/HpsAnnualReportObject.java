package org.smartregister.chw.hf.domain.hps_reports;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.smartregister.chw.hf.domain.ReportObject;
import org.smartregister.chw.hf.repository.HpsAnnualCensorReportsRepository;

import java.util.Date;

import timber.log.Timber;

public class HpsAnnualReportObject extends ReportObject {
    public static String[] selectorsForSimpleKeys = {
            "number_of_male_by_age_group_under1",
            "number_of_female_by_age_group_under1",
            "number_of_male_by_age_group_1_4",
            "number_of_female_by_age_group_1_4",
            "number_of_male_by_age_group_5_14",
            "number_of_female_by_age_group_5_14",
            "number_of_male_by_age_group_15_49",
            "number_of_female_by_age_group_15_49",
            "number_of_male_by_age_group_50_59",
            "number_of_female_by_age_group_50_59",
            "number_of_male_by_age_group_60_plus",
            "number_of_female_by_age_group_60_plus",
            "number_of_house_hold",
            "number_of_house_hold_with_road_access",
            "number_of_house_holds_with_at_least_one_landline_or_mobile_phone",
            "number_of_house_hold_with_basic_nutrition_source_vegetable",
            "number_of_house_hold_with_basic_nutrition_source_fruit_trees",
            "number_of_house_hold_with_basic_nutrition_source_domestic_animal",

//          ******************************** BOX HERE (Down) ****************************     //

            "number_of_households_most_commonly_use_tap_as_sources_of_water",
            "number_of_households_most_commonly_use_river_as_sources_of_water",
            "number_of_households_most_commonly_use_shallow_well_as_sources_of_water",
            "number_of_households_most_commonly_use_water_pond_as_sources_of_water",
            "number_of_households_most_commonly_use_small_dam_as_sources_of_water",
            "number_of_households_most_commonly_use_lake_as_sources_of_water",
            "number_of_households_most_commonly_use_spring_as_sources_of_water",
            "number_of_households_using_electricity_as_source_of_energy_for_lighting",
            "number_of_households_using_solar_as_source_of_energy_for_lighting",
            "number_of_households_using_kerosine_as_source_of_energy_for_lighting",
            "number_of_households_using_koroboi_as_source_of_energy_for_lighting",
            "number_of_households_using_other_source_of_energy_for_lighting",
            "number_of_households_using_electricity_as_source_of_cooking_energy",
            "number_of_households_using_solar_as_source_of_cooking_energy",
            "number_of_households_using_kerosine_as_source_of_cooking_energy",
            "number_of_households_using_gas_as_source_of_cooking_energy",
            "number_of_households_using_charcoal_as_source_of_cooking_energy",
            "number_of_households_using_firewood_as_source_of_cooking_energy",
            "number_of_male_capable_of_engaging_in_economic_activities",
            "number_of_male_engaged_in_economic_activities",
            "number_of_female_capable_of_engaging_in_economic_activities",
            "number_of_female_engaged_in_economic_activities",
            "number_of_health_committee_members_for_effective_committee_meetings",
            "number_of_committee_members_attended_fisrt_quarter",
            "number_of_registered_alternative_medicine_service_providers",
            "number_of_registered_traditional_medicine_service_providers",
            "number_of_unregistered_alternative_medicine_service_providers",
            "number_of_unregistered_traditional_medicine_service_providers"
    };
    public static String[] selectorsForComplexKeys = {
            "number_of_pre_schools_government",
            "number_of_pre_schools_faith_based_organisation",
            "number_of_pre_schools_public",
            "number_of_pre_schools_private",
            "number_of_primary_schools_government",
            "number_of_primary_schools_faith_based_organisation",
            "number_of_primary_schools_public",
            "number_of_primary_schools_private",
            "number_of_secondary_schools_government",
            "number_of_secondary_schools_faith_based_organisation",
            "number_of_secondary_schools_public",
            "number_of_secondary_schools_private",
            "number_of_universities_government",
            "number_of_universities_faith_based_organisation",
            "number_of_universities_public",
            "number_of_universities_private",
            "number_of_special_needs_schools_government",
            "number_of_special_needs_schools_faith_based_organisation",
            "number_of_special_needs_schools_public",
            "number_of_special_needs_schools_private",
            "number_of_dispensary_government",
            "number_of_dispensary_faith_based_organisation",
            "number_of_dispensary_public",
            "number_of_dispensary_private",
            "number_of_health_centers_government",
            "number_of_health_centers_faith_based_organisation",
            "number_of_health_centers_public",
            "number_of_health_centers_private",
            "number_of_hospital_government",
            "number_of_hospital_faith_based_organisation",
            "number_of_hospital_public",
            "number_of_hospital_private",
            "number_of_special_clinics_government",
            "number_of_special_clinics_faith_based_organisation",
            "number_of_special_clinics_public",
            "number_of_special_clinics_private",
            "number_of_laboratory_government",
            "number_of_laboratory_faith_based_organisation",
            "number_of_laboratory_public",
            "number_of_laboratory_private",
            "number_of_pharmacy_government",
            "number_of_pharmacy_faith_based_organisation",
            "number_of_pharmacy_public",
            "number_of_pharmacy_private",
            "number_of_addo_government",
            "number_of_addo_faith_based_organisation",
            "number_of_addo_public",
            "number_of_addo_private",
            "number_of_maternity_home_government",
            "number_of_maternity_home_faith_based_organisation",
            "number_of_maternity_home_public",
            "number_of_maternity_home_private",
            "number_of_orphan_care_centers_government",
            "number_of_orphan_care_centers_faith_based_organisation",
            "number_of_orphan_care_centers_public",
            "number_of_orphan_care_centers_private",
            "number_of_centers_for_children_with_disabilities_government",
            "number_of_centers_for_children_with_disabilities_faith_based_organisation",
            "number_of_centers_for_children_with_disabilities_public",
            "number_of_centers_for_children_with_disabilities_private",
            "number_of_cbecdc_rehabilitation_centre_government",
            "number_of_cbecdc_rehabilitation_centre_faith_based_organisation",
            "number_of_cbecdc_rehabilitation_centre_public",
            "number_of_cbecdc_rehabilitation_centre_private",
            "number_of_day_care_centers_government",
            "number_of_day_care_centers_faith_based_organisation",
            "number_of_day_care_centers_public",
            "number_of_day_care_centers_private",
            "number_of_elderly_care_centers_government",
            "number_of_elderly_care_centers_faith_based_organisation",
            "number_of_elderly_care_centers_public",
            "number_of_elderly_care_centers_private",
    };
    private final String[] simpleKeys = new String[]{
            "hps-M-<1", "hps-F-<1", "hps-M-1-4", "hps-F-1-4", "hps-M-5-14", "hps-F-5-14", "hps-M-15-49", "hps-F-15-49", "hps-M-50-59", "hps-F-50-59", "hps-M-60-+", "hps-F-60-+", "hps-total-households",
            "hps-a-1", "hps-a-2", "hps-b-1", "hps-b-2", "hps-b-3", "hps-u-1", "hps-u-2", "hps-u-3", "hps-u-4", "hps-u-5", "hps-u-6", "hps-u-7", "hps-u-8", "hps-u-9", "hps-u-10", "hps-u-11",
            "hps-u-12", "hps-u-13", "hps-u-14", "hps-u-15", "hps-u-16", "hps-u-17", "hps-u-18", "hps-v-1", "hps-vv-1", "hps-v-2", "hps-vv-2", "hps-x-1", "hps-x-2", "hps-y-1", "hps-y-2", "hps-z-1", "hps-z-2",
    };
    private final String[] complexKeys = new String[]{
            "hps-c", "hps-d", "hps-e", "hps-f", "hps-g", "hps-h", "hps-i", "hps-j", "hps-k", "hps-l", "hps-m", "hps-n", "hps-o", "hps-p", "hps-q", "hps-r", "hps-s", "hps-t",
    };
    private final String[] hpsQuestionsGroups = new String[]{"1", "2", "3", "4"};

    private final Date reportDate;
    JSONObject jsonObject = new JSONObject();
    JSONArray dataArray = new JSONArray();


    public HpsAnnualReportObject(Date reportDate) {
        super(reportDate);
        this.reportDate = reportDate;

    }

    @Override
    public JSONObject getIndicatorData() throws JSONException {
        jsonObject = new JSONObject();
        try {
            // 1) Derive reporting year from provided date
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(reportDate);
            int year = cal.get(java.util.Calendar.YEAR);

            HpsAnnualCensorReportsRepository repo = new HpsAnnualCensorReportsRepository();

            // 2) Aggregate required columns once per section
            java.util.Map<String, Integer> simpleAgg = repo.getAggregatesForYear(year, selectorsForSimpleKeys);
            java.util.Map<String, Integer> complexAgg = repo.getAggregatesForYear(year, selectorsForComplexKeys);

            // 3) Map simple selectors to their corresponding HTML ids
            for (int i = 0; i < simpleKeys.length && i < selectorsForSimpleKeys.length; i++) {
                String htmlId = simpleKeys[i];
                String column = selectorsForSimpleKeys[i];
                int value = simpleAgg.containsKey(column) ? simpleAgg.get(column) : 0;
                jsonObject.put(htmlId, value);
            }

            // 4) Complex table: map each category (1..4) per complex group (c..t)
            int groups = hpsQuestionsGroups.length; // 4
            for (int i = 0; i < complexKeys.length; i++) {
                String base = complexKeys[i];
                for (int j = 0; j < groups; j++) {
                    int idx = (i * groups) + j;
                    if (idx >= selectorsForComplexKeys.length) break;
                    String column = selectorsForComplexKeys[idx];
                    int value = complexAgg.containsKey(column) ? complexAgg.get(column) : 0;
                    jsonObject.put(base + "-" + hpsQuestionsGroups[j], value);
                }
            }

            // 5) Compute totals for age/gender and MF grand total
            int maleTotal = 0;
            int femaleTotal = 0;
            // male age groups
            maleTotal += simpleAgg.getOrDefault("number_of_male_by_age_group_under1", 0);
            maleTotal += simpleAgg.getOrDefault("number_of_male_by_age_group_1_4", 0);
            maleTotal += simpleAgg.getOrDefault("number_of_male_by_age_group_5_14", 0);
            maleTotal += simpleAgg.getOrDefault("number_of_male_by_age_group_15_49", 0);
            maleTotal += simpleAgg.getOrDefault("number_of_male_by_age_group_50_59", 0);
            maleTotal += simpleAgg.getOrDefault("number_of_male_by_age_group_60_plus", 0);
            // female age groups
            femaleTotal += simpleAgg.getOrDefault("number_of_female_by_age_group_under1", 0);
            femaleTotal += simpleAgg.getOrDefault("number_of_female_by_age_group_1_4", 0);
            femaleTotal += simpleAgg.getOrDefault("number_of_female_by_age_group_5_14", 0);
            femaleTotal += simpleAgg.getOrDefault("number_of_female_by_age_group_15_49", 0);
            femaleTotal += simpleAgg.getOrDefault("number_of_female_by_age_group_50_59", 0);
            femaleTotal += simpleAgg.getOrDefault("number_of_female_by_age_group_60_plus", 0);
            jsonObject.put("hps-M-total", maleTotal);
            jsonObject.put("hps-F-total", femaleTotal);
            jsonObject.put("hps-MF-grand-total", maleTotal + femaleTotal);

            // 6) Totals per complex group (c..t): sum four categories
            for (int i = 0; i < complexKeys.length; i++) {
                String base = complexKeys[i];
                int total = 0;
                for (int j = 0; j < groups; j++) {
                    Object v = jsonObject.opt(base + "-" + hpsQuestionsGroups[j]);
                    if (v instanceof Number) total += ((Number) v).intValue();
                    else {
                        try {
                            total += Integer.parseInt(String.valueOf(v));
                        } catch (Exception ignore) {
                        }
                    }
                }
                jsonObject.put(base + "-total", total);
            }

            // 7) Totals for economic activities (v/vv)
            int vTotal = simpleAgg.getOrDefault("number_of_male_capable_of_engaging_in_economic_activities", 0)
                    + simpleAgg.getOrDefault("number_of_female_capable_of_engaging_in_economic_activities", 0);
            int vvTotal = simpleAgg.getOrDefault("number_of_male_engaged_in_economic_activities", 0)
                    + simpleAgg.getOrDefault("number_of_female_engaged_in_economic_activities", 0);
            jsonObject.put("hps-v-total", vTotal);
            jsonObject.put("hps-vv-total", vvTotal);

            // Ensure reportData key exists to appease the viewer script
            jsonObject.put("reportData", dataArray);
        } catch (Exception e) {
            Timber.e(e, "Error computing HPS annual report indicators");
        }
        return jsonObject;
    }
}
