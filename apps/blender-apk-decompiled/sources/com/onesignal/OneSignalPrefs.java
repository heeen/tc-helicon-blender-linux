package com.onesignal;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import com.onesignal.OneSignal;
import java.util.HashMap;

/* JADX INFO: loaded from: classes.dex */
class OneSignalPrefs {
    static final String PREFS_EXISTING_PURCHASES = "ExistingPurchases";
    public static final String PREFS_GT_APP_ID = "GT_APP_ID";
    public static final String PREFS_GT_DO_NOT_SHOW_MISSING_GPS = "GT_DO_NOT_SHOW_MISSING_GPS";
    public static final String PREFS_GT_FIREBASE_TRACKING_ENABLED = "GT_FIREBASE_TRACKING_ENABLED";
    public static final String PREFS_GT_PLAYER_ID = "GT_PLAYER_ID";
    public static final String PREFS_GT_REGISTRATION_ID = "GT_REGISTRATION_ID";
    public static final String PREFS_GT_SOUND_ENABLED = "GT_SOUND_ENABLED";
    public static final String PREFS_GT_UNSENT_ACTIVE_TIME = "GT_UNSENT_ACTIVE_TIME";
    public static final String PREFS_GT_VIBRATE_ENABLED = "GT_VIBRATE_ENABLED";
    public static final String PREFS_ONESIGNAL = OneSignal.class.getSimpleName();
    public static final String PREFS_ONESIGNAL_ACCEPTED_NOTIFICATION_LAST = "ONESIGNAL_ACCEPTED_NOTIFICATION_LAST";
    public static final String PREFS_ONESIGNAL_EMAIL_ADDRESS_LAST = "PREFS_ONESIGNAL_EMAIL_ADDRESS_LAST";
    public static final String PREFS_ONESIGNAL_EMAIL_ID_LAST = "PREFS_ONESIGNAL_EMAIL_ID_LAST";
    public static final String PREFS_ONESIGNAL_PERMISSION_ACCEPTED_LAST = "ONESIGNAL_PERMISSION_ACCEPTED_LAST";
    public static final String PREFS_ONESIGNAL_PLAYER_ID_LAST = "ONESIGNAL_PLAYER_ID_LAST";
    public static final String PREFS_ONESIGNAL_PUSH_TOKEN_LAST = "ONESIGNAL_PUSH_TOKEN_LAST";
    public static final String PREFS_ONESIGNAL_SUBSCRIPTION = "ONESIGNAL_SUBSCRIPTION";
    public static final String PREFS_ONESIGNAL_SUBSCRIPTION_LAST = "ONESIGNAL_SUBSCRIPTION_LAST";
    public static final String PREFS_ONESIGNAL_SYNCED_SUBSCRIPTION = "ONESIGNAL_SYNCED_SUBSCRIPTION";
    public static final String PREFS_ONESIGNAL_USERSTATE_DEPENDVALYES_ = "ONESIGNAL_USERSTATE_DEPENDVALYES_";
    public static final String PREFS_ONESIGNAL_USERSTATE_SYNCVALYES_ = "ONESIGNAL_USERSTATE_SYNCVALYES_";
    public static final String PREFS_ONESIGNAL_USER_PROVIDED_CONSENT = "ONESIGNAL_USER_PROVIDED_CONSENT";
    public static final String PREFS_OS_EMAIL_ID = "OS_EMAIL_ID";
    public static final String PREFS_OS_ETAG_PREFIX = "PREFS_OS_ETAG_PREFIX_";
    public static final String PREFS_OS_FILTER_OTHER_GCM_RECEIVERS = "OS_FILTER_OTHER_GCM_RECEIVERS";
    public static final String PREFS_OS_HTTP_CACHE_PREFIX = "PREFS_OS_HTTP_CACHE_PREFIX_";
    public static final String PREFS_OS_LAST_LOCATION_TIME = "OS_LAST_LOCATION_TIME";
    public static final String PREFS_OS_LAST_SESSION_TIME = "OS_LAST_SESSION_TIME";
    public static final String PREFS_OS_RESTORE_TTL_FILTER = "OS_RESTORE_TTL_FILTER";
    public static final String PREFS_PLAYER_PURCHASES = "GTPlayerPurchases";
    static final String PREFS_PURCHASE_TOKENS = "purchaseTokens";
    public static WritePrefHandlerThread prefsHandler;
    static HashMap<String, HashMap<String, Object>> prefsToApply;

    OneSignalPrefs() {
    }

    static {
        initializePool();
    }

    public static class WritePrefHandlerThread extends HandlerThread {
        private static final int WRITE_CALL_DELAY_TO_BUFFER_MS = 200;
        private long lastSyncTime;
        public Handler mHandler;

        WritePrefHandlerThread() {
            super("OSH_WritePrefs");
            this.lastSyncTime = 0L;
            start();
            this.mHandler = new Handler(getLooper());
        }

        void startDelayedWrite() {
            synchronized (this.mHandler) {
                this.mHandler.removeCallbacksAndMessages(null);
                if (this.lastSyncTime == 0) {
                    this.lastSyncTime = System.currentTimeMillis();
                }
                this.mHandler.postDelayed(getNewRunnable(), (this.lastSyncTime - System.currentTimeMillis()) + 200);
            }
        }

        private Runnable getNewRunnable() {
            return new Runnable() { // from class: com.onesignal.OneSignalPrefs.WritePrefHandlerThread.1
                @Override // java.lang.Runnable
                public void run() {
                    WritePrefHandlerThread.this.flushBufferToDisk();
                }
            };
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void flushBufferToDisk() {
            if (OneSignal.appContext == null) {
                return;
            }
            for (String str : OneSignalPrefs.prefsToApply.keySet()) {
                SharedPreferences.Editor editorEdit = OneSignalPrefs.getSharedPrefsByName(str).edit();
                HashMap<String, Object> map = OneSignalPrefs.prefsToApply.get(str);
                synchronized (map) {
                    for (String str2 : map.keySet()) {
                        Object obj = map.get(str2);
                        if (obj instanceof String) {
                            editorEdit.putString(str2, (String) obj);
                        } else if (obj instanceof Boolean) {
                            editorEdit.putBoolean(str2, ((Boolean) obj).booleanValue());
                        } else if (obj instanceof Integer) {
                            editorEdit.putInt(str2, ((Integer) obj).intValue());
                        } else if (obj instanceof Long) {
                            editorEdit.putLong(str2, ((Long) obj).longValue());
                        }
                    }
                    map.clear();
                }
                editorEdit.apply();
            }
            this.lastSyncTime = System.currentTimeMillis();
        }
    }

    public static void initializePool() {
        prefsToApply = new HashMap<>();
        prefsToApply.put(PREFS_ONESIGNAL, new HashMap<>());
        prefsToApply.put(PREFS_PLAYER_PURCHASES, new HashMap<>());
        prefsHandler = new WritePrefHandlerThread();
    }

    public static void startDelayedWrite() {
        prefsHandler.startDelayedWrite();
    }

    public static void saveString(String str, String str2, String str3) {
        save(str, str2, str3);
    }

    public static void saveBool(String str, String str2, boolean z) {
        save(str, str2, Boolean.valueOf(z));
    }

    public static void saveInt(String str, String str2, int i) {
        save(str, str2, Integer.valueOf(i));
    }

    public static void saveLong(String str, String str2, long j) {
        save(str, str2, Long.valueOf(j));
    }

    private static void save(String str, String str2, Object obj) {
        HashMap<String, Object> map = prefsToApply.get(str);
        synchronized (map) {
            map.put(str2, obj);
        }
        startDelayedWrite();
    }

    static String getString(String str, String str2, String str3) {
        return (String) get(str, str2, String.class, str3);
    }

    static boolean getBool(String str, String str2, boolean z) {
        return ((Boolean) get(str, str2, Boolean.class, Boolean.valueOf(z))).booleanValue();
    }

    static int getInt(String str, String str2, int i) {
        return ((Integer) get(str, str2, Integer.class, Integer.valueOf(i))).intValue();
    }

    static long getLong(String str, String str2, long j) {
        return ((Long) get(str, str2, Long.class, Long.valueOf(j))).longValue();
    }

    private static Object get(String str, String str2, Class cls, Object obj) {
        HashMap<String, Object> map = prefsToApply.get(str);
        synchronized (map) {
            if (cls.equals(Object.class) && map.containsKey(str2)) {
                return true;
            }
            Object obj2 = map.get(str2);
            if (obj2 == null && !map.containsKey(str2)) {
                SharedPreferences sharedPrefsByName = getSharedPrefsByName(str);
                if (sharedPrefsByName == null) {
                    return obj;
                }
                if (cls.equals(String.class)) {
                    return sharedPrefsByName.getString(str2, (String) obj);
                }
                if (cls.equals(Boolean.class)) {
                    return Boolean.valueOf(sharedPrefsByName.getBoolean(str2, ((Boolean) obj).booleanValue()));
                }
                if (cls.equals(Integer.class)) {
                    return Integer.valueOf(sharedPrefsByName.getInt(str2, ((Integer) obj).intValue()));
                }
                if (cls.equals(Long.class)) {
                    return Long.valueOf(sharedPrefsByName.getLong(str2, ((Long) obj).longValue()));
                }
                if (cls.equals(Object.class)) {
                    return Boolean.valueOf(sharedPrefsByName.contains(str2));
                }
                return null;
            }
            return obj2;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static synchronized SharedPreferences getSharedPrefsByName(String str) {
        if (OneSignal.appContext == null) {
            OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "OneSignal.appContext null, could not read " + str + " from getSharedPreferences.", new Throwable());
            return null;
        }
        return OneSignal.appContext.getSharedPreferences(str, 0);
    }
}
