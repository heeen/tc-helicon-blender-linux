package com.onesignal;

import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.onesignal.AndroidSupportV4Compat;
import com.onesignal.OneSignal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/* JADX INFO: loaded from: classes.dex */
class LocationGMS {
    private static final long BACKGROUND_UPDATE_TIME_MS = 570000;
    private static final long FOREGROUND_UPDATE_TIME_MS = 270000;
    private static final long TIME_BACKGROUND_SEC = 600;
    private static final long TIME_FOREGROUND_SEC = 300;
    private static Context classContext;
    private static Thread fallbackFailThread;
    private static boolean locationCoarse;
    private static LocationHandlerThread locationHandlerThread;
    static LocationUpdateListener locationUpdateListener;
    private static GoogleApiClientCompatProxy mGoogleApiClient;
    private static Location mLastLocation;
    static String requestPermission;
    protected static final Object syncLock = new Object() { // from class: com.onesignal.LocationGMS.1
    };
    private static ConcurrentHashMap<CALLBACK_TYPE, LocationHandler> locationHandlers = new ConcurrentHashMap<>();

    enum CALLBACK_TYPE {
        STARTUP,
        PROMPT_LOCATION,
        SYNC_SERVICE
    }

    interface LocationHandler {
        void complete(LocationPoint locationPoint);

        CALLBACK_TYPE getType();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static int getApiFallbackWait() {
        return 30000;
    }

    LocationGMS() {
    }

    static class LocationPoint {
        Float accuracy;
        Boolean bg;
        Double lat;
        Double log;
        Long timeStamp;
        Integer type;

        LocationPoint() {
        }
    }

    static boolean scheduleUpdate(Context context) {
        if (!hasLocationPermission(context) || !OneSignal.shareLocation) {
            return false;
        }
        OneSignalSyncServiceUtils.scheduleLocationUpdateTask(context, ((OneSignal.isForeground() ? TIME_FOREGROUND_SEC : TIME_BACKGROUND_SEC) * 1000) - (System.currentTimeMillis() - getLastLocationTime()));
        return true;
    }

    private static void setLastLocationTime(long j) {
        OneSignalPrefs.saveLong(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_LAST_LOCATION_TIME, j);
    }

    private static long getLastLocationTime() {
        return OneSignalPrefs.getLong(OneSignalPrefs.PREFS_ONESIGNAL, OneSignalPrefs.PREFS_OS_LAST_LOCATION_TIME, -600000L);
    }

    private static boolean hasLocationPermission(Context context) {
        return AndroidSupportV4Compat.ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION") == 0 || AndroidSupportV4Compat.ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_COARSE_LOCATION") == 0;
    }

    static void getLocation(Context context, boolean z, LocationHandler locationHandler) {
        classContext = context;
        locationHandlers.put(locationHandler.getType(), locationHandler);
        if (!OneSignal.shareLocation) {
            fireFailedComplete();
            return;
        }
        int iCheckSelfPermission = AndroidSupportV4Compat.ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION");
        int iCheckSelfPermission2 = -1;
        if (iCheckSelfPermission == -1) {
            iCheckSelfPermission2 = AndroidSupportV4Compat.ContextCompat.checkSelfPermission(context, "android.permission.ACCESS_COARSE_LOCATION");
            locationCoarse = true;
        }
        if (Build.VERSION.SDK_INT < 23) {
            if (iCheckSelfPermission != 0 && iCheckSelfPermission2 != 0) {
                locationHandler.complete(null);
                return;
            } else {
                startGetLocation();
                return;
            }
        }
        if (iCheckSelfPermission != 0) {
            try {
                List listAsList = Arrays.asList(context.getPackageManager().getPackageInfo(context.getPackageName(), 4096).requestedPermissions);
                if (listAsList.contains("android.permission.ACCESS_FINE_LOCATION")) {
                    requestPermission = "android.permission.ACCESS_FINE_LOCATION";
                } else if (listAsList.contains("android.permission.ACCESS_COARSE_LOCATION") && iCheckSelfPermission2 != 0) {
                    requestPermission = "android.permission.ACCESS_COARSE_LOCATION";
                }
                if (requestPermission != null && z) {
                    PermissionsActivity.startPrompt();
                    return;
                } else if (iCheckSelfPermission2 == 0) {
                    startGetLocation();
                    return;
                } else {
                    fireFailedComplete();
                    return;
                }
            } catch (Throwable th) {
                th.printStackTrace();
                return;
            }
        }
        startGetLocation();
    }

    static void startGetLocation() {
        if (fallbackFailThread != null) {
            return;
        }
        try {
            synchronized (syncLock) {
                startFallBackThread();
                if (locationHandlerThread == null) {
                    locationHandlerThread = new LocationHandlerThread();
                }
                if (mGoogleApiClient == null || mLastLocation == null) {
                    GoogleApiClientListener googleApiClientListener = new GoogleApiClientListener();
                    mGoogleApiClient = new GoogleApiClientCompatProxy(new GoogleApiClient.Builder(classContext).addApi(LocationServices.API).addConnectionCallbacks(googleApiClientListener).addOnConnectionFailedListener(googleApiClientListener).setHandler(locationHandlerThread.mHandler).build());
                    mGoogleApiClient.connect();
                } else if (mLastLocation != null) {
                    fireCompleteForLocation(mLastLocation);
                }
            }
        } catch (Throwable th) {
            OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "Location permission exists but there was an error initializing: ", th);
            fireFailedComplete();
        }
    }

    private static void startFallBackThread() {
        fallbackFailThread = new Thread(new Runnable() { // from class: com.onesignal.LocationGMS.2
            @Override // java.lang.Runnable
            public void run() {
                try {
                    Thread.sleep(LocationGMS.getApiFallbackWait());
                    OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "Location permission exists but GoogleApiClient timed out. Maybe related to mismatch google-play aar versions.");
                    LocationGMS.fireFailedComplete();
                    LocationGMS.scheduleUpdate(LocationGMS.classContext);
                } catch (InterruptedException unused) {
                }
            }
        }, "OS_GMS_LOCATION_FALLBACK");
        fallbackFailThread.start();
    }

    static void fireFailedComplete() {
        PermissionsActivity.answered = false;
        synchronized (syncLock) {
            if (mGoogleApiClient != null) {
                mGoogleApiClient.disconnect();
            }
            mGoogleApiClient = null;
        }
        fireComplete(null);
    }

    private static void fireComplete(LocationPoint locationPoint) {
        Thread thread;
        HashMap map = new HashMap();
        synchronized (LocationGMS.class) {
            map.putAll(locationHandlers);
            locationHandlers.clear();
            thread = fallbackFailThread;
        }
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            ((LocationHandler) map.get((CALLBACK_TYPE) it.next())).complete(locationPoint);
        }
        if (thread != null && !Thread.currentThread().equals(thread)) {
            thread.interrupt();
        }
        if (thread == fallbackFailThread) {
            synchronized (LocationGMS.class) {
                if (thread == fallbackFailThread) {
                    fallbackFailThread = null;
                }
            }
        }
        setLastLocationTime(System.currentTimeMillis());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static void fireCompleteForLocation(Location location) {
        LocationPoint locationPoint = new LocationPoint();
        locationPoint.accuracy = Float.valueOf(location.getAccuracy());
        locationPoint.bg = Boolean.valueOf(!OneSignal.isForeground());
        locationPoint.type = Integer.valueOf(!locationCoarse ? 1 : 0);
        locationPoint.timeStamp = Long.valueOf(location.getTime());
        if (locationCoarse) {
            locationPoint.lat = Double.valueOf(new BigDecimal(location.getLatitude()).setScale(7, RoundingMode.HALF_UP).doubleValue());
            locationPoint.log = Double.valueOf(new BigDecimal(location.getLongitude()).setScale(7, RoundingMode.HALF_UP).doubleValue());
        } else {
            locationPoint.lat = Double.valueOf(location.getLatitude());
            locationPoint.log = Double.valueOf(location.getLongitude());
        }
        fireComplete(locationPoint);
        scheduleUpdate(classContext);
    }

    static void onFocusChange() {
        synchronized (syncLock) {
            if (mGoogleApiClient != null && mGoogleApiClient.realInstance().isConnected()) {
                GoogleApiClient googleApiClientRealInstance = mGoogleApiClient.realInstance();
                if (locationUpdateListener != null) {
                    LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClientRealInstance, locationUpdateListener);
                }
                locationUpdateListener = new LocationUpdateListener(googleApiClientRealInstance);
            }
        }
    }

    private static class GoogleApiClientListener implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
        private GoogleApiClientListener() {
        }

        @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
        public void onConnected(Bundle bundle) {
            synchronized (LocationGMS.syncLock) {
                PermissionsActivity.answered = false;
                if (LocationGMS.mLastLocation == null) {
                    Location unused = LocationGMS.mLastLocation = FusedLocationApiWrapper.getLastLocation(LocationGMS.mGoogleApiClient.realInstance());
                    if (LocationGMS.mLastLocation != null) {
                        LocationGMS.fireCompleteForLocation(LocationGMS.mLastLocation);
                    }
                }
                LocationGMS.locationUpdateListener = new LocationUpdateListener(LocationGMS.mGoogleApiClient.realInstance());
            }
        }

        @Override // com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
        public void onConnectionSuspended(int i) {
            LocationGMS.fireFailedComplete();
        }

        @Override // com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            LocationGMS.fireFailedComplete();
        }
    }

    static class LocationUpdateListener implements LocationListener {
        private GoogleApiClient mGoogleApiClient;

        LocationUpdateListener(GoogleApiClient googleApiClient) {
            this.mGoogleApiClient = googleApiClient;
            long j = OneSignal.isForeground() ? LocationGMS.FOREGROUND_UPDATE_TIME_MS : LocationGMS.BACKGROUND_UPDATE_TIME_MS;
            FusedLocationApiWrapper.requestLocationUpdates(this.mGoogleApiClient, LocationRequest.create().setFastestInterval(j).setInterval(j).setMaxWaitTime((long) (j * 1.5d)).setPriority(102), this);
        }

        @Override // com.google.android.gms.location.LocationListener
        public void onLocationChanged(Location location) {
            Location unused = LocationGMS.mLastLocation = location;
            OneSignal.Log(OneSignal.LOG_LEVEL.INFO, "Location Change Detected");
        }
    }

    static class FusedLocationApiWrapper {
        FusedLocationApiWrapper() {
        }

        static void requestLocationUpdates(GoogleApiClient googleApiClient, LocationRequest locationRequest, LocationListener locationListener) {
            try {
                synchronized (LocationGMS.syncLock) {
                    if (googleApiClient.isConnected()) {
                        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener);
                    }
                }
            } catch (Throwable th) {
                OneSignal.Log(OneSignal.LOG_LEVEL.WARN, "FusedLocationApi.requestLocationUpdates failed!", th);
            }
        }

        static Location getLastLocation(GoogleApiClient googleApiClient) {
            synchronized (LocationGMS.syncLock) {
                if (!googleApiClient.isConnected()) {
                    return null;
                }
                return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            }
        }
    }

    private static class LocationHandlerThread extends HandlerThread {
        Handler mHandler;

        LocationHandlerThread() {
            super("OSH_LocationHandlerThread");
            start();
            this.mHandler = new Handler(getLooper());
        }
    }
}
