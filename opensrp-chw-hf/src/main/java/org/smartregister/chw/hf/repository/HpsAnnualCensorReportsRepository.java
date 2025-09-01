package org.smartregister.chw.hf.repository;

import android.content.ContentValues;
import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;

import org.smartregister.chw.hf.domain.hps_reports.HpsAnnualCensusRegister;
import org.smartregister.repository.BaseRepository;

import java.util.List;

import timber.log.Timber;

/**
 * Repository for the ec_hps_annual_census_register table.
 */
public class HpsAnnualCensorReportsRepository extends BaseRepository {

    public static final String TABLE_NAME = "ec_hps_annual_census_register";
    public static final String COL_BASE_ENTITY_ID = "base_entity_id";
    public static final String COL_PROVIDER_ID = "provider_id";
    public static final String COL_YEAR = "year";
    public static final String COL_SELECT_AGE_GROUP = "select_age_group";
    public static final String COL_LAST_INTERACTED_WITH = "last_interacted_with";

    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
            "base_entity_id TEXT," +
            "provider_id TEXT," +
            "year INTEGER," +
            "select_age_group TEXT," +
            "number_of_male_by_age_group_under1 INTEGER," +
            "number_of_female_by_age_group_under1 INTEGER," +
            "number_of_male_by_age_group_1_4 INTEGER," +
            "number_of_female_by_age_group_1_4 INTEGER," +
            "number_of_male_by_age_group_5_14 INTEGER," +
            "number_of_female_by_age_group_5_14 INTEGER," +
            "number_of_male_by_age_group_15_49 INTEGER," +
            "number_of_female_by_age_group_15_49 INTEGER," +
            "number_of_male_by_age_group_50_59 INTEGER," +
            "number_of_female_by_age_group_50_59 INTEGER," +
            "number_of_male_by_age_group_60_plus INTEGER," +
            "number_of_female_by_age_group_60_plus INTEGER," +
            "number_of_house_hold_with_road_access INTEGER," +
            "number_of_house_holds_with_at_least_one_landline_or_mobile_phone INTEGER," +
            "number_of_house_hold_with_basic_nutrition_source_vegetable INTEGER," +
            "number_of_house_hold_with_basic_nutrition_source_fruit_trees INTEGER," +
            "number_of_house_hold_with_basic_nutrition_source_domestic_animal INTEGER," +
            "select_centers_category TEXT," +
            "number_of_pre_schools_government INTEGER," +
            "number_of_primary_schools_government INTEGER," +
            "number_of_secondary_schools_government INTEGER," +
            "number_of_universities_government INTEGER," +
            "number_of_special_needs_schools_government INTEGER," +
            "number_of_dispensary_government INTEGER," +
            "number_of_health_centers_government INTEGER," +
            "number_of_hospital_government INTEGER," +
            "number_of_special_clinics_government INTEGER," +
            "number_of_laboratory_government INTEGER," +
            "number_of_pharmacy_government INTEGER," +
            "number_of_addo_government INTEGER," +
            "number_of_maternity_home_government INTEGER," +
            "number_of_orphan_care_centers_government INTEGER," +
            "number_of_centers_for_children_with_disabilities_government INTEGER," +
            "number_of_cbecdc_rehabilitation_centre_government INTEGER," +
            "number_of_day_care_centers_government INTEGER," +
            "number_of_elderly_care_centers_government INTEGER," +
            "number_of_pre_schools_faith_based_organisation INTEGER," +
            "number_of_primary_schools_faith_based_organisation INTEGER," +
            "number_of_secondary_schools_faith_based_organisation INTEGER," +
            "number_of_universities_faith_based_organisation INTEGER," +
            "number_of_special_needs_schools_faith_based_organisation INTEGER," +
            "number_of_dispensary_faith_based_organisation INTEGER," +
            "number_of_health_centers_faith_based_organisation INTEGER," +
            "number_of_hospital_faith_based_organisation INTEGER," +
            "number_of_special_clinics_faith_based_organisation INTEGER," +
            "number_of_laboratory_faith_based_organisation INTEGER," +
            "number_of_pharmacy_faith_based_organisation INTEGER," +
            "number_of_addo_faith_based_organisation INTEGER," +
            "number_of_maternity_home_faith_based_organisation INTEGER," +
            "number_of_orphan_care_centers_faith_based_organisation INTEGER," +
            "number_of_centers_for_children_with_disabilities_faith_based_organisation INTEGER," +
            "number_of_cbecdc_rehabilitation_centre_faith_based_organisation INTEGER," +
            "number_of_day_care_centers_faith_based_organisation INTEGER," +
            "number_of_elderly_care_centers_faith_based_organisation INTEGER," +
            "number_of_pre_schools_public INTEGER," +
            "number_of_primary_schools_public INTEGER," +
            "number_of_secondary_schools_public INTEGER," +
            "number_of_universities_public INTEGER," +
            "number_of_special_needs_schools_public INTEGER," +
            "number_of_dispensary_public INTEGER," +
            "number_of_health_centers_public INTEGER," +
            "number_of_hospital_public INTEGER," +
            "number_of_special_clinics_public INTEGER," +
            "number_of_laboratory_public INTEGER," +
            "number_of_pharmacy_public INTEGER," +
            "number_of_addo_public INTEGER," +
            "number_of_maternity_home_public INTEGER," +
            "number_of_orphan_care_centers_public INTEGER," +
            "number_of_centers_for_children_with_disabilities_public INTEGER," +
            "number_of_cbecdc_rehabilitation_centre_public INTEGER," +
            "number_of_day_care_centers_public INTEGER," +
            "number_of_elderly_care_centers_public INTEGER," +
            "number_of_pre_schools_private INTEGER," +
            "number_of_primary_schools_private INTEGER," +
            "number_of_secondary_schools_private INTEGER," +
            "number_of_universities_private INTEGER," +
            "number_of_special_needs_schools_private INTEGER," +
            "number_of_dispensary_private INTEGER," +
            "number_of_health_centers_private INTEGER," +
            "number_of_hospital_private INTEGER," +
            "number_of_special_clinics_private INTEGER," +
            "number_of_laboratory_private INTEGER," +
            "number_of_pharmacy_private INTEGER," +
            "number_of_addo_private INTEGER," +
            "number_of_maternity_home_private INTEGER," +
            "number_of_orphan_care_centers_private INTEGER," +
            "number_of_centers_for_children_with_disabilities_private INTEGER," +
            "number_of_cbecdc_rehabilitation_centre_private INTEGER," +
            "number_of_day_care_centers_private INTEGER," +
            "number_of_elderly_care_centers_private INTEGER," +
            "number_of_food_shop_visited INTEGER," +
            "number_of_restaurants_visited INTEGER," +
            "number_of_butcheries_visited INTEGER," +
            "number_of_bar_and_clubs_visited INTEGER," +
            "number_of_guest_house_visited INTEGER," +
            "number_of_loca_food_vendors_visited INTEGER," +
            "number_of_markets_visited INTEGER," +
            "number_of_public_toilets_visited INTEGER," +
            "number_of_bus_stations_visited INTEGER," +
            "number_of_primary_schools_visited INTEGER," +
            "number_of_secondary_schools_visited INTEGER," +
            "number_of_hospital_visited INTEGER," +
            "number_of_health_centers_visited INTEGER," +
            "number_of_dispensaries_visited INTEGER," +
            "number_of_offices_visited INTEGER," +
            "number_of_universities_college_visited INTEGER," +
            "number_of_food_shop_that_met_the_standards INTEGER," +
            "number_of_restaurants_that_met_the_standards INTEGER," +
            "number_of_butcheries_that_met_the_standards INTEGER," +
            "number_of_bar_and_clubs_that_met_the_standards INTEGER," +
            "number_of_guest_house_that_met_the_standards INTEGER," +
            "number_of_loca_food_vendors_that_met_the_standards INTEGER," +
            "number_of_markets_that_met_the_standards INTEGER," +
            "number_of_public_toilets_that_met_the_standards INTEGER," +
            "number_of_bus_stations_that_met_the_standards INTEGER," +
            "number_of_primary_schools_that_met_the_standards INTEGER," +
            "number_of_secondary_schools_that_met_the_standards INTEGER," +
            "number_of_hospital_that_met_the_standards INTEGER," +
            "number_of_health_centers_that_met_the_standards INTEGER," +
            "number_of_dispensaries_that_met_the_standards INTEGER," +
            "number_of_offices_that_met_the_standards INTEGER," +
            "number_of_universities_college_that_met_the_standards INTEGER," +
            "number_of_inspected_agriculture_areas INTEGER," +
            "number_of_inspected_livestock_keeping_areas INTEGER," +
            "number_of_inspected_fishing_areas INTEGER," +
            "number_of_inspected_industries_areas INTEGER," +
            "number_of_inspected_offices_areas INTEGER," +
            "number_of_inspected_transportation_areas INTEGER," +
            "number_of_other_inspected_areas INTEGER," +
            "number_of_agriculture_areas_inspected_with_risk_indicators INTEGER," +
            "number_of_livestock_keeping_areas_inspected_with_risk_indicators INTEGER," +
            "number_of_fishing_areas_inspected_with_risk_indicators INTEGER," +
            "number_of_industries_areas_inspected_with_risk_indicators INTEGER," +
            "number_of_offices_areas_inspected_with_risk_indicators INTEGER," +
            "number_of_transportation_areas_inspected_with_risk_indicators INTEGER," +
            "number_of_other_areas_inspected_with_risk_indicators INTEGER," +
            "number_of_inspected_grains INTEGER," +
            "number_of_inspected_legumes INTEGER," +
            "number_of_inspected_meat INTEGER," +
            "number_of_inspected_fishing INTEGER," +
            "number_of_inspected_alcoholic_beverages INTEGER," +
            "number_of_inspected_non_alcoholic_beverages INTEGER," +
            "number_of_grains_discarded INTEGER," +
            "number_of_legumes_discarded INTEGER," +
            "number_of_meat_discarded INTEGER," +
            "number_of_fishing_discarded INTEGER," +
            "number_of_alcoholic_beverage_discarded INTEGER," +
            "number_of_non_alcoholic_beverage_discarded INTEGER," +
            "health_reports_affecting_people_in_workplaces_respiratory_diseases INTEGER," +
            "health_reports_affecting_people_in_workplaces_toxic_chemicals INTEGER," +
            "health_reports_affecting_people_in_workplaces_burns INTEGER," +
            "health_reports_affecting_people_in_workplaces_hearing_loss INTEGER," +
            "health_reports_affecting_people_in_workplaces_eye_problems INTEGER," +
            "health_reports_affecting_people_in_workplaces_other_effects INTEGER," +
            "amount_of_solid_waste_generated_annually_tons REAL," +
            "amount_of_solid_waste_disposed_at_a_designated_site_annually_tons REAL," +
            "number_of_waste_collection_equipment_vehicles INTEGER," +
            "number_of_waste_collection_equipment_tractors INTEGER," +
            "number_of_waste_collection_equipment_carts INTEGER," +
            "number_of_waste_collection_equipment_wheelbarrows INTEGER," +
            "number_of_waste_collection_equipment_others INTEGER," +
            "number_of_areas_sprayed_with_pesticides_ponds INTEGER," +
            "number_of_areas_sprayed_with_pesticides_cans INTEGER," +
            "number_of_areas_sprayed_with_pesticides_drums INTEGER," +
            "number_of_areas_sprayed_with_pesticides_barrels INTEGER," +
            "number_of_areas_sprayed_with_pesticides_coconut_shells INTEGER," +
            "number_of_times_spraying_was_done_ponds INTEGER," +
            "number_of_times_spraying_was_done_cans INTEGER," +
            "number_of_times_spraying_was_done_drums INTEGER," +
            "number_of_times_spraying_was_done_barrels INTEGER," +
            "number_of_times_spraying_was_done_coconut_shells INTEGER," +
            "types_of_pesticides_used_ponds TEXT," +
            "types_of_pesticides_used_cans TEXT," +
            "types_of_pesticides_used_drums TEXT," +
            "types_of_pesticides_used_barrels TEXT," +
            "types_of_pesticides_used_coconut_shells TEXT," +
            "amount_of_pesticide_used_ponds TEXT," +
            "amount_of_pesticide_used_cans TEXT," +
            "amount_of_pesticide_used_drums TEXT," +
            "amount_of_pesticide_used_barrels TEXT," +
            "amount_of_pesticide_used_coconut_shells TEXT," +
            "number_of_house_hold INTEGER," +
            "number_of_male_capable_of_engaging_in_economic_activities INTEGER," +
            "number_of_female_capable_of_engaging_in_economic_activities INTEGER," +
            "number_of_male_engaged_in_economic_activities INTEGER," +
            "number_of_female_engaged_in_economic_activities INTEGER," +
            "number_of_households_most_commonly_use_tap_as_sources_of_water INTEGER," +
            "number_of_households_most_commonly_use_river_as_sources_of_water INTEGER," +
            "number_of_households_most_commonly_use_shallow_well_as_sources_of_water INTEGER," +
            "number_of_households_most_commonly_use_water_pond_as_sources_of_water INTEGER," +
            "number_of_households_most_commonly_use_small_dam_as_sources_of_water INTEGER," +
            "number_of_households_most_commonly_use_lake_as_sources_of_water INTEGER," +
            "number_of_households_most_commonly_use_spring_as_sources_of_water INTEGER," +
            "number_of_households_using_electricity_as_source_of_energy_for_lighting INTEGER," +
            "number_of_households_using_solar_as_source_of_energy_for_lighting INTEGER," +
            "number_of_households_using_kerosine_as_source_of_energy_for_lighting INTEGER," +
            "number_of_households_using_koroboi_as_source_of_energy_for_lighting INTEGER," +
            "number_of_households_using_other_source_of_energy_for_lighting INTEGER," +
            "number_of_households_using_electricity_as_source_of_cooking_energy INTEGER," +
            "number_of_households_using_solar_as_source_of_cooking_energy INTEGER," +
            "number_of_households_using_kerosine_as_source_of_cooking_energy INTEGER," +
            "number_of_households_using_gas_as_source_of_cooking_energy INTEGER," +
            "number_of_households_using_charcoal_as_source_of_cooking_energy INTEGER," +
            "number_of_households_using_firewood_as_source_of_cooking_energy INTEGER," +
            "number_of_health_committee_members_for_effective_committee_meetings INTEGER," +
            "number_of_committee_members_attended_fisrt_quarter INTEGER," +
            "number_of_registered_alternative_medicine_service_providers INTEGER," +
            "number_of_registered_traditional_medicine_service_providers INTEGER," +
            "number_of_unregistered_alternative_medicine_service_providers INTEGER," +
            "number_of_unregistered_traditional_medicine_service_providers INTEGER," +
            // Household sanitation and related indicators (additional)
            "number_of_households_with_waste_disposal_pits INTEGER," +
            "number_of_households_with_economic_problems INTEGER," +
            "number_of_households_with_no_latrine INTEGER," +
            "number_of_households_with_good_latrine INTEGER," +
            "number_of_households_inspected INTEGER," +
            "number_of_households_near_clean_water_sources INTEGER," +
            "number_of_households_with_dish_racks_for_drying_utensils INTEGER," +
            "number_of_households_with_handwashing_facilities_after_using_the_toilet INTEGER," +
            "number_of_households_that_have_been_sprayed_with_insecticide INTEGER," +
            "number_of_households_with_social_problems INTEGER," +
            // Variants for age groups (alternate keys observed in events)
            "number_of_male_by_age_group_less_1 INTEGER," +
            "number_of_female_by_age_group_less_1 INTEGER," +
            "number_of_male_by_age_group_equal_and_above_60 INTEGER," +
            "number_of_female_by_age_group_equal_and_above_60 INTEGER," +
            // Spelling-variant keys observed in events
            "number_of_local_food_vendors_visited INTEGER," +
            "number_of_local_food_vendors_that_met_the_standards INTEGER," +
            // Committee attendance by quarter (expanded set)
            "number_of_committee_members_attended_first_quarter INTEGER," +
            "number_of_committee_members_attended_second_quarter INTEGER," +
            "number_of_committee_members_attended_third_quarter INTEGER," +
            "number_of_committee_members_attended_fourth_quarter INTEGER," +
            // Optional metadata timestamps from events
            "start TEXT," +
            "end TEXT," +
            "number_of_households_without_good_latrine INTEGER," +
            "last_interacted_with INTEGER," +
            "PRIMARY KEY (year, provider_id)" +
            ")";

    public static void createTable(SQLiteDatabase database) {
        database.execSQL(CREATE_TABLE_SQL);
    }

    public static void dropTable(SQLiteDatabase database) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    // Ensure table exists before any write
    private void ensureTableExists() {
        try {
            getWritableDatabase().execSQL(CREATE_TABLE_SQL);
            // Migrate: ensure new columns exist for older installs
            final String[][] newCols = new String[][]{
                    {"number_of_households_without_good_latrine", "INTEGER"},
                    {"number_of_households_with_waste_disposal_pits", "INTEGER"},
                    {"number_of_households_with_economic_problems", "INTEGER"},
                    {"number_of_households_with_no_latrine", "INTEGER"},
                    {"number_of_households_with_good_latrine", "INTEGER"},
                    {"number_of_households_inspected", "INTEGER"},
                    {"number_of_households_near_clean_water_sources", "INTEGER"},
                    {"number_of_households_with_dish_racks_for_drying_utensils", "INTEGER"},
                    {"number_of_households_with_handwashing_facilities_after_using_the_toilet", "INTEGER"},
                    {"number_of_households_that_have_been_sprayed_with_insecticide", "INTEGER"},
                    {"number_of_households_with_social_problems", "INTEGER"},
                    {"number_of_male_by_age_group_less_1", "INTEGER"},
                    {"number_of_female_by_age_group_less_1", "INTEGER"},
                    {"number_of_male_by_age_group_equal_and_above_60", "INTEGER"},
                    {"number_of_female_by_age_group_equal_and_above_60", "INTEGER"},
                    {"number_of_local_food_vendors_visited", "INTEGER"},
                    {"number_of_local_food_vendors_that_met_the_standards", "INTEGER"},
                    {"number_of_committee_members_attended_first_quarter", "INTEGER"},
                    {"number_of_committee_members_attended_second_quarter", "INTEGER"},
                    {"number_of_committee_members_attended_third_quarter", "INTEGER"},
                    {"number_of_committee_members_attended_fourth_quarter", "INTEGER"},
                    {"start", "TEXT"},
                    {"end", "TEXT"}
            };
            for (String[] c : newCols) {
                ensureColumnExists(c[0], c[1]);
            }
        } catch (Exception e) {
            Timber.e(e, "Error ensuring %s table exists", TABLE_NAME);
        }
    }

    private void ensureColumnExists(String column, String type) {
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getWritableDatabase();
            boolean found = false;
            cursor = db.rawQuery("PRAGMA table_info(" + TABLE_NAME + ")", null);
            int nameIdx = cursor.getColumnIndex("name");
            while (cursor.moveToNext()) {
                if (column.equalsIgnoreCase(cursor.getString(nameIdx))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                db.execSQL("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + column + " " + type);
            }
        } catch (Exception e) {
            Timber.e(e, "Error ensuring column %s exists on %s", column, TABLE_NAME);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    /**
     * Insert or replace a single annual census register row.
     * Uses (year, provider_id) as the primary key conflict target.
     */
    public long save(HpsAnnualCensusRegister record) {
        try {
            ensureTableExists();
            ContentValues values = toContentValues(record);
            // Upsert on the composite primary key (year, provider_id)
            return getWritableDatabase().insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Timber.e(e, "Error saving HpsAnnualCensusRegister");
            return -1;
        }
    }

    /**
     * Bulk insert/replace in a single transaction for performance.
     */
    public void saveAll(List<HpsAnnualCensusRegister> records) {
        if (records == null || records.isEmpty()) return;
        ensureTableExists();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (HpsAnnualCensusRegister r : records) {
                ContentValues values = toContentValues(r);
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Timber.e(e, "Error bulk saving HpsAnnualCensusRegister");
        } finally {
            db.endTransaction();
        }
    }

    private ContentValues toContentValues(HpsAnnualCensusRegister r) {
        ContentValues cv = new ContentValues();
        // Core columns
        if (r.getBaseEntityId() != null) cv.put(COL_BASE_ENTITY_ID, r.getBaseEntityId());
        if (r.getProviderId() != null) cv.put(COL_PROVIDER_ID, r.getProviderId());
        if (r.getYear() != null) cv.put(COL_YEAR, r.getYear());
        if (r.getSelectAgeGroup() != null) cv.put(COL_SELECT_AGE_GROUP, r.getSelectAgeGroup());
        if (r.getLastInteractedWith() != null) cv.put(COL_LAST_INTERACTED_WITH, r.getLastInteractedWith());

        // The table contains a very large number of integer, text, and real columns.
        // They can be supplied via the typed indicator maps on the DTO.
        for (java.util.Map.Entry<String, Integer> e : r.getIntegerIndicators().entrySet()) {
            if (e.getValue() != null) cv.put(e.getKey(), e.getValue());
        }
        for (java.util.Map.Entry<String, String> e : r.getStringIndicators().entrySet()) {
            if (e.getValue() != null) cv.put(e.getKey(), e.getValue());
        }
        for (java.util.Map.Entry<String, Double> e : r.getRealIndicators().entrySet()) {
            if (e.getValue() != null) cv.put(e.getKey(), e.getValue());
        }
        return cv;
    }
}
