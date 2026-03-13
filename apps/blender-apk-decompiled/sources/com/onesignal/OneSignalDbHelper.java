package com.onesignal;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import com.onesignal.OneSignal;
import com.onesignal.OneSignalDbContract;
import java.util.ArrayList;

/* JADX INFO: loaded from: classes.dex */
public class OneSignalDbHelper extends SQLiteOpenHelper {
    private static final String COMMA_SEP = ",";
    private static final String DATABASE_NAME = "OneSignal.db";
    static final int DATABASE_VERSION = 3;
    private static final int DB_OPEN_RETRY_BACKOFF = 400;
    private static final int DB_OPEN_RETRY_MAX = 5;
    private static final String INT_TYPE = " INTEGER";
    private static final String SQL_CREATE_ENTRIES = "CREATE TABLE notification (_id INTEGER PRIMARY KEY,notification_id TEXT,android_notification_id INTEGER,group_id TEXT,collapse_id TEXT,is_summary INTEGER DEFAULT 0,opened INTEGER DEFAULT 0,dismissed INTEGER DEFAULT 0,title TEXT,message TEXT,full_data TEXT,created_time TIMESTAMP DEFAULT (strftime('%s', 'now')),expire_time TIMESTAMP);";
    private static final String[] SQL_INDEX_ENTRIES = {OneSignalDbContract.NotificationTable.INDEX_CREATE_NOTIFICATION_ID, OneSignalDbContract.NotificationTable.INDEX_CREATE_ANDROID_NOTIFICATION_ID, OneSignalDbContract.NotificationTable.INDEX_CREATE_GROUP_ID, OneSignalDbContract.NotificationTable.INDEX_CREATE_COLLAPSE_ID, OneSignalDbContract.NotificationTable.INDEX_CREATE_CREATED_TIME, OneSignalDbContract.NotificationTable.INDEX_CREATE_EXPIRE_TIME};
    private static final String TEXT_TYPE = " TEXT";
    private static OneSignalDbHelper sInstance;

    private static int getDbVersion() {
        return 3;
    }

    OneSignalDbHelper(Context context) {
        super(context, DATABASE_NAME, (SQLiteDatabase.CursorFactory) null, getDbVersion());
    }

    public static synchronized OneSignalDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new OneSignalDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    synchronized SQLiteDatabase getWritableDbWithRetries() {
        int i;
        int i2 = 0;
        while (true) {
            try {
            } finally {
                if (i2 < i) {
                }
            }
        }
        return getWritableDatabase();
    }

    synchronized SQLiteDatabase getReadableDbWithRetries() {
        int i;
        int i2 = 0;
        while (true) {
            try {
            } finally {
                if (i2 < i) {
                }
            }
        }
        return getReadableDatabase();
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onCreate(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
        for (String str : SQL_INDEX_ENTRIES) {
            sQLiteDatabase.execSQL(str);
        }
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        try {
            internalOnUpgrade(sQLiteDatabase, i);
        } catch (SQLiteException e) {
            OneSignal.Log(OneSignal.LOG_LEVEL.ERROR, "Error in upgrade, migration may have already run! Skipping!", e);
        }
    }

    private static void internalOnUpgrade(SQLiteDatabase sQLiteDatabase, int i) {
        if (i < 2) {
            upgradeFromV1ToV2(sQLiteDatabase);
        }
        if (i < 3) {
            upgradeFromV2ToV3(sQLiteDatabase);
        }
    }

    private static void upgradeFromV1ToV2(SQLiteDatabase sQLiteDatabase) {
        safeExecSQL(sQLiteDatabase, "ALTER TABLE notification ADD COLUMN collapse_id TEXT;");
        safeExecSQL(sQLiteDatabase, OneSignalDbContract.NotificationTable.INDEX_CREATE_GROUP_ID);
    }

    private static void upgradeFromV2ToV3(SQLiteDatabase sQLiteDatabase) {
        safeExecSQL(sQLiteDatabase, "ALTER TABLE notification ADD COLUMN expire_time TIMESTAMP;");
        safeExecSQL(sQLiteDatabase, "UPDATE notification SET expire_time = created_time + 259200;");
        safeExecSQL(sQLiteDatabase, OneSignalDbContract.NotificationTable.INDEX_CREATE_EXPIRE_TIME);
    }

    private static void safeExecSQL(SQLiteDatabase sQLiteDatabase, String str) {
        try {
            sQLiteDatabase.execSQL(str);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    @Override // android.database.sqlite.SQLiteOpenHelper
    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "SDK version rolled back! Clearing OneSignal.db as it could be in an unexpected state.");
        Cursor cursorRawQuery = sQLiteDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        try {
            ArrayList<String> arrayList = new ArrayList(cursorRawQuery.getCount());
            while (cursorRawQuery.moveToNext()) {
                arrayList.add(cursorRawQuery.getString(0));
            }
            for (String str : arrayList) {
                if (!str.startsWith("sqlite_")) {
                    sQLiteDatabase.execSQL("DROP TABLE IF EXISTS " + str);
                }
            }
            cursorRawQuery.close();
            onCreate(sQLiteDatabase);
        } catch (Throwable th) {
            cursorRawQuery.close();
            throw th;
        }
    }

    static StringBuilder recentUninteractedWithNotificationsWhere() {
        long jCurrentTimeMillis = System.currentTimeMillis() / 1000;
        StringBuilder sb = new StringBuilder("created_time > " + (jCurrentTimeMillis - 604800) + " AND " + OneSignalDbContract.NotificationTable.COLUMN_NAME_DISMISSED + " = 0 AND " + OneSignalDbContract.NotificationTable.COLUMN_NAME_OPENED + " = 0 AND " + OneSignalDbContract.NotificationTable.COLUMN_NAME_IS_SUMMARY + " = 0");
        if (OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_RESTORE_TTL_FILTER, true)) {
            sb.append(" AND expire_time > " + jCurrentTimeMillis);
        }
        return sb;
    }
}
