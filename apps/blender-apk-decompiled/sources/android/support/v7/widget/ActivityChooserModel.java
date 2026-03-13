package android.support.v7.widget;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.DataSetObservable;
import android.os.AsyncTask;
import android.text.TextUtils;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
class ActivityChooserModel extends DataSetObservable {
    static final String ATTRIBUTE_ACTIVITY = "activity";
    static final String ATTRIBUTE_TIME = "time";
    static final String ATTRIBUTE_WEIGHT = "weight";
    static final boolean DEBUG = false;
    private static final int DEFAULT_ACTIVITY_INFLATION = 5;
    private static final float DEFAULT_HISTORICAL_RECORD_WEIGHT = 1.0f;
    public static final String DEFAULT_HISTORY_FILE_NAME = "activity_choser_model_history.xml";
    public static final int DEFAULT_HISTORY_MAX_LENGTH = 50;
    private static final String HISTORY_FILE_EXTENSION = ".xml";
    private static final int INVALID_INDEX = -1;
    static final String LOG_TAG = "ActivityChooserModel";
    static final String TAG_HISTORICAL_RECORD = "historical-record";
    static final String TAG_HISTORICAL_RECORDS = "historical-records";
    private OnChooseActivityListener mActivityChoserModelPolicy;
    final Context mContext;
    final String mHistoryFileName;
    private Intent mIntent;
    private static final Object sRegistryLock = new Object();
    private static final Map<String, ActivityChooserModel> sDataModelRegistry = new HashMap();
    private final Object mInstanceLock = new Object();
    private final List<ActivityResolveInfo> mActivities = new ArrayList();
    private final List<HistoricalRecord> mHistoricalRecords = new ArrayList();
    private ActivitySorter mActivitySorter = new DefaultSorter();
    private int mHistoryMaxSize = 50;
    boolean mCanReadHistoricalData = true;
    private boolean mReadShareHistoryCalled = false;
    private boolean mHistoricalRecordsChanged = true;
    private boolean mReloadActivities = false;

    public interface ActivityChooserModelClient {
        void setActivityChooserModel(ActivityChooserModel activityChooserModel);
    }

    public interface ActivitySorter {
        void sort(Intent intent, List<ActivityResolveInfo> list, List<HistoricalRecord> list2);
    }

    public interface OnChooseActivityListener {
        boolean onChooseActivity(ActivityChooserModel activityChooserModel, Intent intent);
    }

    public static ActivityChooserModel get(Context context, String str) {
        ActivityChooserModel activityChooserModel;
        synchronized (sRegistryLock) {
            activityChooserModel = sDataModelRegistry.get(str);
            if (activityChooserModel == null) {
                activityChooserModel = new ActivityChooserModel(context, str);
                sDataModelRegistry.put(str, activityChooserModel);
            }
        }
        return activityChooserModel;
    }

    private ActivityChooserModel(Context context, String str) {
        this.mContext = context.getApplicationContext();
        if (!TextUtils.isEmpty(str) && !str.endsWith(HISTORY_FILE_EXTENSION)) {
            this.mHistoryFileName = str + HISTORY_FILE_EXTENSION;
            return;
        }
        this.mHistoryFileName = str;
    }

    public void setIntent(Intent intent) {
        synchronized (this.mInstanceLock) {
            if (this.mIntent == intent) {
                return;
            }
            this.mIntent = intent;
            this.mReloadActivities = true;
            ensureConsistentState();
        }
    }

    public Intent getIntent() {
        Intent intent;
        synchronized (this.mInstanceLock) {
            intent = this.mIntent;
        }
        return intent;
    }

    public int getActivityCount() {
        int size;
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            size = this.mActivities.size();
        }
        return size;
    }

    public ResolveInfo getActivity(int i) {
        ResolveInfo resolveInfo;
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            resolveInfo = this.mActivities.get(i).resolveInfo;
        }
        return resolveInfo;
    }

    public int getActivityIndex(ResolveInfo resolveInfo) {
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            List<ActivityResolveInfo> list = this.mActivities;
            int size = list.size();
            for (int i = 0; i < size; i++) {
                if (list.get(i).resolveInfo == resolveInfo) {
                    return i;
                }
            }
            return -1;
        }
    }

    public Intent chooseActivity(int i) {
        synchronized (this.mInstanceLock) {
            if (this.mIntent == null) {
                return null;
            }
            ensureConsistentState();
            ActivityResolveInfo activityResolveInfo = this.mActivities.get(i);
            ComponentName componentName = new ComponentName(activityResolveInfo.resolveInfo.activityInfo.packageName, activityResolveInfo.resolveInfo.activityInfo.name);
            Intent intent = new Intent(this.mIntent);
            intent.setComponent(componentName);
            if (this.mActivityChoserModelPolicy != null) {
                if (this.mActivityChoserModelPolicy.onChooseActivity(this, new Intent(intent))) {
                    return null;
                }
            }
            addHistoricalRecord(new HistoricalRecord(componentName, System.currentTimeMillis(), 1.0f));
            return intent;
        }
    }

    public void setOnChooseActivityListener(OnChooseActivityListener onChooseActivityListener) {
        synchronized (this.mInstanceLock) {
            this.mActivityChoserModelPolicy = onChooseActivityListener;
        }
    }

    public ResolveInfo getDefaultActivity() {
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            if (this.mActivities.isEmpty()) {
                return null;
            }
            return this.mActivities.get(0).resolveInfo;
        }
    }

    public void setDefaultActivity(int i) {
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            ActivityResolveInfo activityResolveInfo = this.mActivities.get(i);
            ActivityResolveInfo activityResolveInfo2 = this.mActivities.get(0);
            addHistoricalRecord(new HistoricalRecord(new ComponentName(activityResolveInfo.resolveInfo.activityInfo.packageName, activityResolveInfo.resolveInfo.activityInfo.name), System.currentTimeMillis(), activityResolveInfo2 != null ? (activityResolveInfo2.weight - activityResolveInfo.weight) + 5.0f : 1.0f));
        }
    }

    private void persistHistoricalDataIfNeeded() {
        if (!this.mReadShareHistoryCalled) {
            throw new IllegalStateException("No preceding call to #readHistoricalData");
        }
        if (this.mHistoricalRecordsChanged) {
            this.mHistoricalRecordsChanged = false;
            if (TextUtils.isEmpty(this.mHistoryFileName)) {
                return;
            }
            new PersistHistoryAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new ArrayList(this.mHistoricalRecords), this.mHistoryFileName);
        }
    }

    public void setActivitySorter(ActivitySorter activitySorter) {
        synchronized (this.mInstanceLock) {
            if (this.mActivitySorter == activitySorter) {
                return;
            }
            this.mActivitySorter = activitySorter;
            if (sortActivitiesIfNeeded()) {
                notifyChanged();
            }
        }
    }

    public void setHistoryMaxSize(int i) {
        synchronized (this.mInstanceLock) {
            if (this.mHistoryMaxSize == i) {
                return;
            }
            this.mHistoryMaxSize = i;
            pruneExcessiveHistoricalRecordsIfNeeded();
            if (sortActivitiesIfNeeded()) {
                notifyChanged();
            }
        }
    }

    public int getHistoryMaxSize() {
        int i;
        synchronized (this.mInstanceLock) {
            i = this.mHistoryMaxSize;
        }
        return i;
    }

    public int getHistorySize() {
        int size;
        synchronized (this.mInstanceLock) {
            ensureConsistentState();
            size = this.mHistoricalRecords.size();
        }
        return size;
    }

    private void ensureConsistentState() {
        boolean zLoadActivitiesIfNeeded = loadActivitiesIfNeeded() | readHistoricalDataIfNeeded();
        pruneExcessiveHistoricalRecordsIfNeeded();
        if (zLoadActivitiesIfNeeded) {
            sortActivitiesIfNeeded();
            notifyChanged();
        }
    }

    private boolean sortActivitiesIfNeeded() {
        if (this.mActivitySorter == null || this.mIntent == null || this.mActivities.isEmpty() || this.mHistoricalRecords.isEmpty()) {
            return false;
        }
        this.mActivitySorter.sort(this.mIntent, this.mActivities, Collections.unmodifiableList(this.mHistoricalRecords));
        return true;
    }

    private boolean loadActivitiesIfNeeded() {
        if (!this.mReloadActivities || this.mIntent == null) {
            return false;
        }
        this.mReloadActivities = false;
        this.mActivities.clear();
        List<ResolveInfo> listQueryIntentActivities = this.mContext.getPackageManager().queryIntentActivities(this.mIntent, 0);
        int size = listQueryIntentActivities.size();
        for (int i = 0; i < size; i++) {
            this.mActivities.add(new ActivityResolveInfo(listQueryIntentActivities.get(i)));
        }
        return true;
    }

    private boolean readHistoricalDataIfNeeded() {
        if (!this.mCanReadHistoricalData || !this.mHistoricalRecordsChanged || TextUtils.isEmpty(this.mHistoryFileName)) {
            return false;
        }
        this.mCanReadHistoricalData = false;
        this.mReadShareHistoryCalled = true;
        readHistoricalDataImpl();
        return true;
    }

    private boolean addHistoricalRecord(HistoricalRecord historicalRecord) {
        boolean zAdd = this.mHistoricalRecords.add(historicalRecord);
        if (zAdd) {
            this.mHistoricalRecordsChanged = true;
            pruneExcessiveHistoricalRecordsIfNeeded();
            persistHistoricalDataIfNeeded();
            sortActivitiesIfNeeded();
            notifyChanged();
        }
        return zAdd;
    }

    private void pruneExcessiveHistoricalRecordsIfNeeded() {
        int size = this.mHistoricalRecords.size() - this.mHistoryMaxSize;
        if (size <= 0) {
            return;
        }
        this.mHistoricalRecordsChanged = true;
        for (int i = 0; i < size; i++) {
            this.mHistoricalRecords.remove(0);
        }
    }

    public static final class HistoricalRecord {
        public final ComponentName activity;
        public final long time;
        public final float weight;

        public HistoricalRecord(String str, long j, float f) {
            this(ComponentName.unflattenFromString(str), j, f);
        }

        public HistoricalRecord(ComponentName componentName, long j, float f) {
            this.activity = componentName;
            this.time = j;
            this.weight = f;
        }

        public int hashCode() {
            return (((((this.activity == null ? 0 : this.activity.hashCode()) + 31) * 31) + ((int) (this.time ^ (this.time >>> 32)))) * 31) + Float.floatToIntBits(this.weight);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            HistoricalRecord historicalRecord = (HistoricalRecord) obj;
            if (this.activity == null) {
                if (historicalRecord.activity != null) {
                    return false;
                }
            } else if (!this.activity.equals(historicalRecord.activity)) {
                return false;
            }
            return this.time == historicalRecord.time && Float.floatToIntBits(this.weight) == Float.floatToIntBits(historicalRecord.weight);
        }

        public String toString() {
            return "[; activity:" + this.activity + "; time:" + this.time + "; weight:" + new BigDecimal(this.weight) + "]";
        }
    }

    public static final class ActivityResolveInfo implements Comparable<ActivityResolveInfo> {
        public final ResolveInfo resolveInfo;
        public float weight;

        public ActivityResolveInfo(ResolveInfo resolveInfo) {
            this.resolveInfo = resolveInfo;
        }

        public int hashCode() {
            return Float.floatToIntBits(this.weight) + 31;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            return obj != null && getClass() == obj.getClass() && Float.floatToIntBits(this.weight) == Float.floatToIntBits(((ActivityResolveInfo) obj).weight);
        }

        @Override // java.lang.Comparable
        public int compareTo(ActivityResolveInfo activityResolveInfo) {
            return Float.floatToIntBits(activityResolveInfo.weight) - Float.floatToIntBits(this.weight);
        }

        public String toString() {
            return "[resolveInfo:" + this.resolveInfo.toString() + "; weight:" + new BigDecimal(this.weight) + "]";
        }
    }

    private static final class DefaultSorter implements ActivitySorter {
        private static final float WEIGHT_DECAY_COEFFICIENT = 0.95f;
        private final Map<ComponentName, ActivityResolveInfo> mPackageNameToActivityMap = new HashMap();

        DefaultSorter() {
        }

        @Override // android.support.v7.widget.ActivityChooserModel.ActivitySorter
        public void sort(Intent intent, List<ActivityResolveInfo> list, List<HistoricalRecord> list2) {
            Map<ComponentName, ActivityResolveInfo> map = this.mPackageNameToActivityMap;
            map.clear();
            int size = list.size();
            for (int i = 0; i < size; i++) {
                ActivityResolveInfo activityResolveInfo = list.get(i);
                activityResolveInfo.weight = 0.0f;
                map.put(new ComponentName(activityResolveInfo.resolveInfo.activityInfo.packageName, activityResolveInfo.resolveInfo.activityInfo.name), activityResolveInfo);
            }
            float f = 1.0f;
            for (int size2 = list2.size() - 1; size2 >= 0; size2--) {
                HistoricalRecord historicalRecord = list2.get(size2);
                ActivityResolveInfo activityResolveInfo2 = map.get(historicalRecord.activity);
                if (activityResolveInfo2 != null) {
                    activityResolveInfo2.weight += historicalRecord.weight * f;
                    f *= WEIGHT_DECAY_COEFFICIENT;
                }
            }
            Collections.sort(list);
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:15:0x0036, code lost:
    
        r0.close();
     */
    /* JADX WARN: Code restructure failed: missing block: B:65:?, code lost:
    
        return;
     */
    /* JADX WARN: Code restructure failed: missing block: B:66:?, code lost:
    
        return;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private void readHistoricalDataImpl() {
        /*
            r9 = this;
            android.content.Context r0 = r9.mContext     // Catch: java.io.FileNotFoundException -> Lc4
            java.lang.String r1 = r9.mHistoryFileName     // Catch: java.io.FileNotFoundException -> Lc4
            java.io.FileInputStream r0 = r0.openFileInput(r1)     // Catch: java.io.FileNotFoundException -> Lc4
            org.xmlpull.v1.XmlPullParser r1 = android.util.Xml.newPullParser()     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            java.lang.String r2 = "UTF-8"
            r1.setInput(r0, r2)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            r2 = 0
        L12:
            r3 = 1
            if (r2 == r3) goto L1d
            r4 = 2
            if (r2 == r4) goto L1d
            int r2 = r1.next()     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            goto L12
        L1d:
            java.lang.String r2 = "historical-records"
            java.lang.String r4 = r1.getName()     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            boolean r2 = r2.equals(r4)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            if (r2 == 0) goto L7a
            java.util.List<android.support.v7.widget.ActivityChooserModel$HistoricalRecord> r2 = r9.mHistoricalRecords     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            r2.clear()     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
        L2e:
            int r4 = r1.next()     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            if (r4 != r3) goto L3b
            if (r0 == 0) goto Lbd
        L36:
            r0.close()     // Catch: java.io.IOException -> Lbd
            goto Lbd
        L3b:
            r5 = 3
            if (r4 == r5) goto L2e
            r5 = 4
            if (r4 != r5) goto L42
            goto L2e
        L42:
            java.lang.String r4 = r1.getName()     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            java.lang.String r5 = "historical-record"
            boolean r4 = r5.equals(r4)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            if (r4 == 0) goto L72
            java.lang.String r4 = "activity"
            r5 = 0
            java.lang.String r4 = r1.getAttributeValue(r5, r4)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            java.lang.String r6 = "time"
            java.lang.String r6 = r1.getAttributeValue(r5, r6)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            long r6 = java.lang.Long.parseLong(r6)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            java.lang.String r8 = "weight"
            java.lang.String r5 = r1.getAttributeValue(r5, r8)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            float r5 = java.lang.Float.parseFloat(r5)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            android.support.v7.widget.ActivityChooserModel$HistoricalRecord r8 = new android.support.v7.widget.ActivityChooserModel$HistoricalRecord     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            r8.<init>(r4, r6, r5)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            r2.add(r8)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            goto L2e
        L72:
            org.xmlpull.v1.XmlPullParserException r1 = new org.xmlpull.v1.XmlPullParserException     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            java.lang.String r2 = "Share records file not well-formed."
            r1.<init>(r2)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            throw r1     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
        L7a:
            org.xmlpull.v1.XmlPullParserException r1 = new org.xmlpull.v1.XmlPullParserException     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            java.lang.String r2 = "Share records file does not start with historical-records tag."
            r1.<init>(r2)     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
            throw r1     // Catch: java.lang.Throwable -> L82 java.io.IOException -> L84 org.xmlpull.v1.XmlPullParserException -> La0
        L82:
            r9 = move-exception
            goto Lbe
        L84:
            r1 = move-exception
            java.lang.String r2 = android.support.v7.widget.ActivityChooserModel.LOG_TAG     // Catch: java.lang.Throwable -> L82
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L82
            r3.<init>()     // Catch: java.lang.Throwable -> L82
            java.lang.String r4 = "Error reading historical recrod file: "
            r3.append(r4)     // Catch: java.lang.Throwable -> L82
            java.lang.String r9 = r9.mHistoryFileName     // Catch: java.lang.Throwable -> L82
            r3.append(r9)     // Catch: java.lang.Throwable -> L82
            java.lang.String r9 = r3.toString()     // Catch: java.lang.Throwable -> L82
            android.util.Log.e(r2, r9, r1)     // Catch: java.lang.Throwable -> L82
            if (r0 == 0) goto Lbd
            goto L36
        La0:
            r1 = move-exception
            java.lang.String r2 = android.support.v7.widget.ActivityChooserModel.LOG_TAG     // Catch: java.lang.Throwable -> L82
            java.lang.StringBuilder r3 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> L82
            r3.<init>()     // Catch: java.lang.Throwable -> L82
            java.lang.String r4 = "Error reading historical recrod file: "
            r3.append(r4)     // Catch: java.lang.Throwable -> L82
            java.lang.String r9 = r9.mHistoryFileName     // Catch: java.lang.Throwable -> L82
            r3.append(r9)     // Catch: java.lang.Throwable -> L82
            java.lang.String r9 = r3.toString()     // Catch: java.lang.Throwable -> L82
            android.util.Log.e(r2, r9, r1)     // Catch: java.lang.Throwable -> L82
            if (r0 == 0) goto Lbd
            goto L36
        Lbd:
            return
        Lbe:
            if (r0 == 0) goto Lc3
            r0.close()     // Catch: java.io.IOException -> Lc3
        Lc3:
            throw r9
        Lc4:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.ActivityChooserModel.readHistoricalDataImpl():void");
    }

    private final class PersistHistoryAsyncTask extends AsyncTask<Object, Void, Void> {
        PersistHistoryAsyncTask() {
        }

        /* JADX WARN: Removed duplicated region for block: B:46:0x0071 A[EXC_TOP_SPLITTER, SYNTHETIC] */
        @Override // android.os.AsyncTask
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public java.lang.Void doInBackground(java.lang.Object... r12) {
            /*
                Method dump skipped, instruction units count: 256
                To view this dump add '--comments-level debug' option
            */
            throw new UnsupportedOperationException("Method not decompiled: android.support.v7.widget.ActivityChooserModel.PersistHistoryAsyncTask.doInBackground(java.lang.Object[]):java.lang.Void");
        }
    }
}
