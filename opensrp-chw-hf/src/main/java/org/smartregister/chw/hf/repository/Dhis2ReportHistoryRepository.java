package org.smartregister.chw.hf.repository;

import android.content.ContentValues;
import android.database.Cursor;

import net.sqlcipher.database.SQLiteDatabase;

import org.json.JSONObject;
import org.smartregister.chw.hf.domain.dhis2_reports.Dhis2ReportHistory;
import org.smartregister.chw.hf.utils.Constants;
import org.smartregister.domain.Event;
import org.smartregister.domain.Obs;
import org.smartregister.repository.BaseRepository;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

/**
 * Persists DHIS2 payload history derived from event obs REPORT_DATA (and optionally REPORT_DHIS_PAYLOAD)
 * to enable re-visualization after fresh installs and remote syncs.
 */
public class Dhis2ReportHistoryRepository extends BaseRepository {

    public static final String TABLE = "ec_dhis2_report_history";

    public static final String COL_EVENT_ID = "event_id";
    public static final String COL_BASE_ENTITY_ID = "base_entity_id";
    public static final String COL_EVENT_DATE = "event_date";
    public static final String COL_PROVIDER_ID = "provider_id";
    public static final String COL_LOCATION_ID = "location_id";
    public static final String COL_EVENT_TYPE = "event_type";

    public static final String COL_REPORT_TYPE = "report_type";
    public static final String COL_PERIOD = "period";
    public static final String COL_ORG_UNIT = "org_unit";
    public static final String COL_DATA_SET = "data_set";
    public static final String COL_COMPLETE_DATE = "complete_date";

    public static final String COL_REPORT_DATA_JSON = "report_data_json";
    public static final String COL_DHIS2_PAYLOAD_JSON = "dhis2_payload_json";

    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_LAST_INTERACTED_WITH = "last_interacted_with";

    private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE + " (" +
            COL_EVENT_ID + " TEXT PRIMARY KEY, " +
            COL_BASE_ENTITY_ID + " TEXT, " +
            COL_EVENT_DATE + " INTEGER, " +
            COL_PROVIDER_ID + " TEXT, " +
            COL_LOCATION_ID + " TEXT, " +
            COL_EVENT_TYPE + " TEXT, " +
            COL_REPORT_TYPE + " TEXT, " +
            COL_PERIOD + " TEXT, " +
            COL_ORG_UNIT + " TEXT, " +
            COL_DATA_SET + " TEXT, " +
            COL_COMPLETE_DATE + " TEXT, " +
            COL_REPORT_DATA_JSON + " TEXT, " +
            COL_DHIS2_PAYLOAD_JSON + " TEXT, " +
            COL_CREATED_AT + " INTEGER, " +
            COL_LAST_INTERACTED_WITH + " INTEGER" +
            ")";

    private void ensureTable() {
        try {
            getWritableDatabase().execSQL(CREATE_TABLE);
        } catch (Exception e) {
            Timber.e(e, "Error ensuring table %s", TABLE);
        }
    }

    public void saveFromEvent(Event event) {
        if (event == null) return;
        try {
            ensureTable();

            String eventId = event.getFormSubmissionId();
            if (eventId == null) return;

            String reportDataJson = null;
            String dhis2PayloadJson = null;
            String period = null;
            String reportType = null;

            if (event.getObs() != null) {
                for (Obs obs : event.getObs()) {
                    String field = obs.getFormSubmissionField();
                    Object val = obs.getValue();
                    String sVal = val != null ? String.valueOf(val) : null;
                    if (Constants.FormConstants.FormSubmissionFields.REPORT_DATA.equals(field)) {
                        reportDataJson = sVal;
                    } else if (Constants.FormConstants.FormSubmissionFields.REPORT_DHIS_PAYLOAD.equals(field)) {
                        dhis2PayloadJson = sVal;
                    } else if ("report_period".equals(field)) {
                        period = sVal;
                    } else if ("report_category".equals(field)) {
                        reportType = sVal;
                    }
                }
            }

            String orgUnit = null;
            String dataSet = null;
            String completeDate = null;
            if (dhis2PayloadJson != null) {
                try {
                    JSONObject obj = new JSONObject(dhis2PayloadJson);
                    if (obj.has("orgUnit")) orgUnit = obj.optString("orgUnit", null);
                    if (obj.has("dataSet")) dataSet = obj.optString("dataSet", null);
                    if (obj.has("completeDate")) completeDate = obj.optString("completeDate", null);
                    if (period == null && obj.has("period")) period = obj.optString("period", null);
                } catch (Exception ex) {
                    Timber.e(ex);
                }
            }

            if (reportType == null) {
                String et = event.getEventType();
                if (Constants.Events.SEND_MONTHLY_MTUHA_BOOK_3_TO_DHIS2.equals(et)) reportType = "hps_monthly";
                else if (Constants.Events.SEND_ANNUAL_REPORTS_TO_DHIS2.equals(et)) reportType = "hps_annual";
            }

            ContentValues cv = new ContentValues();
            cv.put(COL_EVENT_ID, eventId);
            cv.put(COL_BASE_ENTITY_ID, event.getBaseEntityId());
            cv.put(COL_EVENT_DATE, event.getEventDate() != null ? event.getEventDate().getMillis() : null);
            cv.put(COL_PROVIDER_ID, event.getProviderId());
            cv.put(COL_LOCATION_ID, event.getLocationId());
            cv.put(COL_EVENT_TYPE, event.getEventType());
            cv.put(COL_REPORT_TYPE, reportType);
            cv.put(COL_PERIOD, period);
            cv.put(COL_ORG_UNIT, orgUnit);
            cv.put(COL_DATA_SET, dataSet);
            cv.put(COL_COMPLETE_DATE, completeDate);
            cv.put(COL_REPORT_DATA_JSON, reportDataJson);
            cv.put(COL_DHIS2_PAYLOAD_JSON, dhis2PayloadJson);
            long now = System.currentTimeMillis();
            cv.put(COL_CREATED_AT, now);
            cv.put(COL_LAST_INTERACTED_WITH, now);

            getWritableDatabase().insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Timber.e(e, "Error saving DHIS2 history from event");
        }
    }

    public Dhis2ReportHistory getByEventId(String eventId) {
        ensureTable();
        Cursor c = null;
        try {
            c = getReadableDatabase().query(TABLE, null, COL_EVENT_ID + "=?", new String[]{eventId}, null, null, null);
            if (c != null && c.moveToFirst()) {
                return mapRow(c);
            }
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public List<Dhis2ReportHistory> list(String reportType, String period, int limit, int offset) {
        ensureTable();
        List<Dhis2ReportHistory> out = new ArrayList<>();
        Cursor c = null;
        try {
            String sel;
            List<String> args = new ArrayList<>();
            if (period != null) {
                sel = COL_REPORT_TYPE + "=? AND " + COL_PERIOD + "=?";
                args.add(reportType);
                args.add(period);
            } else {
                sel = COL_REPORT_TYPE + "=?";
                args.add(reportType);
            }
            String lim = offset + "," + limit;
            c = getReadableDatabase().query(TABLE, null, sel, args.toArray(new String[0]), null, null, COL_EVENT_DATE + " DESC", lim);
            while (c != null && c.moveToNext()) out.add(mapRow(c));
        } catch (Exception e) {
            Timber.e(e);
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private Dhis2ReportHistory mapRow(Cursor cursor) {
        Dhis2ReportHistory h = new Dhis2ReportHistory();
        h.eventId = cursor.getString(cursor.getColumnIndex(COL_EVENT_ID));
        h.baseEntityId = cursor.getString(cursor.getColumnIndex(COL_BASE_ENTITY_ID));
        if (!cursor.isNull(cursor.getColumnIndex(COL_EVENT_DATE))) h.eventDate = cursor.getLong(cursor.getColumnIndex(COL_EVENT_DATE));
        h.providerId = cursor.getString(cursor.getColumnIndex(COL_PROVIDER_ID));
        h.locationId = cursor.getString(cursor.getColumnIndex(COL_LOCATION_ID));
        h.eventType = cursor.getString(cursor.getColumnIndex(COL_EVENT_TYPE));
        h.reportType = cursor.getString(cursor.getColumnIndex(COL_REPORT_TYPE));
        h.period = cursor.getString(cursor.getColumnIndex(COL_PERIOD));
        h.orgUnit = cursor.getString(cursor.getColumnIndex(COL_ORG_UNIT));
        h.dataSet = cursor.getString(cursor.getColumnIndex(COL_DATA_SET));
        h.completeDate = cursor.getString(cursor.getColumnIndex(COL_COMPLETE_DATE));
        h.reportDataJson = cursor.getString(cursor.getColumnIndex(COL_REPORT_DATA_JSON));
        h.dhis2PayloadJson = cursor.getString(cursor.getColumnIndex(COL_DHIS2_PAYLOAD_JSON));
        if (!cursor.isNull(cursor.getColumnIndex(COL_CREATED_AT))) h.createdAt = cursor.getLong(cursor.getColumnIndex(COL_CREATED_AT));
        if (!cursor.isNull(cursor.getColumnIndex(COL_LAST_INTERACTED_WITH))) h.lastInteractedWith = cursor.getLong(cursor.getColumnIndex(COL_LAST_INTERACTED_WITH));
        return h;
    }
}

