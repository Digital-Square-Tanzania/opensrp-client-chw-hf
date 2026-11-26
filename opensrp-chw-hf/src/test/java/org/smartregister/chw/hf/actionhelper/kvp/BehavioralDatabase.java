package org.smartregister.chw.hf.actionhelper.kvp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;

import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;

import net.zetetic.database.DatabaseErrorHandler;
import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteDatabaseHook;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;
import org.smartregister.chw.kvp.domain.Visit;
import org.smartregister.chw.kvp.repository.VisitDetailsRepository;
import org.smartregister.chw.kvp.repository.VisitRepository;
import org.smartregister.clientandeventmodel.Event;
import org.smartregister.dao.AbstractDao;
import org.smartregister.repository.EventClientRepository;
import org.smartregister.repository.Repository;
import org.smartregister.sync.helper.ECSyncHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

final class BehavioralDatabase implements AutoCloseable {
    private final android.database.sqlite.SQLiteDatabase sqlite = android.database.sqlite.SQLiteDatabase.create(null);
    private final Repository originalRepository = ReflectionHelpers.getStaticField(AbstractDao.class, "repository");
    final SQLiteDatabase database;
    final VisitRepository visits;
    final VisitDetailsRepository details;
    final ECSyncHelper sync;

    BehavioralDatabase() {
        database = ReflectionHelpers.callConstructor(SQLiteDatabase.class, from(String.class, ":memory:"),
                from(byte[].class, new byte[0]), from(int.class, 0), from(SQLiteDatabase.CursorFactory.class, null),
                from(DatabaseErrorHandler.class, null), from(SQLiteDatabaseHook.class, null));
        ((CipherDatabase) Shadow.extract(database)).sqlite = sqlite;
        VisitRepository.createTable(database);
        database.execSQL(VisitRepository.ADD_VISIT_GROUP_COLUMN);
        VisitDetailsRepository.createTable(database);
        EventClientRepository.createTable(database, EventClientRepository.Table.event, EventClientRepository.event_column.values());
        EventClientRepository.createTable(database, EventClientRepository.Table.client, EventClientRepository.client_column.values());

        Repository repository = mock(Repository.class);
        when(repository.getReadableDatabase()).thenReturn(database);
        ReflectionHelpers.setStaticField(AbstractDao.class, "repository", repository);
        visits = new VisitRepository() {
            @Override
            public SQLiteDatabase getReadableDatabase() {
                return database;
            }

            @Override
            public SQLiteDatabase getWritableDatabase() {
                return database;
            }
        };
        details = new VisitDetailsRepository() {
            @Override
            public SQLiteDatabase getReadableDatabase() {
                return database;
            }

            @Override
            public SQLiteDatabase getWritableDatabase() {
                return database;
            }
        };
        EventClientRepository events = new EventClientRepository() {
            @Override
            public SQLiteDatabase getReadableDatabase() {
                return database;
            }

            @Override
            public SQLiteDatabase getWritableDatabase() {
                return database;
            }
        };
        sync = new ECSyncHelper(ApplicationProvider.getApplicationContext(), events) {};
    }

    void setAge(Visit visit, int days) {
        Calendar updated = Calendar.getInstance();
        updated.add(Calendar.DAY_OF_YEAR, -days);
        ContentValues values = new ContentValues();
        values.put("updated_at", updated.getTimeInMillis());
        sqlite.update(VisitRepository.VISIT_TABLE, values, "visit_id = ?", new String[]{visit.getVisitId()});
    }

    List<Event> queuedEvents(String type) {
        try (Cursor rows = sqlite.rawQuery("SELECT json FROM event WHERE eventType = ? ORDER BY rowid", new String[]{type})) {
            List<Event> events = new ArrayList<>();
            Gson gson = new Gson();
            while (rows.moveToNext()) {
                events.add(gson.fromJson(rows.getString(0), Event.class));
            }
            return events;
        }
    }

    String queueStatus(Visit visit) {
        return DatabaseUtils.stringForQuery(sqlite, "SELECT syncStatus FROM event WHERE formSubmissionId = ?",
                new String[]{visit.getFormSubmissionId()});
    }

    @Override
    public void close() {
        ReflectionHelpers.setStaticField(AbstractDao.class, "repository", originalRepository);
        sqlite.close();
    }

    // Use the production repositories and SQL with Robolectric's in-memory SQLite transport.
    @Implements(value = SQLiteDatabase.class, isInAndroidSdk = false)
    public static class CipherDatabase {
        private android.database.sqlite.SQLiteDatabase sqlite;

        @Implementation
        protected void execSQL(String sql) {
            sqlite.execSQL(sql);
        }

        @Implementation
        protected Cursor rawQuery(String sql, String[] args) {
            return sqlite.rawQuery(sql, args);
        }

        @Implementation
        protected Cursor query(String table, String[] columns, String where, String[] args,
                               String group, String having, String order, String limit) {
            return sqlite.query(table, columns, where, args, group, having, order, limit);
        }

        @Implementation
        protected Cursor query(String table, String[] columns, String where, String[] args,
                               String group, String having, String order) {
            return query(table, columns, where, args, group, having, order, null);
        }

        @Implementation
        protected long insert(String table, String nullColumn, ContentValues values) {
            return sqlite.insertOrThrow(table, nullColumn, values);
        }

        @Implementation
        protected int update(String table, ContentValues values, String where, String[] args) {
            return sqlite.update(table, values, where, args);
        }

        @Implementation
        protected int delete(String table, String where, String[] args) {
            return sqlite.delete(table, where, args);
        }

        @Implementation
        protected void beginTransaction() {
            sqlite.beginTransaction();
        }

        @Implementation
        protected void setTransactionSuccessful() {
            sqlite.setTransactionSuccessful();
        }

        @Implementation
        protected void endTransaction() {
            sqlite.endTransaction();
        }
    }
}
