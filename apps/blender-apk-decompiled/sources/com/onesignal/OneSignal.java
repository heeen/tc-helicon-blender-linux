package com.onesignal;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.util.Base64;
import android.util.Log;
import com.onesignal.LocationGMS;
import com.onesignal.OSNotification;
import com.onesignal.OSNotificationAction;
import com.onesignal.OneSignalDbContract;
import com.onesignal.OneSignalRemoteParams;
import com.onesignal.OneSignalRestClient;
import com.onesignal.PushRegistrator;
import com.onesignal.UserStateSynchronizer;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class OneSignal {
    static final long MIN_ON_FOCUS_TIME = 60;
    private static final long MIN_ON_SESSION_TIME = 30;
    public static final String VERSION = "031008";
    static Context appContext;
    static String appId;
    private static OSEmailSubscriptionState currentEmailSubscriptionState;
    private static OSPermissionState currentPermissionState;
    private static OSSubscriptionState currentSubscriptionState;
    static DelayedConsentInitializationParameters delayedInitParams;
    private static int deviceType;
    private static EmailUpdateHandler emailLogoutHandler;
    private static OSObservable<OSEmailSubscriptionObserver, OSEmailSubscriptionStateChanges> emailSubscriptionStateChangesObserver;
    private static EmailUpdateHandler emailUpdateHandler;
    private static boolean foreground;
    private static boolean getTagsCall;
    private static IAPUpdateJob iapUpdateJob;
    private static IdsAvailableHandler idsAvailableHandler;
    static boolean initDone;
    static OSEmailSubscriptionState lastEmailSubscriptionState;
    private static LocationGMS.LocationPoint lastLocationPoint;
    static OSPermissionState lastPermissionState;
    private static String lastRegistrationId;
    static OSSubscriptionState lastSubscriptionState;
    private static boolean locationFired;
    private static String mGoogleProjectNumber;
    static Builder mInitBuilder;
    private static PushRegistrator mPushRegistrator;
    private static OSUtils osUtils;
    static ExecutorService pendingTaskExecutor;
    private static OSObservable<OSPermissionObserver, OSPermissionStateChanges> permissionStateChangesObserver;
    private static boolean promptedLocation;
    private static boolean registerForPushFired;
    static OneSignalRemoteParams.Params remoteParams;
    private static int subscribableStatus;
    private static OSObservable<OSSubscriptionObserver, OSSubscriptionStateChanges> subscriptionStateChangesObserver;
    private static TrackAmazonPurchase trackAmazonPurchase;
    private static TrackFirebaseAnalytics trackFirebaseAnalytics;
    private static TrackGooglePurchase trackGooglePurchase;
    private static boolean waitingToPostStateSync;
    private static LOG_LEVEL visualLogLevel = LOG_LEVEL.NONE;
    private static LOG_LEVEL logCatLevel = LOG_LEVEL.WARN;
    private static String userId = null;
    private static String emailId = null;
    public static ConcurrentLinkedQueue<Runnable> taskQueueWaitingForInit = new ConcurrentLinkedQueue<>();
    static AtomicLong lastTaskId = new AtomicLong();
    private static long lastTrackedFocusTime = 1;
    private static long unSentActiveTime = -1;
    private static AdvertisingIdentifierProvider mainAdIdProvider = new AdvertisingIdProviderGPS();
    public static String sdkType = "native";
    static boolean shareLocation = true;
    private static Collection<JSONArray> unprocessedOpenedNotifis = new ArrayList();
    private static HashSet<String> postedOpenedNotifIds = new HashSet<>();
    private static ArrayList<GetTagsHandler> pendingGetTagsHandlers = new ArrayList<>();
    static boolean requiresUserPrivacyConsent = false;

    public interface ChangeTagsUpdateHandler {
        void onFailure(SendTagsError sendTagsError);

        void onSuccess(JSONObject jSONObject);
    }

    public enum EmailErrorType {
        VALIDATION,
        REQUIRES_EMAIL_AUTH,
        INVALID_OPERATION,
        NETWORK
    }

    public interface EmailUpdateHandler {
        void onFailure(EmailUpdateError emailUpdateError);

        void onSuccess();
    }

    public interface GetTagsHandler {
        void tagsAvailable(JSONObject jSONObject);
    }

    public interface IdsAvailableHandler {
        void idsAvailable(String str, String str2);
    }

    public enum LOG_LEVEL {
        NONE,
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        VERBOSE
    }

    public interface NotificationOpenedHandler {
        void notificationOpened(OSNotificationOpenResult oSNotificationOpenResult);
    }

    public interface NotificationReceivedHandler {
        void notificationReceived(OSNotification oSNotification);
    }

    public enum OSInFocusDisplayOption {
        None,
        InAppAlert,
        Notification
    }

    public interface PostNotificationResponseHandler {
        void onFailure(JSONObject jSONObject);

        void onSuccess(JSONObject jSONObject);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static boolean pushStatusRuntimeError(int i) {
        return i < -6;
    }

    public static class SendTagsError {
        private int code;
        private String message;

        SendTagsError(int i, String str) {
            this.message = str;
            this.code = i;
        }

        public int getCode() {
            return this.code;
        }

        public String getMessage() {
            return this.message;
        }
    }

    public static class EmailUpdateError {
        private String message;
        private EmailErrorType type;

        EmailUpdateError(EmailErrorType emailErrorType, String str) {
            this.type = emailErrorType;
            this.message = str;
        }

        public EmailErrorType getType() {
            return this.type;
        }

        public String getMessage() {
            return this.message;
        }
    }

    public static class Builder {
        Context mContext;
        boolean mDisableGmsMissingPrompt;
        OSInFocusDisplayOption mDisplayOption;
        boolean mDisplayOptionCarryOver;
        boolean mFilterOtherGCMReceivers;
        NotificationOpenedHandler mNotificationOpenedHandler;
        NotificationReceivedHandler mNotificationReceivedHandler;
        boolean mPromptLocation;
        boolean mUnsubscribeWhenNotificationsAreDisabled;

        private Builder() {
            this.mDisplayOption = OSInFocusDisplayOption.InAppAlert;
        }

        private Builder(Context context) {
            this.mDisplayOption = OSInFocusDisplayOption.InAppAlert;
            this.mContext = context;
        }

        private void setDisplayOptionCarryOver(boolean z) {
            this.mDisplayOptionCarryOver = z;
        }

        public Builder setNotificationOpenedHandler(NotificationOpenedHandler notificationOpenedHandler) {
            this.mNotificationOpenedHandler = notificationOpenedHandler;
            return this;
        }

        public Builder setNotificationReceivedHandler(NotificationReceivedHandler notificationReceivedHandler) {
            this.mNotificationReceivedHandler = notificationReceivedHandler;
            return this;
        }

        public Builder autoPromptLocation(boolean z) {
            this.mPromptLocation = z;
            return this;
        }

        public Builder disableGmsMissingPrompt(boolean z) {
            this.mDisableGmsMissingPrompt = z;
            return this;
        }

        public Builder inFocusDisplaying(OSInFocusDisplayOption oSInFocusDisplayOption) {
            OneSignal.getCurrentOrNewInitBuilder().mDisplayOptionCarryOver = false;
            this.mDisplayOption = oSInFocusDisplayOption;
            return this;
        }

        public Builder unsubscribeWhenNotificationsAreDisabled(boolean z) {
            this.mUnsubscribeWhenNotificationsAreDisabled = z;
            return this;
        }

        public Builder filterOtherGCMReceivers(boolean z) {
            this.mFilterOtherGCMReceivers = z;
            return this;
        }

        public void init() {
            OneSignal.init(this);
        }
    }

    private static OSPermissionState getCurrentPermissionState(Context context) {
        if (context == null) {
            return null;
        }
        if (currentPermissionState == null) {
            currentPermissionState = new OSPermissionState(false);
            currentPermissionState.observable.addObserverStrong(new OSPermissionChangedInternalObserver());
        }
        return currentPermissionState;
    }

    private static OSPermissionState getLastPermissionState(Context context) {
        if (context == null) {
            return null;
        }
        if (lastPermissionState == null) {
            lastPermissionState = new OSPermissionState(true);
        }
        return lastPermissionState;
    }

    static OSObservable<OSPermissionObserver, OSPermissionStateChanges> getPermissionStateChangesObserver() {
        if (permissionStateChangesObserver == null) {
            permissionStateChangesObserver = new OSObservable<>("onOSPermissionChanged", true);
        }
        return permissionStateChangesObserver;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static OSSubscriptionState getCurrentSubscriptionState(Context context) {
        if (context == null) {
            return null;
        }
        if (currentSubscriptionState == null) {
            currentSubscriptionState = new OSSubscriptionState(false, getCurrentPermissionState(context).getEnabled());
            getCurrentPermissionState(context).observable.addObserver(currentSubscriptionState);
            currentSubscriptionState.observable.addObserverStrong(new OSSubscriptionChangedInternalObserver());
        }
        return currentSubscriptionState;
    }

    private static OSSubscriptionState getLastSubscriptionState(Context context) {
        if (context == null) {
            return null;
        }
        if (lastSubscriptionState == null) {
            lastSubscriptionState = new OSSubscriptionState(true, false);
        }
        return lastSubscriptionState;
    }

    static OSObservable<OSSubscriptionObserver, OSSubscriptionStateChanges> getSubscriptionStateChangesObserver() {
        if (subscriptionStateChangesObserver == null) {
            subscriptionStateChangesObserver = new OSObservable<>("onOSSubscriptionChanged", true);
        }
        return subscriptionStateChangesObserver;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static OSEmailSubscriptionState getCurrentEmailSubscriptionState(Context context) {
        if (context == null) {
            return null;
        }
        if (currentEmailSubscriptionState == null) {
            currentEmailSubscriptionState = new OSEmailSubscriptionState(false);
            currentEmailSubscriptionState.observable.addObserverStrong(new OSEmailSubscriptionChangedInternalObserver());
        }
        return currentEmailSubscriptionState;
    }

    private static OSEmailSubscriptionState getLastEmailSubscriptionState(Context context) {
        if (context == null) {
            return null;
        }
        if (lastEmailSubscriptionState == null) {
            lastEmailSubscriptionState = new OSEmailSubscriptionState(true);
        }
        return lastEmailSubscriptionState;
    }

    static OSObservable<OSEmailSubscriptionObserver, OSEmailSubscriptionStateChanges> getEmailSubscriptionStateChangesObserver() {
        if (emailSubscriptionStateChangesObserver == null) {
            emailSubscriptionStateChangesObserver = new OSObservable<>("onOSEmailSubscriptionChanged", true);
        }
        return emailSubscriptionStateChangesObserver;
    }

    private static class IAPUpdateJob {
        boolean newAsExisting;
        OneSignalRestClient.ResponseHandler restResponseHandler;
        JSONArray toReport;

        IAPUpdateJob(JSONArray jSONArray) {
            this.toReport = jSONArray;
        }
    }

    public static Builder getCurrentOrNewInitBuilder() {
        if (mInitBuilder == null) {
            mInitBuilder = new Builder();
        }
        return mInitBuilder;
    }

    public static void setAppContext(@NonNull Context context) {
        if (context == null) {
            Log(LOG_LEVEL.WARN, "setAppContext(null) is not valid, ignoring!");
            return;
        }
        boolean z = appContext == null;
        appContext = context.getApplicationContext();
        if (z) {
            OneSignalPrefs.startDelayedWrite();
        }
    }

    public static Builder startInit(Context context) {
        return new Builder(context);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void init(Builder builder) {
        if (getCurrentOrNewInitBuilder().mDisplayOptionCarryOver) {
            builder.mDisplayOption = getCurrentOrNewInitBuilder().mDisplayOption;
        }
        mInitBuilder = builder;
        Context context = mInitBuilder.mContext;
        mInitBuilder.mContext = null;
        try {
            Bundle bundle = context.getPackageManager().getApplicationInfo(context.getPackageName(), 128).metaData;
            String string = bundle.getString("onesignal_google_project_number");
            if (string != null && string.length() > 4) {
                string = string.substring(4);
            }
            setRequiresUserPrivacyConsent("ENABLE".equalsIgnoreCase(bundle.getString("com.onesignal.PrivacyConsent")));
            init(context, string, bundle.getString("onesignal_app_id"), mInitBuilder.mNotificationOpenedHandler, mInitBuilder.mNotificationReceivedHandler);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public static void init(Context context, String str, String str2) {
        init(context, str, str2, null, null);
    }

    public static void init(Context context, String str, String str2, NotificationOpenedHandler notificationOpenedHandler) {
        init(context, str, str2, notificationOpenedHandler, null);
    }

    public static void init(Context context, String str, String str2, NotificationOpenedHandler notificationOpenedHandler, NotificationReceivedHandler notificationReceivedHandler) {
        setAppContext(context);
        if (requiresUserPrivacyConsent && !userProvidedPrivacyConsent()) {
            Log(LOG_LEVEL.VERBOSE, "OneSignal SDK initialization delayed, user privacy consent is set to required for this application.");
            delayedInitParams = new DelayedConsentInitializationParameters(context, str, str2, notificationOpenedHandler, notificationReceivedHandler);
            return;
        }
        mInitBuilder = getCurrentOrNewInitBuilder();
        mInitBuilder.mDisplayOptionCarryOver = false;
        mInitBuilder.mNotificationOpenedHandler = notificationOpenedHandler;
        mInitBuilder.mNotificationReceivedHandler = notificationReceivedHandler;
        if (!((remoteParams == null || remoteParams.googleProjectNumber == null) ? false : true)) {
            mGoogleProjectNumber = str;
        }
        osUtils = new OSUtils();
        deviceType = osUtils.getDeviceType();
        subscribableStatus = osUtils.initializationChecker(context, deviceType, str2);
        if (subscribableStatus == -999) {
            return;
        }
        if (initDone) {
            if (mInitBuilder.mNotificationOpenedHandler != null) {
                fireCallbackForOpenedNotifications();
                return;
            }
            return;
        }
        boolean z = context instanceof Activity;
        foreground = z;
        appId = str2;
        saveFilterOtherGCMReceivers(mInitBuilder.mFilterOtherGCMReceivers);
        if (z) {
            ActivityLifecycleHandler.curActivity = (Activity) context;
            NotificationRestorer.asyncRestore(appContext);
        } else {
            ActivityLifecycleHandler.nextResumeIsFirstActivity = true;
        }
        lastTrackedFocusTime = SystemClock.elapsedRealtime();
        OneSignalStateSynchronizer.initUserState();
        ((Application) appContext).registerActivityLifecycleCallbacks(new ActivityLifecycleListener());
        try {
            Class.forName("com.amazon.device.iap.PurchasingListener");
            trackAmazonPurchase = new TrackAmazonPurchase(appContext);
        } catch (ClassNotFoundException unused) {
        }
        String savedAppId = getSavedAppId();
        if (savedAppId != null) {
            if (!savedAppId.equals(appId)) {
                Log(LOG_LEVEL.DEBUG, "APP ID changed, clearing user id as it is no longer valid.");
                SaveAppId(appId);
                OneSignalStateSynchronizer.resetCurrentState();
            }
        } else {
            BadgeCountUpdater.updateCount(0, appContext);
            SaveAppId(appId);
        }
        OSPermissionChangedInternalObserver.handleInternalChanges(getCurrentPermissionState(appContext));
        if (foreground || getUserId() == null) {
            if (isPastOnSessionTime()) {
                OneSignalStateSynchronizer.setNewSession();
            }
            setLastSessionTime(System.currentTimeMillis());
            startRegistrationOrOnSession();
        }
        if (mInitBuilder.mNotificationOpenedHandler != null) {
            fireCallbackForOpenedNotifications();
        }
        if (TrackGooglePurchase.CanTrack(appContext)) {
            trackGooglePurchase = new TrackGooglePurchase(appContext);
        }
        if (TrackFirebaseAnalytics.CanTrack()) {
            trackFirebaseAnalytics = new TrackFirebaseAnalytics(appContext);
        }
        PushRegistratorFCM.disableFirebaseInstanceIdService(appContext);
        initDone = true;
        startPendingTasks();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void onTaskRan(long j) {
        if (lastTaskId.get() == j) {
            Log(LOG_LEVEL.INFO, "Last Pending Task has ran, shutting down");
            pendingTaskExecutor.shutdown();
        }
    }

    private static class PendingTaskRunnable implements Runnable {
        private Runnable innerTask;
        private long taskId;

        PendingTaskRunnable(Runnable runnable) {
            this.innerTask = runnable;
        }

        @Override // java.lang.Runnable
        public void run() {
            this.innerTask.run();
            OneSignal.onTaskRan(this.taskId);
        }
    }

    private static void startPendingTasks() {
        if (taskQueueWaitingForInit.isEmpty()) {
            return;
        }
        pendingTaskExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() { // from class: com.onesignal.OneSignal.1
            @Override // java.util.concurrent.ThreadFactory
            public Thread newThread(@NonNull Runnable runnable) {
                Thread thread = new Thread(runnable);
                thread.setName("OS_PENDING_EXECUTOR_" + thread.getId());
                return thread;
            }
        });
        while (!taskQueueWaitingForInit.isEmpty()) {
            pendingTaskExecutor.submit(taskQueueWaitingForInit.poll());
        }
    }

    private static void addTaskToQueue(PendingTaskRunnable pendingTaskRunnable) {
        pendingTaskRunnable.taskId = lastTaskId.incrementAndGet();
        if (pendingTaskExecutor == null) {
            Log(LOG_LEVEL.INFO, "Adding a task to the pending queue with ID: " + pendingTaskRunnable.taskId);
            taskQueueWaitingForInit.add(pendingTaskRunnable);
            return;
        }
        if (pendingTaskExecutor.isShutdown()) {
            return;
        }
        Log(LOG_LEVEL.INFO, "Executor is still running, add to the executor with ID: " + pendingTaskRunnable.taskId);
        pendingTaskExecutor.submit(pendingTaskRunnable);
    }

    private static boolean shouldRunTaskThroughQueue() {
        if (initDone && pendingTaskExecutor == null) {
            return false;
        }
        if (initDone || pendingTaskExecutor != null) {
            return (pendingTaskExecutor == null || pendingTaskExecutor.isShutdown()) ? false : true;
        }
        return true;
    }

    private static void startRegistrationOrOnSession() {
        if (waitingToPostStateSync) {
            return;
        }
        waitingToPostStateSync = true;
        if (OneSignalStateSynchronizer.getSyncAsNewSession()) {
            locationFired = false;
        }
        startLocationUpdate();
        registerForPushFired = false;
        makeAndroidParamsRequest();
    }

    private static void startLocationUpdate() {
        LocationGMS.LocationHandler locationHandler = new LocationGMS.LocationHandler() { // from class: com.onesignal.OneSignal.2
            @Override // com.onesignal.LocationGMS.LocationHandler
            public LocationGMS.CALLBACK_TYPE getType() {
                return LocationGMS.CALLBACK_TYPE.STARTUP;
            }

            @Override // com.onesignal.LocationGMS.LocationHandler
            public void complete(LocationGMS.LocationPoint locationPoint) {
                LocationGMS.LocationPoint unused = OneSignal.lastLocationPoint = locationPoint;
                boolean unused2 = OneSignal.locationFired = true;
                OneSignal.registerUser();
            }
        };
        boolean z = true;
        boolean z2 = mInitBuilder.mPromptLocation && !promptedLocation;
        if (!promptedLocation && !mInitBuilder.mPromptLocation) {
            z = false;
        }
        promptedLocation = z;
        LocationGMS.getLocation(appContext, z2, locationHandler);
    }

    private static PushRegistrator getPushRegistrator() {
        if (mPushRegistrator != null) {
            return mPushRegistrator;
        }
        if (deviceType == 2) {
            mPushRegistrator = new PushRegistratorADM();
        } else if (OSUtils.hasFCMLibrary()) {
            mPushRegistrator = new PushRegistratorFCM();
        } else {
            mPushRegistrator = new PushRegistratorGCM();
        }
        return mPushRegistrator;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void registerForPushToken() {
        getPushRegistrator().registerForPush(appContext, mGoogleProjectNumber, new PushRegistrator.RegisteredHandler() { // from class: com.onesignal.OneSignal.3
            @Override // com.onesignal.PushRegistrator.RegisteredHandler
            public void complete(String str, int i) {
                if (i >= 1) {
                    if (OneSignal.pushStatusRuntimeError(OneSignal.subscribableStatus)) {
                        int unused = OneSignal.subscribableStatus = i;
                    }
                } else if (OneSignalStateSynchronizer.getRegistrationId() == null && (OneSignal.subscribableStatus == 1 || OneSignal.pushStatusRuntimeError(OneSignal.subscribableStatus))) {
                    int unused2 = OneSignal.subscribableStatus = i;
                }
                String unused3 = OneSignal.lastRegistrationId = str;
                boolean unused4 = OneSignal.registerForPushFired = true;
                OneSignal.getCurrentSubscriptionState(OneSignal.appContext).setPushToken(str);
                OneSignal.registerUser();
            }
        });
    }

    private static void makeAndroidParamsRequest() {
        if (remoteParams != null) {
            registerForPushToken();
        } else {
            OneSignalRemoteParams.makeAndroidParamsRequest(new OneSignalRemoteParams.CallBack() { // from class: com.onesignal.OneSignal.4
                @Override // com.onesignal.OneSignalRemoteParams.CallBack
                public void complete(OneSignalRemoteParams.Params params) {
                    OneSignal.remoteParams = params;
                    if (OneSignal.remoteParams.googleProjectNumber != null) {
                        String unused = OneSignal.mGoogleProjectNumber = OneSignal.remoteParams.googleProjectNumber;
                    }
                    OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_FIREBASE_TRACKING_ENABLED, OneSignal.remoteParams.firebaseAnalytics);
                    OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_RESTORE_TTL_FILTER, OneSignal.remoteParams.restoreTTLFilter);
                    NotificationChannelManager.processChannelList(OneSignal.appContext, params.notificationChannels);
                    OneSignal.registerForPushToken();
                }
            });
        }
    }

    private static void fireCallbackForOpenedNotifications() {
        Iterator<JSONArray> it = unprocessedOpenedNotifis.iterator();
        while (it.hasNext()) {
            runNotificationOpenedCallback(it.next(), true, false);
        }
        unprocessedOpenedNotifis.clear();
    }

    public static void onesignalLog(LOG_LEVEL log_level, String str) {
        Log(log_level, str);
    }

    public static boolean userProvidedPrivacyConsent() {
        return getSavedUserConsentStatus();
    }

    public static void provideUserConsent(boolean z) {
        boolean zUserProvidedPrivacyConsent = userProvidedPrivacyConsent();
        saveUserConsentStatus(z);
        if (zUserProvidedPrivacyConsent || !z || delayedInitParams == null) {
            return;
        }
        init(delayedInitParams.context, delayedInitParams.googleProjectNumber, delayedInitParams.appId, delayedInitParams.openedHandler, delayedInitParams.receivedHandler);
        delayedInitParams = null;
    }

    public static void setRequiresUserPrivacyConsent(boolean z) {
        if (requiresUserPrivacyConsent && !z) {
            Log(LOG_LEVEL.ERROR, "Cannot change requiresUserPrivacyConsent() from TRUE to FALSE");
        } else {
            requiresUserPrivacyConsent = z;
        }
    }

    public static boolean requiresUserPrivacyConsent() {
        return requiresUserPrivacyConsent && !userProvidedPrivacyConsent();
    }

    static boolean shouldLogUserPrivacyConsentErrorMessageForMethodName(String str) {
        if (!requiresUserPrivacyConsent || userProvidedPrivacyConsent()) {
            return false;
        }
        if (str == null) {
            return true;
        }
        Log(LOG_LEVEL.WARN, "Method " + str + " was called before the user provided privacy consent. Your application is set to require the user's privacy consent before the OneSignal SDK can be initialized. Please ensure the user has provided consent before calling this method. You can check the latest OneSignal consent status by calling OneSignal.userProvidedPrivacyConsent()");
        return true;
    }

    public static void setLogLevel(LOG_LEVEL log_level, LOG_LEVEL log_level2) {
        logCatLevel = log_level;
        visualLogLevel = log_level2;
    }

    public static void setLogLevel(int i, int i2) {
        setLogLevel(getLogLevel(i), getLogLevel(i2));
    }

    private static LOG_LEVEL getLogLevel(int i) {
        switch (i) {
            case 0:
                return LOG_LEVEL.NONE;
            case 1:
                return LOG_LEVEL.FATAL;
            case 2:
                return LOG_LEVEL.ERROR;
            case 3:
                return LOG_LEVEL.WARN;
            case 4:
                return LOG_LEVEL.INFO;
            case 5:
                return LOG_LEVEL.DEBUG;
            case 6:
                return LOG_LEVEL.VERBOSE;
            default:
                if (i < 0) {
                    return LOG_LEVEL.NONE;
                }
                return LOG_LEVEL.VERBOSE;
        }
    }

    private static boolean atLogLevel(LOG_LEVEL log_level) {
        return log_level.compareTo(visualLogLevel) < 1 || log_level.compareTo(logCatLevel) < 1;
    }

    static void Log(LOG_LEVEL log_level, String str) {
        Log(log_level, str, null);
    }

    static void Log(final LOG_LEVEL log_level, String str, Throwable th) {
        if (log_level.compareTo(logCatLevel) < 1) {
            if (log_level == LOG_LEVEL.VERBOSE) {
                Log.v("OneSignal", str, th);
            } else if (log_level == LOG_LEVEL.DEBUG) {
                Log.d("OneSignal", str, th);
            } else if (log_level == LOG_LEVEL.INFO) {
                Log.i("OneSignal", str, th);
            } else if (log_level == LOG_LEVEL.WARN) {
                Log.w("OneSignal", str, th);
            } else if (log_level == LOG_LEVEL.ERROR || log_level == LOG_LEVEL.FATAL) {
                Log.e("OneSignal", str, th);
            }
        }
        if (log_level.compareTo(visualLogLevel) >= 1 || ActivityLifecycleHandler.curActivity == null) {
            return;
        }
        try {
            final String str2 = str + "\n";
            if (th != null) {
                String str3 = str2 + th.getMessage();
                StringWriter stringWriter = new StringWriter();
                th.printStackTrace(new PrintWriter(stringWriter));
                str2 = str3 + stringWriter.toString();
            }
            OSUtils.runOnMainUIThread(new Runnable() { // from class: com.onesignal.OneSignal.5
                @Override // java.lang.Runnable
                public void run() {
                    if (ActivityLifecycleHandler.curActivity != null) {
                        new AlertDialog.Builder(ActivityLifecycleHandler.curActivity).setTitle(log_level.toString()).setMessage(str2).show();
                    }
                }
            });
        } catch (Throwable th2) {
            Log.e("OneSignal", "Error showing logging message.", th2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void logHttpError(String str, int i, Throwable th, String str2) {
        String str3 = "";
        if (str2 != null && atLogLevel(LOG_LEVEL.INFO)) {
            str3 = "\n" + str2 + "\n";
        }
        Log(LOG_LEVEL.WARN, "HTTP code: " + i + " " + str + str3, th);
    }

    @WorkerThread
    static boolean onAppLostFocus() {
        foreground = false;
        setLastSessionTime(System.currentTimeMillis());
        LocationGMS.onFocusChange();
        if (!initDone) {
            return false;
        }
        if (trackAmazonPurchase != null) {
            trackAmazonPurchase.checkListener();
        }
        if (lastTrackedFocusTime == -1) {
            return false;
        }
        long jElapsedRealtime = (long) (((SystemClock.elapsedRealtime() - lastTrackedFocusTime) / 1000.0d) + 0.5d);
        lastTrackedFocusTime = SystemClock.elapsedRealtime();
        if (jElapsedRealtime < 0 || jElapsedRealtime > 86400) {
            return false;
        }
        if (appContext == null) {
            Log(LOG_LEVEL.ERROR, "Android Context not found, please call OneSignal.init when your app starts.");
            return false;
        }
        boolean zScheduleSyncService = scheduleSyncService();
        long jGetUnsentActiveTime = GetUnsentActiveTime() + jElapsedRealtime;
        SaveUnsentActiveTime(jGetUnsentActiveTime);
        if (jGetUnsentActiveTime < MIN_ON_FOCUS_TIME || getUserId() == null) {
            return jGetUnsentActiveTime >= MIN_ON_FOCUS_TIME;
        }
        if (!zScheduleSyncService) {
            OneSignalSyncServiceUtils.scheduleSyncTask(appContext);
        }
        OneSignalSyncServiceUtils.syncOnFocusTime();
        return false;
    }

    static boolean scheduleSyncService() {
        boolean zPersist = OneSignalStateSynchronizer.persist();
        if (zPersist) {
            OneSignalSyncServiceUtils.scheduleSyncTask(appContext);
        }
        return LocationGMS.scheduleUpdate(appContext) || zPersist;
    }

    static void sendOnFocus(long j, boolean z) {
        try {
            JSONObject jSONObjectPut = new JSONObject().put("app_id", appId).put("type", 1).put("state", "ping").put("active_time", j);
            addNetType(jSONObjectPut);
            sendOnFocusToPlayer(getUserId(), jSONObjectPut, z);
            String emailId2 = getEmailId();
            if (emailId2 != null) {
                sendOnFocusToPlayer(emailId2, jSONObjectPut, z);
            }
        } catch (Throwable th) {
            Log(LOG_LEVEL.ERROR, "Generating on_focus:JSON Failed.", th);
        }
    }

    private static void sendOnFocusToPlayer(String str, JSONObject jSONObject, boolean z) {
        String str2 = "players/" + str + "/on_focus";
        OneSignalRestClient.ResponseHandler responseHandler = new OneSignalRestClient.ResponseHandler() { // from class: com.onesignal.OneSignal.6
            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onFailure(int i, String str3, Throwable th) {
                OneSignal.logHttpError("sending on_focus Failed", i, th, str3);
            }

            @Override // com.onesignal.OneSignalRestClient.ResponseHandler
            void onSuccess(String str3) {
                OneSignal.SaveUnsentActiveTime(0L);
            }
        };
        if (z) {
            OneSignalRestClient.postSync(str2, jSONObject, responseHandler);
        } else {
            OneSignalRestClient.post(str2, jSONObject, responseHandler);
        }
    }

    static void onAppFocus() {
        foreground = true;
        LocationGMS.onFocusChange();
        lastTrackedFocusTime = SystemClock.elapsedRealtime();
        if (isPastOnSessionTime()) {
            OneSignalStateSynchronizer.setNewSession();
        }
        setLastSessionTime(System.currentTimeMillis());
        startRegistrationOrOnSession();
        if (trackGooglePurchase != null) {
            trackGooglePurchase.trackIAP();
        }
        NotificationRestorer.asyncRestore(appContext);
        getCurrentPermissionState(appContext).refreshAsTo();
        if (trackFirebaseAnalytics != null && getFirebaseAnalyticsEnabled(appContext)) {
            trackFirebaseAnalytics.trackInfluenceOpenEvent();
        }
        OneSignalSyncServiceUtils.cancelSyncTask(appContext);
    }

    static boolean isForeground() {
        return foreground;
    }

    private static void addNetType(JSONObject jSONObject) {
        try {
            jSONObject.put("net_type", osUtils.getNetType());
        } catch (Throwable unused) {
        }
    }

    private static int getTimeZoneOffset() {
        TimeZone timeZone = Calendar.getInstance().getTimeZone();
        int rawOffset = timeZone.getRawOffset();
        if (timeZone.inDaylightTime(new Date())) {
            rawOffset += timeZone.getDSTSavings();
        }
        return rawOffset / 1000;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void registerUser() {
        Log(LOG_LEVEL.DEBUG, "registerUser: registerForPushFired:" + registerForPushFired + ", locationFired: " + locationFired + ", remoteParams: " + remoteParams);
        if (registerForPushFired && locationFired && remoteParams != null) {
            new Thread(new Runnable() { // from class: com.onesignal.OneSignal.7
                @Override // java.lang.Runnable
                public void run() {
                    try {
                        OneSignal.registerUserTask();
                        OneSignalChromeTab.setup(OneSignal.appContext, OneSignal.appId, OneSignal.userId, AdvertisingIdProviderGPS.getLastValue());
                    } catch (JSONException e) {
                        OneSignal.Log(LOG_LEVEL.FATAL, "FATAL Error registering device!", e);
                    }
                }
            }, "OS_REG_USER").start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void registerUserTask() throws JSONException {
        String packageName = appContext.getPackageName();
        PackageManager packageManager = appContext.getPackageManager();
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("app_id", appId);
        Object identifier = mainAdIdProvider.getIdentifier(appContext);
        if (identifier != null) {
            jSONObject.put("ad_id", identifier);
        }
        jSONObject.put("device_os", Build.VERSION.RELEASE);
        jSONObject.put("timezone", getTimeZoneOffset());
        jSONObject.put("language", OSUtils.getCorrectedLanguage());
        jSONObject.put("sdk", VERSION);
        jSONObject.put("sdk_type", sdkType);
        jSONObject.put("android_package", packageName);
        jSONObject.put("device_model", Build.MODEL);
        try {
            jSONObject.put("game_version", packageManager.getPackageInfo(packageName, 0).versionCode);
        } catch (PackageManager.NameNotFoundException unused) {
        }
        try {
            List<PackageInfo> installedPackages = packageManager.getInstalledPackages(0);
            JSONArray jSONArray = new JSONArray();
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            for (int i = 0; i < installedPackages.size(); i++) {
                messageDigest.update(installedPackages.get(i).packageName.getBytes());
                String strEncodeToString = Base64.encodeToString(messageDigest.digest(), 2);
                if (remoteParams.awl.has(strEncodeToString)) {
                    jSONArray.put(strEncodeToString);
                }
            }
            jSONObject.put("pkgs", jSONArray);
        } catch (Throwable unused2) {
        }
        jSONObject.put("net_type", osUtils.getNetType());
        jSONObject.put("carrier", osUtils.getCarrierName());
        jSONObject.put("rooted", RootToolsInternalMethods.isRooted());
        OneSignalStateSynchronizer.updateDeviceInfo(jSONObject);
        JSONObject jSONObject2 = new JSONObject();
        jSONObject2.put("identifier", lastRegistrationId);
        jSONObject2.put("subscribableStatus", subscribableStatus);
        jSONObject2.put("androidPermission", areNotificationsEnabledForSubscribedState());
        jSONObject2.put("device_type", deviceType);
        OneSignalStateSynchronizer.updatePushState(jSONObject2);
        if (shareLocation && lastLocationPoint != null) {
            OneSignalStateSynchronizer.updateLocation(lastLocationPoint);
        }
        OneSignalStateSynchronizer.readyToUpdate(true);
        waitingToPostStateSync = false;
    }

    @Deprecated
    public static void syncHashedEmail(final String str) {
        if (!shouldLogUserPrivacyConsentErrorMessageForMethodName("SyncHashedEmail()") && OSUtils.isValidEmail(str)) {
            Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.8
                @Override // java.lang.Runnable
                public void run() {
                    OneSignalStateSynchronizer.syncHashedEmail(str.trim().toLowerCase());
                }
            };
            if (appContext == null || shouldRunTaskThroughQueue()) {
                Log(LOG_LEVEL.ERROR, "You should initialize OneSignal before calling syncHashedEmail! Moving this operation to a pending task queue.");
                addTaskToQueue(new PendingTaskRunnable(runnable));
            } else {
                runnable.run();
            }
        }
    }

    public static void setEmail(@NonNull String str, EmailUpdateHandler emailUpdateHandler2) {
        setEmail(str, null, emailUpdateHandler2);
    }

    public static void setEmail(@NonNull String str) {
        setEmail(str, null, null);
    }

    public static void setEmail(@NonNull String str, @Nullable String str2) {
        setEmail(str, str2, null);
    }

    public static void setEmail(@NonNull final String str, @Nullable final String str2, @Nullable EmailUpdateHandler emailUpdateHandler2) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("setEmail()")) {
            return;
        }
        if (!OSUtils.isValidEmail(str)) {
            if (emailUpdateHandler2 != null) {
                emailUpdateHandler2.onFailure(new EmailUpdateError(EmailErrorType.VALIDATION, "Email is invalid"));
            }
            Log(LOG_LEVEL.ERROR, "Email is invalid");
            return;
        }
        if (remoteParams != null && remoteParams.useEmailAuth && str2 == null) {
            if (emailUpdateHandler2 != null) {
                emailUpdateHandler2.onFailure(new EmailUpdateError(EmailErrorType.REQUIRES_EMAIL_AUTH, "Email authentication (auth token) is set to REQUIRED for this application. Please provide an auth token from your backend server or change the setting in the OneSignal dashboard."));
            }
            Log(LOG_LEVEL.ERROR, "Email authentication (auth token) is set to REQUIRED for this application. Please provide an auth token from your backend server or change the setting in the OneSignal dashboard.");
            return;
        }
        emailUpdateHandler = emailUpdateHandler2;
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.9
            @Override // java.lang.Runnable
            public void run() {
                String strTrim = str.trim();
                String str3 = str2;
                if (str3 != null) {
                    str3.toLowerCase();
                }
                OneSignal.getCurrentEmailSubscriptionState(OneSignal.appContext).setEmailAddress(strTrim);
                OneSignalStateSynchronizer.setEmail(strTrim.toLowerCase(), str3);
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            Log(LOG_LEVEL.ERROR, "You should initialize OneSignal before calling setEmail! Moving this operation to a pending task queue.");
            addTaskToQueue(new PendingTaskRunnable(runnable));
        } else {
            runnable.run();
        }
    }

    public static void logoutEmail() {
        logoutEmail(null);
    }

    public static void logoutEmail(@Nullable EmailUpdateHandler emailUpdateHandler2) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("logoutEmail()")) {
            return;
        }
        if (getEmailId() == null) {
            if (emailUpdateHandler2 != null) {
                emailUpdateHandler2.onFailure(new EmailUpdateError(EmailErrorType.INVALID_OPERATION, "logoutEmail not valid as email was not set or already logged out!"));
            }
            Log(LOG_LEVEL.ERROR, "logoutEmail not valid as email was not set or already logged out!");
            return;
        }
        emailLogoutHandler = emailUpdateHandler2;
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.10
            @Override // java.lang.Runnable
            public void run() {
                OneSignalStateSynchronizer.logoutEmail();
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            Log(LOG_LEVEL.ERROR, "You should initialize OneSignal before calling logoutEmail! Moving this operation to a pending task queue.");
            addTaskToQueue(new PendingTaskRunnable(runnable));
        } else {
            runnable.run();
        }
    }

    public static void setExternalUserId(final String str) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("setExternalId()")) {
            return;
        }
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.11
            @Override // java.lang.Runnable
            public void run() {
                try {
                    OneSignalStateSynchronizer.setExternalUserId(str);
                } catch (JSONException e) {
                    String str2 = str == "" ? "remove" : "set";
                    OneSignal.onesignalLog(LOG_LEVEL.ERROR, "Attempted to " + str2 + " external ID but encountered a JSON exception");
                    e.printStackTrace();
                }
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            addTaskToQueue(new PendingTaskRunnable(runnable));
        } else {
            runnable.run();
        }
    }

    public static void removeExternalUserId() {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("removeExternalUserId()")) {
            return;
        }
        setExternalUserId("");
    }

    public static void sendTag(String str, String str2) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("sendTag()")) {
            return;
        }
        try {
            sendTags(new JSONObject().put(str, str2));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void sendTags(String str) {
        try {
            sendTags(new JSONObject(str));
        } catch (JSONException e) {
            Log(LOG_LEVEL.ERROR, "Generating JSONObject for sendTags failed!", e);
        }
    }

    public static void sendTags(JSONObject jSONObject) {
        sendTags(jSONObject, null);
    }

    public static void sendTags(final JSONObject jSONObject, final ChangeTagsUpdateHandler changeTagsUpdateHandler) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("sendTags()")) {
            return;
        }
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.12
            @Override // java.lang.Runnable
            public void run() {
                if (jSONObject == null) {
                    if (changeTagsUpdateHandler != null) {
                        changeTagsUpdateHandler.onFailure(new SendTagsError(-1, "Attempted to send null tags"));
                        return;
                    }
                    return;
                }
                JSONObject jSONObject2 = OneSignalStateSynchronizer.getTags(false).result;
                JSONObject jSONObject3 = new JSONObject();
                Iterator<String> itKeys = jSONObject.keys();
                while (itKeys.hasNext()) {
                    String next = itKeys.next();
                    try {
                        Object objOpt = jSONObject.opt(next);
                        if ((objOpt instanceof JSONArray) || (objOpt instanceof JSONObject)) {
                            OneSignal.Log(LOG_LEVEL.ERROR, "Omitting key '" + next + "'! sendTags DO NOT supported nested values!");
                        } else if (jSONObject.isNull(next) || "".equals(objOpt)) {
                            if (jSONObject2 != null && jSONObject2.has(next)) {
                                jSONObject3.put(next, "");
                            }
                        } else {
                            jSONObject3.put(next, objOpt.toString());
                        }
                    } catch (Throwable unused) {
                    }
                }
                if (!jSONObject3.toString().equals("{}")) {
                    OneSignalStateSynchronizer.sendTags(jSONObject3, changeTagsUpdateHandler);
                } else if (changeTagsUpdateHandler != null) {
                    changeTagsUpdateHandler.onSuccess(jSONObject2);
                }
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            Log(LOG_LEVEL.ERROR, "You must initialize OneSignal before modifying tags!Moving this operation to a pending task queue.");
            if (changeTagsUpdateHandler != null) {
                changeTagsUpdateHandler.onFailure(new SendTagsError(-1, "You must initialize OneSignal before modifying tags!Moving this operation to a pending task queue."));
            }
            addTaskToQueue(new PendingTaskRunnable(runnable));
            return;
        }
        runnable.run();
    }

    public static void postNotification(String str, PostNotificationResponseHandler postNotificationResponseHandler) {
        try {
            postNotification(new JSONObject(str), postNotificationResponseHandler);
        } catch (JSONException unused) {
            Log(LOG_LEVEL.ERROR, "Invalid postNotification JSON format: " + str);
        }
    }

    public static void postNotification(JSONObject jSONObject, final PostNotificationResponseHandler postNotificationResponseHandler) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("postNotification()")) {
            return;
        }
        try {
            if (!jSONObject.has("app_id")) {
                jSONObject.put("app_id", getSavedAppId());
            }
            OneSignalRestClient.post("notifications/", jSONObject, new OneSignalRestClient.ResponseHandler() { // from class: com.onesignal.OneSignal.13
                @Override // com.onesignal.OneSignalRestClient.ResponseHandler
                public void onSuccess(String str) {
                    LOG_LEVEL log_level = LOG_LEVEL.DEBUG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("HTTP create notification success: ");
                    sb.append(str != null ? str : "null");
                    OneSignal.Log(log_level, sb.toString());
                    if (postNotificationResponseHandler != null) {
                        try {
                            JSONObject jSONObject2 = new JSONObject(str);
                            if (jSONObject2.has("errors")) {
                                postNotificationResponseHandler.onFailure(jSONObject2);
                            } else {
                                postNotificationResponseHandler.onSuccess(new JSONObject(str));
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                }

                @Override // com.onesignal.OneSignalRestClient.ResponseHandler
                void onFailure(int i, String str, Throwable th) {
                    OneSignal.logHttpError("create notification failed", i, th, str);
                    if (postNotificationResponseHandler != null) {
                        if (i == 0) {
                            str = "{\"error\": \"HTTP no response error\"}";
                        }
                        try {
                            try {
                                postNotificationResponseHandler.onFailure(new JSONObject(str));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } catch (Throwable unused) {
                            postNotificationResponseHandler.onFailure(new JSONObject("{\"error\": \"Unknown response!\"}"));
                        }
                    }
                }
            });
        } catch (JSONException e) {
            Log(LOG_LEVEL.ERROR, "HTTP create notification json exception!", e);
            if (postNotificationResponseHandler != null) {
                try {
                    postNotificationResponseHandler.onFailure(new JSONObject("{'error': 'HTTP create notification json exception!'}"));
                } catch (JSONException e2) {
                    e2.printStackTrace();
                }
            }
        }
    }

    public static void getTags(final GetTagsHandler getTagsHandler) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("getTags()")) {
            return;
        }
        synchronized (pendingGetTagsHandlers) {
            pendingGetTagsHandlers.add(getTagsHandler);
            if (pendingGetTagsHandlers.size() > 1) {
                return;
            }
            Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.14
                @Override // java.lang.Runnable
                public void run() {
                    if (getTagsHandler == null) {
                        OneSignal.Log(LOG_LEVEL.ERROR, "getTagsHandler is null!");
                    } else {
                        if (OneSignal.getUserId() == null) {
                            return;
                        }
                        OneSignal.internalFireGetTagsCallbacks();
                    }
                }
            };
            if (appContext == null) {
                Log(LOG_LEVEL.ERROR, "You must initialize OneSignal before getting tags! Moving this tag operation to a pending queue.");
                taskQueueWaitingForInit.add(runnable);
            } else {
                runnable.run();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void internalFireGetTagsCallbacks() {
        synchronized (pendingGetTagsHandlers) {
            if (pendingGetTagsHandlers.size() == 0) {
                return;
            }
            new Thread(new Runnable() { // from class: com.onesignal.OneSignal.15
                @Override // java.lang.Runnable
                public void run() {
                    UserStateSynchronizer.GetTagsResult tags = OneSignalStateSynchronizer.getTags(!OneSignal.getTagsCall);
                    if (tags.serverSuccess) {
                        boolean unused = OneSignal.getTagsCall = true;
                    }
                    synchronized (OneSignal.pendingGetTagsHandlers) {
                        for (GetTagsHandler getTagsHandler : OneSignal.pendingGetTagsHandlers) {
                            if (tags.result == null || tags.toString().equals("{}")) {
                                getTagsHandler.tagsAvailable(null);
                            } else {
                                getTagsHandler.tagsAvailable(tags.result);
                            }
                        }
                        OneSignal.pendingGetTagsHandlers.clear();
                    }
                }
            }, "OS_GETTAGS_CALLBACK").start();
        }
    }

    public static void deleteTag(String str) {
        deleteTag(str, null);
    }

    public static void deleteTag(String str, ChangeTagsUpdateHandler changeTagsUpdateHandler) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("deleteTag()")) {
            return;
        }
        ArrayList arrayList = new ArrayList(1);
        arrayList.add(str);
        deleteTags(arrayList, changeTagsUpdateHandler);
    }

    public static void deleteTags(Collection<String> collection) {
        deleteTags(collection, (ChangeTagsUpdateHandler) null);
    }

    public static void deleteTags(Collection<String> collection, ChangeTagsUpdateHandler changeTagsUpdateHandler) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("deleteTags()")) {
            return;
        }
        try {
            JSONObject jSONObject = new JSONObject();
            Iterator<String> it = collection.iterator();
            while (it.hasNext()) {
                jSONObject.put(it.next(), "");
            }
            sendTags(jSONObject, changeTagsUpdateHandler);
        } catch (Throwable th) {
            Log(LOG_LEVEL.ERROR, "Failed to generate JSON for deleteTags.", th);
        }
    }

    public static void deleteTags(String str) {
        deleteTags(str, (ChangeTagsUpdateHandler) null);
    }

    public static void deleteTags(String str, ChangeTagsUpdateHandler changeTagsUpdateHandler) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("deleteTags()")) {
            return;
        }
        try {
            JSONObject jSONObject = new JSONObject();
            JSONArray jSONArray = new JSONArray(str);
            for (int i = 0; i < jSONArray.length(); i++) {
                jSONObject.put(jSONArray.getString(i), "");
            }
            sendTags(jSONObject, changeTagsUpdateHandler);
        } catch (Throwable th) {
            Log(LOG_LEVEL.ERROR, "Failed to generate JSON for deleteTags.", th);
        }
    }

    public static void idsAvailable(IdsAvailableHandler idsAvailableHandler2) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("idsAvailable()")) {
            return;
        }
        idsAvailableHandler = idsAvailableHandler2;
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.16
            @Override // java.lang.Runnable
            public void run() {
                if (OneSignal.getUserId() != null) {
                    OSUtils.runOnMainUIThread(new Runnable() { // from class: com.onesignal.OneSignal.16.1
                        @Override // java.lang.Runnable
                        public void run() {
                            OneSignal.internalFireIdsAvailableCallback();
                        }
                    });
                }
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            Log(LOG_LEVEL.ERROR, "You must initialize OneSignal before getting tags! Moving this tag operation to a pending queue.");
            addTaskToQueue(new PendingTaskRunnable(runnable));
        } else {
            runnable.run();
        }
    }

    static void fireIdsAvailableCallback() {
        if (idsAvailableHandler != null) {
            OSUtils.runOnMainUIThread(new Runnable() { // from class: com.onesignal.OneSignal.17
                @Override // java.lang.Runnable
                public void run() {
                    OneSignal.internalFireIdsAvailableCallback();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static synchronized void internalFireIdsAvailableCallback() {
        if (idsAvailableHandler == null) {
            return;
        }
        String registrationId = OneSignalStateSynchronizer.getRegistrationId();
        if (!OneSignalStateSynchronizer.getSubscribed()) {
            registrationId = null;
        }
        String userId2 = getUserId();
        if (userId2 == null) {
            return;
        }
        idsAvailableHandler.idsAvailable(userId2, registrationId);
        if (registrationId != null) {
            idsAvailableHandler = null;
        }
    }

    static void sendPurchases(JSONArray jSONArray, boolean z, OneSignalRestClient.ResponseHandler responseHandler) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("sendPurchases()")) {
            return;
        }
        if (getUserId() == null) {
            iapUpdateJob = new IAPUpdateJob(jSONArray);
            iapUpdateJob.newAsExisting = z;
            iapUpdateJob.restResponseHandler = responseHandler;
            return;
        }
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("app_id", appId);
            if (z) {
                jSONObject.put("existing", true);
            }
            jSONObject.put("purchases", jSONArray);
            OneSignalRestClient.post("players/" + getUserId() + "/on_purchase", jSONObject, responseHandler);
            if (getEmailId() != null) {
                OneSignalRestClient.post("players/" + getEmailId() + "/on_purchase", jSONObject, null);
            }
        } catch (Throwable th) {
            Log(LOG_LEVEL.ERROR, "Failed to generate JSON for sendPurchases.", th);
        }
    }

    private static boolean openURLFromNotification(Context context, JSONArray jSONArray) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName(null)) {
            return false;
        }
        int length = jSONArray.length();
        boolean z = false;
        for (int i = 0; i < length; i++) {
            try {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                if (jSONObject.has("custom")) {
                    JSONObject jSONObject2 = new JSONObject(jSONObject.optString("custom"));
                    if (jSONObject2.has("u")) {
                        String strOptString = jSONObject2.optString("u", null);
                        if (!strOptString.contains("://")) {
                            strOptString = "http://" + strOptString;
                        }
                        Intent intent = new Intent("android.intent.action.VIEW", Uri.parse(strOptString.trim()));
                        intent.addFlags(1476919296);
                        context.startActivity(intent);
                        z = true;
                    }
                }
            } catch (Throwable th) {
                Log(LOG_LEVEL.ERROR, "Error parsing JSON item " + i + "/" + length + " for launching a web URL.", th);
            }
        }
        return z;
    }

    private static void runNotificationOpenedCallback(JSONArray jSONArray, boolean z, boolean z2) {
        if (mInitBuilder == null || mInitBuilder.mNotificationOpenedHandler == null) {
            unprocessedOpenedNotifis.add(jSONArray);
        } else {
            fireNotificationOpenedHandler(generateOsNotificationOpenResult(jSONArray, z, z2));
        }
    }

    @NonNull
    private static OSNotificationOpenResult generateOsNotificationOpenResult(JSONArray jSONArray, boolean z, boolean z2) {
        int length = jSONArray.length();
        OSNotificationOpenResult oSNotificationOpenResult = new OSNotificationOpenResult();
        OSNotification oSNotification = new OSNotification();
        oSNotification.isAppInFocus = isAppActive();
        oSNotification.shown = z;
        oSNotification.androidNotificationId = jSONArray.optJSONObject(0).optInt("notificationId");
        String strOptString = null;
        boolean z3 = true;
        for (int i = 0; i < length; i++) {
            try {
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                oSNotification.payload = NotificationBundleProcessor.OSNotificationPayloadFrom(jSONObject);
                if (strOptString == null && jSONObject.has("actionSelected")) {
                    strOptString = jSONObject.optString("actionSelected", null);
                }
                if (z3) {
                    z3 = false;
                } else {
                    if (oSNotification.groupedNotifications == null) {
                        oSNotification.groupedNotifications = new ArrayList();
                    }
                    oSNotification.groupedNotifications.add(oSNotification.payload);
                }
            } catch (Throwable th) {
                Log(LOG_LEVEL.ERROR, "Error parsing JSON item " + i + "/" + length + " for callback.", th);
            }
        }
        oSNotificationOpenResult.notification = oSNotification;
        oSNotificationOpenResult.action = new OSNotificationAction();
        oSNotificationOpenResult.action.actionID = strOptString;
        oSNotificationOpenResult.action.type = strOptString != null ? OSNotificationAction.ActionType.ActionTaken : OSNotificationAction.ActionType.Opened;
        if (z2) {
            oSNotificationOpenResult.notification.displayType = OSNotification.DisplayType.InAppAlert;
        } else {
            oSNotificationOpenResult.notification.displayType = OSNotification.DisplayType.Notification;
        }
        return oSNotificationOpenResult;
    }

    private static void fireNotificationOpenedHandler(final OSNotificationOpenResult oSNotificationOpenResult) {
        OSUtils.runOnMainUIThread(new Runnable() { // from class: com.onesignal.OneSignal.18
            @Override // java.lang.Runnable
            public void run() {
                OneSignal.mInitBuilder.mNotificationOpenedHandler.notificationOpened(oSNotificationOpenResult);
            }
        });
    }

    static void handleNotificationReceived(JSONArray jSONArray, boolean z, boolean z2) {
        OSNotificationOpenResult oSNotificationOpenResultGenerateOsNotificationOpenResult = generateOsNotificationOpenResult(jSONArray, z, z2);
        if (trackFirebaseAnalytics != null && getFirebaseAnalyticsEnabled(appContext)) {
            trackFirebaseAnalytics.trackReceivedEvent(oSNotificationOpenResultGenerateOsNotificationOpenResult);
        }
        if (mInitBuilder == null || mInitBuilder.mNotificationReceivedHandler == null) {
            return;
        }
        mInitBuilder.mNotificationReceivedHandler.notificationReceived(oSNotificationOpenResultGenerateOsNotificationOpenResult.notification);
    }

    public static void handleNotificationOpen(Context context, JSONArray jSONArray, boolean z) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName(null)) {
            return;
        }
        notificationOpenedRESTCall(context, jSONArray);
        if (trackFirebaseAnalytics != null && getFirebaseAnalyticsEnabled(appContext)) {
            trackFirebaseAnalytics.trackOpenedEvent(generateOsNotificationOpenResult(jSONArray, true, z));
        }
        boolean zEquals = "DISABLE".equals(OSUtils.getManifestMeta(context, "com.onesignal.NotificationOpened.DEFAULT"));
        boolean zOpenURLFromNotification = zEquals ? false : openURLFromNotification(context, jSONArray);
        runNotificationOpenedCallback(jSONArray, true, z);
        if (z || zOpenURLFromNotification || zEquals) {
            return;
        }
        fireIntentFromNotificationOpen(context);
    }

    private static void fireIntentFromNotificationOpen(Context context) {
        Intent launchIntentForPackage;
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName(null) || (launchIntentForPackage = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName())) == null) {
            return;
        }
        launchIntentForPackage.setFlags(268566528);
        context.startActivity(launchIntentForPackage);
    }

    private static void notificationOpenedRESTCall(Context context, JSONArray jSONArray) {
        for (int i = 0; i < jSONArray.length(); i++) {
            try {
                String strOptString = new JSONObject(jSONArray.getJSONObject(i).optString("custom", null)).optString("i", null);
                if (!postedOpenedNotifIds.contains(strOptString)) {
                    postedOpenedNotifIds.add(strOptString);
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.put("app_id", getSavedAppId(context));
                    jSONObject.put("player_id", getSavedUserId(context));
                    jSONObject.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_OPENED, true);
                    OneSignalRestClient.put("notifications/" + strOptString, jSONObject, new OneSignalRestClient.ResponseHandler() { // from class: com.onesignal.OneSignal.19
                        @Override // com.onesignal.OneSignalRestClient.ResponseHandler
                        void onFailure(int i2, String str, Throwable th) {
                            OneSignal.logHttpError("sending Notification Opened Failed", i2, th, str);
                        }
                    });
                }
            } catch (Throwable th) {
                Log(LOG_LEVEL.ERROR, "Failed to generate JSON to send notification opened.", th);
            }
        }
    }

    private static void SaveAppId(String str) {
        if (appContext == null) {
            return;
        }
        OneSignalPrefs.saveString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_APP_ID, str);
    }

    static String getSavedAppId() {
        return getSavedAppId(appContext);
    }

    private static String getSavedAppId(Context context) {
        return context == null ? "" : OneSignalPrefs.getString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_APP_ID, null);
    }

    static boolean getSavedUserConsentStatus() {
        return OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_USER_PROVIDED_CONSENT, false);
    }

    static void saveUserConsentStatus(boolean z) {
        OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_ONESIGNAL_USER_PROVIDED_CONSENT, z);
    }

    private static String getSavedUserId(Context context) {
        return context == null ? "" : OneSignalPrefs.getString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_PLAYER_ID, null);
    }

    static String getUserId() {
        if (userId == null && appContext != null) {
            userId = OneSignalPrefs.getString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_PLAYER_ID, null);
        }
        return userId;
    }

    static void saveUserId(String str) {
        userId = str;
        if (appContext == null) {
            return;
        }
        OneSignalPrefs.saveString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_PLAYER_ID, userId);
    }

    static String getEmailId() {
        if ("".equals(emailId)) {
            return null;
        }
        if (emailId == null && appContext != null) {
            emailId = OneSignalPrefs.getString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_EMAIL_ID, null);
        }
        return emailId;
    }

    static void saveEmailId(String str) {
        emailId = str;
        if (appContext == null) {
            return;
        }
        OneSignalPrefs.saveString(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_EMAIL_ID, "".equals(emailId) ? null : emailId);
    }

    static boolean getFilterOtherGCMReceivers(Context context) {
        return OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_FILTER_OTHER_GCM_RECEIVERS, false);
    }

    static void saveFilterOtherGCMReceivers(boolean z) {
        if (appContext == null) {
            return;
        }
        OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_FILTER_OTHER_GCM_RECEIVERS, z);
    }

    static void updateUserIdDependents(String str) {
        saveUserId(str);
        fireIdsAvailableCallback();
        internalFireGetTagsCallbacks();
        getCurrentSubscriptionState(appContext).setUserId(str);
        if (iapUpdateJob != null) {
            sendPurchases(iapUpdateJob.toReport, iapUpdateJob.newAsExisting, iapUpdateJob.restResponseHandler);
            iapUpdateJob = null;
        }
        OneSignalStateSynchronizer.refreshEmailState();
        OneSignalChromeTab.setup(appContext, appId, str, AdvertisingIdProviderGPS.getLastValue());
    }

    static void updateEmailIdDependents(String str) {
        saveEmailId(str);
        getCurrentEmailSubscriptionState(appContext).setEmailUserId(str);
        try {
            OneSignalStateSynchronizer.updatePushState(new JSONObject().put("parent_player_id", str));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static boolean getFirebaseAnalyticsEnabled(Context context) {
        return OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_FIREBASE_TRACKING_ENABLED, false);
    }

    public static void enableVibrate(boolean z) {
        if (appContext == null) {
            return;
        }
        OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_VIBRATE_ENABLED, z);
    }

    static boolean getVibrate(Context context) {
        return OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_VIBRATE_ENABLED, true);
    }

    public static void enableSound(boolean z) {
        if (appContext == null) {
            return;
        }
        OneSignalPrefs.saveBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_SOUND_ENABLED, z);
    }

    static boolean getSoundEnabled(Context context) {
        return OneSignalPrefs.getBool(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_SOUND_ENABLED, true);
    }

    static void setLastSessionTime(long j) {
        OneSignalPrefs.saveLong(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_LAST_SESSION_TIME, j);
    }

    private static long getLastSessionTime(Context context) {
        return OneSignalPrefs.getLong(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_LAST_SESSION_TIME, -31000L);
    }

    public static void setInFocusDisplaying(OSInFocusDisplayOption oSInFocusDisplayOption) {
        getCurrentOrNewInitBuilder().mDisplayOptionCarryOver = true;
        getCurrentOrNewInitBuilder().mDisplayOption = oSInFocusDisplayOption;
    }

    public static void setInFocusDisplaying(int i) {
        setInFocusDisplaying(getInFocusDisplaying(i));
    }

    public static OSInFocusDisplayOption currentInFocusDisplayOption() {
        return getCurrentOrNewInitBuilder().mDisplayOption;
    }

    private static OSInFocusDisplayOption getInFocusDisplaying(int i) {
        switch (i) {
            case 0:
                return OSInFocusDisplayOption.None;
            case 1:
                return OSInFocusDisplayOption.InAppAlert;
            case 2:
                return OSInFocusDisplayOption.Notification;
            default:
                if (i < 0) {
                    return OSInFocusDisplayOption.None;
                }
                return OSInFocusDisplayOption.Notification;
        }
    }

    static boolean getNotificationsWhenActiveEnabled() {
        return mInitBuilder == null || mInitBuilder.mDisplayOption == OSInFocusDisplayOption.Notification;
    }

    static boolean getInAppAlertNotificationEnabled() {
        return mInitBuilder != null && mInitBuilder.mDisplayOption == OSInFocusDisplayOption.InAppAlert;
    }

    public static void setSubscription(final boolean z) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("setSubscription()")) {
            return;
        }
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.20
            @Override // java.lang.Runnable
            public void run() {
                OneSignal.getCurrentSubscriptionState(OneSignal.appContext).setUserSubscriptionSetting(z);
                OneSignalStateSynchronizer.setSubscription(z);
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Moving subscription action to a waiting task queue.");
            addTaskToQueue(new PendingTaskRunnable(runnable));
        } else {
            runnable.run();
        }
    }

    public static void setLocationShared(boolean z) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("setLocationShared()")) {
            return;
        }
        shareLocation = z;
        if (!z) {
            OneSignalStateSynchronizer.clearLocation();
        }
        Log(LOG_LEVEL.DEBUG, "shareLocation:" + shareLocation);
    }

    public static void promptLocation() {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("promptLocation()")) {
            return;
        }
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.21
            @Override // java.lang.Runnable
            public void run() {
                LocationGMS.getLocation(OneSignal.appContext, true, new LocationGMS.LocationHandler() { // from class: com.onesignal.OneSignal.21.1
                    @Override // com.onesignal.LocationGMS.LocationHandler
                    public LocationGMS.CALLBACK_TYPE getType() {
                        return LocationGMS.CALLBACK_TYPE.PROMPT_LOCATION;
                    }

                    @Override // com.onesignal.LocationGMS.LocationHandler
                    public void complete(LocationGMS.LocationPoint locationPoint) {
                        if (OneSignal.shouldLogUserPrivacyConsentErrorMessageForMethodName("promptLocation()") || locationPoint == null) {
                            return;
                        }
                        OneSignalStateSynchronizer.updateLocation(locationPoint);
                    }
                });
                boolean unused = OneSignal.promptedLocation = true;
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not prompt for location at this time - moving this operation to awaiting queue.");
            addTaskToQueue(new PendingTaskRunnable(runnable));
        } else {
            runnable.run();
        }
    }

    public static void clearOneSignalNotifications() {
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.22
            @Override // java.lang.Runnable
            public void run() throws Throwable {
                Cursor cursorQuery;
                SQLiteDatabase writableDbWithRetries;
                LOG_LEVEL log_level;
                String str;
                NotificationManager notificationManager = (NotificationManager) OneSignal.appContext.getSystemService(OneSignalDbContract.NotificationTable.TABLE_NAME);
                OneSignalDbHelper oneSignalDbHelper = OneSignalDbHelper.getInstance(OneSignal.appContext);
                Cursor cursor = null;
                sQLiteDatabase = null;
                SQLiteDatabase sQLiteDatabase = null;
                cursor = null;
                try {
                    try {
                        cursorQuery = oneSignalDbHelper.getReadableDbWithRetries().query(OneSignalDbContract.NotificationTable.TABLE_NAME, new String[]{OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID}, "dismissed = 0 AND opened = 0", null, null, null, null);
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        try {
                            if (cursorQuery.moveToFirst()) {
                                do {
                                    notificationManager.cancel(cursorQuery.getInt(cursorQuery.getColumnIndex(OneSignalDbContract.NotificationTable.COLUMN_NAME_ANDROID_NOTIFICATION_ID)));
                                } while (cursorQuery.moveToNext());
                            }
                            try {
                                try {
                                    writableDbWithRetries = oneSignalDbHelper.getWritableDbWithRetries();
                                } catch (Throwable th2) {
                                    th = th2;
                                    writableDbWithRetries = sQLiteDatabase;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                            }
                            try {
                                writableDbWithRetries.beginTransaction();
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(OneSignalDbContract.NotificationTable.COLUMN_NAME_DISMISSED, (Integer) 1);
                                writableDbWithRetries.update(OneSignalDbContract.NotificationTable.TABLE_NAME, contentValues, "opened = 0", null);
                                writableDbWithRetries.setTransactionSuccessful();
                                if (writableDbWithRetries != null) {
                                    try {
                                        writableDbWithRetries.endTransaction();
                                    } catch (Throwable th4) {
                                        th = th4;
                                        log_level = LOG_LEVEL.ERROR;
                                        str = "Error closing transaction! ";
                                        OneSignal.Log(log_level, str, th);
                                    }
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                if (writableDbWithRetries != null) {
                                    try {
                                        writableDbWithRetries.endTransaction();
                                    } catch (Throwable th6) {
                                        OneSignal.Log(LOG_LEVEL.ERROR, "Error closing transaction! ", th6);
                                    }
                                }
                                throw th;
                            }
                            BadgeCountUpdater.updateCount(0, OneSignal.appContext);
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            if (cursorQuery != null) {
                                cursorQuery.close();
                            }
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        cursor = cursorQuery;
                        OneSignal.Log(LOG_LEVEL.ERROR, "Error canceling all notifications! ", th);
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                } catch (Throwable th9) {
                    th = th9;
                    cursorQuery = cursor;
                }
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not clear notifications at this time - moving this operation toa waiting task queue.");
            addTaskToQueue(new PendingTaskRunnable(runnable));
        } else {
            runnable.run();
        }
    }

    public static void cancelNotification(final int i) {
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.23
            /* JADX WARN: Multi-variable type inference failed */
            /* JADX WARN: Removed duplicated region for block: B:34:0x00ae A[EXC_TOP_SPLITTER, SYNTHETIC] */
            /* JADX WARN: Type inference failed for: r0v1, types: [com.onesignal.OneSignalDbHelper] */
            /* JADX WARN: Type inference failed for: r0v6 */
            @Override // java.lang.Runnable
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct add '--show-bad-code' argument
            */
            public void run() throws java.lang.Throwable {
                /*
                    r7 = this;
                    android.content.Context r0 = com.onesignal.OneSignal.appContext
                    com.onesignal.OneSignalDbHelper r0 = com.onesignal.OneSignalDbHelper.getInstance(r0)
                    r1 = 0
                    android.database.sqlite.SQLiteDatabase r0 = r0.getWritableDbWithRetries()     // Catch: java.lang.Throwable -> L6a java.lang.Throwable -> L6d
                    r0.beginTransaction()     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.StringBuilder r2 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    r2.<init>()     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.String r3 = "android_notification_id = "
                    r2.append(r3)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    int r3 = r1     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    r2.append(r3)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.String r3 = " AND "
                    r2.append(r3)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.String r3 = "opened"
                    r2.append(r3)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.String r3 = " = 0 AND "
                    r2.append(r3)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.String r3 = "dismissed"
                    r2.append(r3)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.String r3 = " = 0"
                    r2.append(r3)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.String r2 = r2.toString()     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    android.content.ContentValues r3 = new android.content.ContentValues     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    r3.<init>()     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.String r4 = "dismissed"
                    r5 = 1
                    java.lang.Integer r5 = java.lang.Integer.valueOf(r5)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    r3.put(r4, r5)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    java.lang.String r4 = "notification"
                    int r1 = r0.update(r4, r3, r2, r1)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    if (r1 <= 0) goto L58
                    android.content.Context r1 = com.onesignal.OneSignal.appContext     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    int r2 = r1     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    com.onesignal.NotificationSummaryManager.updatePossibleDependentSummaryOnDismiss(r1, r0, r2)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                L58:
                    android.content.Context r1 = com.onesignal.OneSignal.appContext     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    com.onesignal.BadgeCountUpdater.update(r0, r1)     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    r0.setTransactionSuccessful()     // Catch: java.lang.Throwable -> L66 java.lang.Throwable -> L68
                    if (r0 == 0) goto L9c
                    r0.endTransaction()     // Catch: java.lang.Throwable -> L94
                    goto L9c
                L66:
                    r7 = move-exception
                    goto Lac
                L68:
                    r1 = move-exception
                    goto L71
                L6a:
                    r7 = move-exception
                    r0 = r1
                    goto Lac
                L6d:
                    r0 = move-exception
                    r6 = r1
                    r1 = r0
                    r0 = r6
                L71:
                    com.onesignal.OneSignal$LOG_LEVEL r2 = com.onesignal.OneSignal.LOG_LEVEL.ERROR     // Catch: java.lang.Throwable -> L66
                    java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L66
                    r3.<init>()     // Catch: java.lang.Throwable -> L66
                    java.lang.String r4 = "Error marking a notification id "
                    r3.append(r4)     // Catch: java.lang.Throwable -> L66
                    int r4 = r1     // Catch: java.lang.Throwable -> L66
                    r3.append(r4)     // Catch: java.lang.Throwable -> L66
                    java.lang.String r4 = " as dismissed! "
                    r3.append(r4)     // Catch: java.lang.Throwable -> L66
                    java.lang.String r3 = r3.toString()     // Catch: java.lang.Throwable -> L66
                    com.onesignal.OneSignal.Log(r2, r3, r1)     // Catch: java.lang.Throwable -> L66
                    if (r0 == 0) goto L9c
                    r0.endTransaction()     // Catch: java.lang.Throwable -> L94
                    goto L9c
                L94:
                    r0 = move-exception
                    com.onesignal.OneSignal$LOG_LEVEL r1 = com.onesignal.OneSignal.LOG_LEVEL.ERROR
                    java.lang.String r2 = "Error closing transaction! "
                    com.onesignal.OneSignal.Log(r1, r2, r0)
                L9c:
                    android.content.Context r0 = com.onesignal.OneSignal.appContext
                    java.lang.String r1 = "notification"
                    java.lang.Object r0 = r0.getSystemService(r1)
                    android.app.NotificationManager r0 = (android.app.NotificationManager) r0
                    int r7 = r1
                    r0.cancel(r7)
                    return
                Lac:
                    if (r0 == 0) goto Lba
                    r0.endTransaction()     // Catch: java.lang.Throwable -> Lb2
                    goto Lba
                Lb2:
                    r0 = move-exception
                    com.onesignal.OneSignal$LOG_LEVEL r1 = com.onesignal.OneSignal.LOG_LEVEL.ERROR
                    java.lang.String r2 = "Error closing transaction! "
                    com.onesignal.OneSignal.Log(r1, r2, r0)
                Lba:
                    throw r7
                */
                throw new UnsupportedOperationException("Method not decompiled: com.onesignal.OneSignal.AnonymousClass23.run():void");
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not clear notification id: " + i + " at this time - movingthis operation to a waiting task queue. The notification will still be canceledfrom NotificationManager at this time.");
            taskQueueWaitingForInit.add(runnable);
            return;
        }
        runnable.run();
    }

    public static void cancelGroupedNotifications(final String str) {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("cancelGroupedNotifications()")) {
            return;
        }
        Runnable runnable = new Runnable() { // from class: com.onesignal.OneSignal.24
            /* JADX WARN: Removed duplicated region for block: B:28:0x00a5 A[Catch: Throwable -> 0x00d5, TRY_ENTER, TRY_LEAVE, TryCatch #5 {Throwable -> 0x00d5, blocks: (B:28:0x00a5, B:39:0x00d1), top: B:58:0x0079 }] */
            /* JADX WARN: Removed duplicated region for block: B:76:? A[RETURN, SYNTHETIC] */
            @Override // java.lang.Runnable
            /*
                Code decompiled incorrectly, please refer to instructions dump.
                To view partially-correct add '--show-bad-code' argument
            */
            public void run() throws java.lang.Throwable {
                /*
                    Method dump skipped, instruction units count: 250
                    To view this dump add '--comments-level debug' option
                */
                throw new UnsupportedOperationException("Method not decompiled: com.onesignal.OneSignal.AnonymousClass24.run():void");
            }
        };
        if (appContext == null || shouldRunTaskThroughQueue()) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not clear notifications part of group " + str + " - movingthis operation to a waiting task queue.");
            addTaskToQueue(new PendingTaskRunnable(runnable));
            return;
        }
        runnable.run();
    }

    public static void removeNotificationOpenedHandler() {
        getCurrentOrNewInitBuilder().mNotificationOpenedHandler = null;
    }

    public static void removeNotificationReceivedHandler() {
        getCurrentOrNewInitBuilder().mNotificationReceivedHandler = null;
    }

    public static void addPermissionObserver(OSPermissionObserver oSPermissionObserver) {
        if (appContext == null) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not add permission observer");
            return;
        }
        getPermissionStateChangesObserver().addObserver(oSPermissionObserver);
        if (getCurrentPermissionState(appContext).compare(getLastPermissionState(appContext))) {
            OSPermissionChangedInternalObserver.fireChangesToPublicObserver(getCurrentPermissionState(appContext));
        }
    }

    public static void removePermissionObserver(OSPermissionObserver oSPermissionObserver) {
        if (appContext == null) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not modify permission observer");
        } else {
            getPermissionStateChangesObserver().removeObserver(oSPermissionObserver);
        }
    }

    public static void addSubscriptionObserver(OSSubscriptionObserver oSSubscriptionObserver) {
        if (appContext == null) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not add subscription observer");
            return;
        }
        getSubscriptionStateChangesObserver().addObserver(oSSubscriptionObserver);
        if (getCurrentSubscriptionState(appContext).compare(getLastSubscriptionState(appContext))) {
            OSSubscriptionChangedInternalObserver.fireChangesToPublicObserver(getCurrentSubscriptionState(appContext));
        }
    }

    public static void removeSubscriptionObserver(OSSubscriptionObserver oSSubscriptionObserver) {
        if (appContext == null) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not modify subscription observer");
        } else {
            getSubscriptionStateChangesObserver().removeObserver(oSSubscriptionObserver);
        }
    }

    public static void addEmailSubscriptionObserver(@NonNull OSEmailSubscriptionObserver oSEmailSubscriptionObserver) {
        if (appContext == null) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not add email subscription observer");
            return;
        }
        getEmailSubscriptionStateChangesObserver().addObserver(oSEmailSubscriptionObserver);
        if (getCurrentEmailSubscriptionState(appContext).compare(getLastEmailSubscriptionState(appContext))) {
            OSEmailSubscriptionChangedInternalObserver.fireChangesToPublicObserver(getCurrentEmailSubscriptionState(appContext));
        }
    }

    public static void removeEmailSubscriptionObserver(@NonNull OSEmailSubscriptionObserver oSEmailSubscriptionObserver) {
        if (appContext == null) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not modify email subscription observer");
        } else {
            getEmailSubscriptionStateChangesObserver().removeObserver(oSEmailSubscriptionObserver);
        }
    }

    public static OSPermissionSubscriptionState getPermissionSubscriptionState() {
        if (shouldLogUserPrivacyConsentErrorMessageForMethodName("getPermissionSubscriptionState()")) {
            return null;
        }
        if (appContext == null) {
            Log(LOG_LEVEL.ERROR, "OneSignal.init has not been called. Could not get OSPermissionSubscriptionState");
            return null;
        }
        OSPermissionSubscriptionState oSPermissionSubscriptionState = new OSPermissionSubscriptionState();
        oSPermissionSubscriptionState.subscriptionStatus = getCurrentSubscriptionState(appContext);
        oSPermissionSubscriptionState.permissionStatus = getCurrentPermissionState(appContext);
        oSPermissionSubscriptionState.emailSubscriptionStatus = getCurrentEmailSubscriptionState(appContext);
        return oSPermissionSubscriptionState;
    }

    static long GetUnsentActiveTime() {
        if (unSentActiveTime == -1 && appContext != null) {
            unSentActiveTime = OneSignalPrefs.getLong(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_UNSENT_ACTIVE_TIME, 0L);
        }
        Log(LOG_LEVEL.INFO, "GetUnsentActiveTime: " + unSentActiveTime);
        return unSentActiveTime;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void SaveUnsentActiveTime(long j) {
        unSentActiveTime = j;
        if (appContext == null) {
            return;
        }
        Log(LOG_LEVEL.INFO, "SaveUnsentActiveTime: " + unSentActiveTime);
        OneSignalPrefs.saveLong(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_GT_UNSENT_ACTIVE_TIME, j);
    }

    private static boolean isDuplicateNotification(String str, Context context) throws Throwable {
        boolean zMoveToFirst;
        Cursor cursorQuery;
        if (str == null || "".equals(str)) {
            return false;
        }
        Cursor cursor = null;
        try {
            try {
                cursorQuery = OneSignalDbHelper.getInstance(context).getReadableDbWithRetries().query(OneSignalDbContract.NotificationTable.TABLE_NAME, new String[]{OneSignalDbContract.NotificationTable.COLUMN_NAME_NOTIFICATION_ID}, "notification_id = ?", new String[]{str}, null, null, null);
            } catch (Throwable th) {
                th = th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
        try {
            zMoveToFirst = cursorQuery.moveToFirst();
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (Throwable th3) {
            cursor = cursorQuery;
            th = th3;
            Log(LOG_LEVEL.ERROR, "Could not check for duplicate, assuming unique.", th);
            if (cursor != null) {
                cursor.close();
            }
            zMoveToFirst = false;
        }
        if (!zMoveToFirst) {
            return false;
        }
        Log(LOG_LEVEL.DEBUG, "Duplicate GCM message received, skip processing of " + str);
        return true;
    }

    static boolean notValidOrDuplicated(Context context, JSONObject jSONObject) {
        String notificationIdFromGCMJsonPayload = getNotificationIdFromGCMJsonPayload(jSONObject);
        return notificationIdFromGCMJsonPayload == null || isDuplicateNotification(notificationIdFromGCMJsonPayload, context);
    }

    static String getNotificationIdFromGCMBundle(Bundle bundle) {
        if (bundle.isEmpty()) {
            return null;
        }
        try {
            if (bundle.containsKey("custom")) {
                JSONObject jSONObject = new JSONObject(bundle.getString("custom"));
                if (jSONObject.has("i")) {
                    return jSONObject.optString("i", null);
                }
                Log(LOG_LEVEL.DEBUG, "Not a OneSignal formatted GCM message. No 'i' field in custom.");
            } else {
                Log(LOG_LEVEL.DEBUG, "Not a OneSignal formatted GCM message. No 'custom' field in the bundle.");
            }
        } catch (Throwable th) {
            Log(LOG_LEVEL.DEBUG, "Could not parse bundle, probably not a OneSignal notification.", th);
        }
        return null;
    }

    private static String getNotificationIdFromGCMJsonPayload(JSONObject jSONObject) {
        try {
            return new JSONObject(jSONObject.optString("custom")).optString("i", null);
        } catch (Throwable unused) {
            return null;
        }
    }

    static boolean isAppActive() {
        return initDone && isForeground();
    }

    private static boolean isPastOnSessionTime() {
        return (System.currentTimeMillis() - getLastSessionTime(appContext)) / 1000 >= MIN_ON_SESSION_TIME;
    }

    static boolean areNotificationsEnabledForSubscribedState() {
        if (mInitBuilder.mUnsubscribeWhenNotificationsAreDisabled) {
            return OSUtils.areNotificationsEnabled(appContext);
        }
        return true;
    }

    static void handleSuccessfulEmailLogout() {
        if (emailLogoutHandler != null) {
            emailLogoutHandler.onSuccess();
            emailLogoutHandler = null;
        }
    }

    static void handleFailedEmailLogout() {
        if (emailLogoutHandler != null) {
            emailLogoutHandler.onFailure(new EmailUpdateError(EmailErrorType.NETWORK, "Failed due to network failure. Will retry on next sync."));
            emailLogoutHandler = null;
        }
    }

    static void fireEmailUpdateSuccess() {
        if (emailUpdateHandler != null) {
            emailUpdateHandler.onSuccess();
            emailUpdateHandler = null;
        }
    }

    static void fireEmailUpdateFailure() {
        if (emailUpdateHandler != null) {
            emailUpdateHandler.onFailure(new EmailUpdateError(EmailErrorType.NETWORK, "Failed due to network failure. Will retry on next sync."));
            emailUpdateHandler = null;
        }
    }
}
