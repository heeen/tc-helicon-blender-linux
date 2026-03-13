package tchelicon.com.blenderappandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuPopupHelper;
import android.support.v7.widget.PopupMenu;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import com.airbnb.lottie.LottieAnimationView;
import com.flurry.android.FlurryAgent;
import com.kobakei.ratethisapp.RateThisApp;
import com.onesignal.OneSignal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class MixActivity extends AppCompatActivity {
    private static final long SCAN_PERIOD = 15000;
    private static final String TAG = "MixActivity";
    CountDownTimer bleBroadcastTimer;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    CountDownTimer countDownTimer;
    View loadPresetView;
    View outputSelectView;
    PopupWindow popupWindow;
    AppState appState = new AppState();
    Central central = Central.getInstance();
    Peripheral peripheral = Peripheral.getInstance();
    int REQUEST_ENABLE_BT = 111;
    boolean scanning = false;
    boolean flipperIsMixView = false;
    boolean bleUpdatingToValues = false;
    boolean isPresetEditMode = false;
    BottomSheetDialog infoMenu = null;

    /* JADX WARN: Type inference failed for: r8v13, types: [tchelicon.com.blenderappandroid.MixActivity$2] */
    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.support.v4.app.SupportActivity, android.app.Activity
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        SharedPreferences sharedPreferences = getSharedPreferences("MyPreferences", 0);
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        sharedPreferences.getInt(Constants.kInputTipShowCount, 0);
        editorEdit.putBoolean(Constants.kInputTipShowThisLaunch, false);
        editorEdit.apply();
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        Log.d(TAG, "onCreate()");
        setupThirdParty();
        this.appState.init();
        this.appState.appStateCallback = new AppStateCallback() { // from class: tchelicon.com.blenderappandroid.MixActivity.1
            @Override // tchelicon.com.blenderappandroid.AppStateCallback
            public void callbackUpdateToValues() {
                new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: tchelicon.com.blenderappandroid.MixActivity.1.1
                    @Override // java.lang.Runnable
                    public void run() {
                        MixActivity.this.updateToValues();
                    }
                });
            }

            @Override // tchelicon.com.blenderappandroid.AppStateCallback
            public void connectionStatusChanged() {
                new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: tchelicon.com.blenderappandroid.MixActivity.1.2
                    @Override // java.lang.Runnable
                    public void run() {
                        MixActivity.this.handleConnectionStatusChanged();
                    }
                });
            }

            @Override // tchelicon.com.blenderappandroid.AppStateCallback
            public void scanForMore() {
                Log.d(MixActivity.TAG, "scanForMore");
                new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: tchelicon.com.blenderappandroid.MixActivity.1.3
                    @Override // java.lang.Runnable
                    public void run() {
                        MixActivity.this.connectPressed();
                    }
                });
            }

            @Override // tchelicon.com.blenderappandroid.AppStateCallback
            public void toast(final CharSequence charSequence, final int i) {
                Log.d(MixActivity.TAG, "callback toast = " + ((Object) charSequence));
                new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: tchelicon.com.blenderappandroid.MixActivity.1.4
                    @Override // java.lang.Runnable
                    public void run() {
                        MixActivity.this.showToast(charSequence, i);
                    }
                });
            }
        };
        this.central.init(getApplicationContext(), this.appState);
        this.peripheral.init(getApplicationContext(), this.appState);
        checkBluetoothPermissions();
        setupBluetooth();
        ((ProgressBar) findViewById(R.id.progressBar)).setProgress(0);
        this.bleBroadcastTimer = new CountDownTimer(5000L, Constants.broadcastTime) { // from class: tchelicon.com.blenderappandroid.MixActivity.2
            @Override // android.os.CountDownTimer
            public void onTick(long j) {
                MixActivity.this.broadcast();
            }

            @Override // android.os.CountDownTimer
            public void onFinish() {
                try {
                    MixActivity.this.restartBLETimer();
                } catch (Exception e) {
                    Log.e("Error", "Error: " + e.toString());
                }
            }
        }.start();
        initMixController();
        updateToValues();
    }

    private void initMixController() {
        MixController mixController = (MixController) findViewById(R.id.mixController);
        if (mixController != null) {
            mixController.onCreate(this.appState);
        } else {
            Log.d(TAG, "onCreate() mixController is null");
        }
    }

    private void setupThirdParty() {
        new FlurryAgent.Builder().withLogEnabled(true).build(this, Constants.kFlurry);
        setContentView(R.layout.activity_mix);
        FlurryAnalytics.analytics("Starup", "Location", Locale.getDefault().getCountry());
        OneSignal.startInit(this).inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification).unsubscribeWhenNotificationsAreDisabled(true).init();
        RateThisApp.onCreate(this);
        RateThisApp.init(new RateThisApp.Config(3, 3));
        RateThisApp.showRateDialogIfNeeded(this);
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity
    public void onPause() {
        super.onPause();
        Log.d(TAG, "OnPAUSE");
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.app.Activity
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, android.app.Activity
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        this.central.disconnectEverything();
        if (this.appState.connectionState == ConnectionState.peripheral) {
            this.peripheral.disconnect();
        }
        super.onDestroy();
    }

    public void showToast(CharSequence charSequence, int i) {
        Log.d(TAG, "showToast text = " + ((Object) charSequence));
        View viewInflate = getLayoutInflater().inflate(R.layout.toast, (ViewGroup) findViewById(R.id.custom_toast_container));
        TextView textView = (TextView) viewInflate.findViewById(R.id.text);
        textView.setText(charSequence);
        textView.setTextSize(15.0f);
        textView.setWidth((int) ((getResources().getDisplayMetrics().density * 344.0f) + 0.5f));
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(83, 32, 48);
        toast.setDuration(i);
        toast.setView(viewInflate);
        toast.show();
    }

    public void clearForShutdown() {
        this.countDownTimer.cancel();
        this.central.stopScan();
        this.peripheral.stopAdvertising();
    }

    public void handleConnectionStatusChanged() {
        Log.d(TAG, "handleConnectionStatusChanged");
        Log.d(TAG, "hasBlender = " + this.appState.hasBlender);
        Log.d(TAG, "scanningForMore = " + this.appState.scanningForMore);
        Log.d(TAG, "advertising = " + this.appState.advertising);
        Log.d(TAG, "scanning = " + this.appState.scanning);
        if (!this.appState.hasBlender.booleanValue()) {
            if (this.infoMenu != null) {
                this.infoMenu.dismiss();
                this.infoMenu = null;
            }
            if (this.popupWindow != null) {
                this.popupWindow.dismiss();
                this.popupWindow = null;
            }
        }
        ViewFlipper viewFlipper = (ViewFlipper) findViewById(R.id.flipper);
        if (this.appState.hasBlender.booleanValue() && !this.appState.scanningForMore.booleanValue()) {
            viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.mixController)));
            setProgressImagePlaying(false);
            setStatusBarColor(R.color.c1);
            return;
        }
        if (this.appState.scanningForMore.booleanValue()) {
            viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.content_connect_progress)));
            setProgressText("Searching for friend");
            setProgressImagePlaying(true);
            return;
        }
        if (this.appState.advertising.booleanValue()) {
            viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.content_connect_progress)));
            setProgressText("Searching for network");
            setProgressImagePlaying(true);
        } else if (this.appState.scanning.booleanValue()) {
            viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.content_connect_progress)));
            setProgressText("Press the Bluetooth button on Blender");
            setProgressImagePlaying(true);
        } else if (!this.appState.hasBlender.booleanValue()) {
            viewFlipper.setDisplayedChild(viewFlipper.indexOfChild(findViewById(R.id.content_connect)));
            setProgressImagePlaying(false);
            setStatusBarColor(R.color.c3);
        } else {
            Log.d(TAG, "connectionStatusChanged Unhandled case");
            setProgressImagePlaying(false);
        }
    }

    public void setStatusBarColor(int i) {
        if (Build.VERSION.SDK_INT >= 21) {
            Window window = getWindow();
            window.addFlags(Integer.MIN_VALUE);
            window.setStatusBarColor(getResources().getColor(i));
        }
    }

    public void setProgressImagePlaying(Boolean bool) {
        LottieAnimationView lottieAnimationView = (LottieAnimationView) findViewById(R.id.progressImage);
        if (lottieAnimationView != null) {
            if (bool.booleanValue()) {
                lottieAnimationView.playAnimation();
            } else {
                lottieAnimationView.cancelAnimation();
            }
        }
    }

    public void broadcast() {
        if (this.appState.connectionState == ConnectionState.central) {
            this.central.sendChanges();
        } else if (this.appState.connectionState == ConnectionState.peripheral) {
            this.peripheral.sendChanges();
        }
    }

    public void restartBLETimer() {
        this.bleBroadcastTimer.start();
    }

    public void setupBluetooth() {
        this.bluetoothManager = (BluetoothManager) getSystemService("bluetooth");
        this.bluetoothAdapter = this.bluetoothManager.getAdapter();
        if (this.bluetoothAdapter == null) {
            Log.d(TAG, "setupBluetooth bluetoothAdapter == null");
        } else {
            Log.d(TAG, "setupBluetooth bluetoothAdapter not null");
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:32:0x00ae  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public java.lang.Boolean checkBluetoothPermissions() {
        /*
            r6 = this;
            java.lang.String r0 = "android.permission.ACCESS_FINE_LOCATION"
            int r0 = android.support.v4.content.ContextCompat.checkSelfPermission(r6, r0)
            r1 = 1
            r2 = 0
            if (r0 != 0) goto Lc
            r0 = r1
            goto Ld
        Lc:
            r0 = r2
        Ld:
            if (r0 != 0) goto L2a
            java.lang.String r3 = "MixActivity"
            java.lang.String r4 = "fineLocationPermissionCheck Failed"
            android.util.Log.d(r3, r4)
            java.lang.String r3 = "android.permission.ACCESS_FINE_LOCATION"
            java.lang.String[] r3 = new java.lang.String[]{r3}
            int r4 = tchelicon.com.blenderappandroid.Constants.fineLocationRequestCode
            android.support.v4.app.ActivityCompat.requestPermissions(r6, r3, r4)
            java.lang.String r3 = "Bluetooth requires location permissions to work"
            android.widget.Toast r3 = android.widget.Toast.makeText(r6, r3, r2)
            r3.show()
        L2a:
            java.lang.String r3 = "android.permission.BLUETOOTH_ADMIN"
            int r3 = android.support.v4.content.ContextCompat.checkSelfPermission(r6, r3)
            if (r3 != 0) goto L34
            r3 = r1
            goto L35
        L34:
            r3 = r2
        L35:
            if (r3 != 0) goto L52
            java.lang.String r4 = "MixActivity"
            java.lang.String r5 = "bluetoothAdminPermissionCheck Failed"
            android.util.Log.d(r4, r5)
            java.lang.String r4 = "android.permission.BLUETOOTH_ADMIN"
            java.lang.String[] r4 = new java.lang.String[]{r4}
            int r5 = tchelicon.com.blenderappandroid.Constants.bluetoothAdminRequestCode
            android.support.v4.app.ActivityCompat.requestPermissions(r6, r4, r5)
            java.lang.String r4 = "Bluetooth requires bluetooth admin permissions to work"
            android.widget.Toast r4 = android.widget.Toast.makeText(r6, r4, r2)
            r4.show()
        L52:
            java.lang.String r4 = "android.permission.BLUETOOTH"
            int r4 = android.support.v4.content.ContextCompat.checkSelfPermission(r6, r4)
            if (r4 != 0) goto L5c
            r4 = r1
            goto L5d
        L5c:
            r4 = r2
        L5d:
            if (r4 != 0) goto L7a
            java.lang.String r4 = "MixActivity"
            java.lang.String r5 = "bluetoothPermissionCheck Failed"
            android.util.Log.d(r4, r5)
            java.lang.String r4 = "android.permission.BLUETOOTH"
            java.lang.String[] r4 = new java.lang.String[]{r4}
            int r5 = tchelicon.com.blenderappandroid.Constants.bluetoothRequestCode
            android.support.v4.app.ActivityCompat.requestPermissions(r6, r4, r5)
            java.lang.String r4 = "Bluetooth requires bluetooth permissions to work"
            android.widget.Toast r4 = android.widget.Toast.makeText(r6, r4, r2)
            r4.show()
        L7a:
            android.bluetooth.BluetoothAdapter r4 = r6.bluetoothAdapter
            if (r4 != 0) goto L8a
            java.lang.String r4 = "MixActivity"
            java.lang.String r5 = "checkBluetoothPermissions bluetoothAdapter == null"
            android.util.Log.d(r4, r5)
            r6.setupBluetooth()
        L88:
            r6 = r2
            goto La7
        L8a:
            android.bluetooth.BluetoothAdapter r4 = r6.bluetoothAdapter
            boolean r4 = r4.isEnabled()
            if (r4 != 0) goto La6
            java.lang.String r4 = "MixActivity"
            java.lang.String r5 = "checkBluetoothPermissions !bluetoothAdapter.isEnabled()"
            android.util.Log.d(r4, r5)
            android.content.Intent r4 = new android.content.Intent
            java.lang.String r5 = "android.bluetooth.adapter.action.REQUEST_ENABLE"
            r4.<init>(r5)
            int r5 = r6.REQUEST_ENABLE_BT
            r6.startActivityForResult(r4, r5)
            goto L88
        La6:
            r6 = r1
        La7:
            if (r0 == 0) goto Lae
            if (r3 == 0) goto Lae
            if (r6 == 0) goto Lae
            goto Laf
        Lae:
            r1 = r2
        Laf:
            java.lang.Boolean r6 = java.lang.Boolean.valueOf(r1)
            return r6
        */
        throw new UnsupportedOperationException("Method not decompiled: tchelicon.com.blenderappandroid.MixActivity.checkBluetoothPermissions():java.lang.Boolean");
    }

    @Override // android.support.v4.app.FragmentActivity, android.app.Activity, android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback
    public void onRequestPermissionsResult(int i, String[] strArr, int[] iArr) {
        Log.d(TAG, "onRequest " + strArr[0] + "  result " + Integer.toString(iArr[0]) + "  requestCode " + Integer.toString(i));
    }

    @Override // android.app.Activity
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        menuItem.getItemId();
        return super.onOptionsItemSelected(menuItem);
    }

    public void hostPressed(View view) {
        Log.d(TAG, "host pressed");
        this.central.reconnectDevice();
        centralScan();
    }

    /* JADX WARN: Type inference failed for: r0v13, types: [tchelicon.com.blenderappandroid.MixActivity$3] */
    public void centralScan() {
        if (checkBluetoothPermissions().booleanValue()) {
            if (!this.appState.hasBlender.booleanValue()) {
                this.appState.waitingForBlenderState = true;
            }
            this.central.startScan();
            final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            this.appState.appStateCallback.connectionStatusChanged();
            if (this.countDownTimer != null) {
                this.countDownTimer.cancel();
                this.peripheral.stopAdvertising();
            }
            this.countDownTimer = new CountDownTimer(Constants.scanningTimeOutTime, 100L) { // from class: tchelicon.com.blenderappandroid.MixActivity.3
                @Override // android.os.CountDownTimer
                public void onTick(long j) {
                    progressBar.setProgress((int) (((Constants.scanningTimeOutTime - j) / Constants.scanningTimeOutTime) * 100.0d));
                }

                @Override // android.os.CountDownTimer
                public void onFinish() {
                    progressBar.setProgress(100);
                    Log.d(MixActivity.TAG, "TIME OUT SCANNING");
                    MixActivity.this.central.stopScan();
                    MixActivity.this.appState.appStateCallback.connectionStatusChanged();
                }
            }.start();
            return;
        }
        FlurryAnalytics.analytics("Bluetooth", "Central Errors", "centralScan failed checkBluetoothPermissions");
        Log.d(TAG, "centralScan failed to start because BLE check failed");
    }

    public void connectPressed() {
        Log.d(TAG, "connectPressed()");
        if (this.appState.connectionState == ConnectionState.peripheral) {
            this.appState.setScanForMore(true);
            this.appState.appDetailChanges.add(new Tuple(Constants.ParameterID.scanForMore.getId(), (byte) 0, (byte) 0));
        } else if (this.appState.connectionState == ConnectionState.central) {
            this.appState.setScanForMore(true);
            centralScan();
        }
    }

    /* JADX WARN: Type inference failed for: r8v13, types: [tchelicon.com.blenderappandroid.MixActivity$4] */
    public void joinPressed(View view) {
        if (checkBluetoothPermissions().booleanValue()) {
            Log.d(TAG, "joinPressed passed checkBluetoothPermissions");
            if (this.countDownTimer != null) {
                this.countDownTimer.cancel();
                this.central.stopScan();
                this.peripheral.stopAdvertising();
            }
            this.appState.parameterChanges.clear();
            this.appState.appDetailChanges.clear();
            this.peripheral.startAdvertising();
            final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
            this.countDownTimer = new CountDownTimer(Constants.scanningTimeOutTime, 100L) { // from class: tchelicon.com.blenderappandroid.MixActivity.4
                @Override // android.os.CountDownTimer
                public void onTick(long j) {
                    progressBar.setProgress((int) (((Constants.scanningTimeOutTime - j) / Constants.scanningTimeOutTime) * 100.0d));
                }

                @Override // android.os.CountDownTimer
                public void onFinish() {
                    progressBar.setProgress(100);
                    MixActivity.this.peripheral.stopAdvertising();
                }
            }.start();
            return;
        }
        FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "joinPressed failed checkBluetoothPermissions");
    }

    public void cancelPressed(View view) {
        if (this.countDownTimer != null) {
            this.countDownTimer.cancel();
            this.central.stopScan();
            this.peripheral.stopAdvertising();
        }
    }

    public void iconButtonPressed(View view) {
        int iIntValue = ((Integer) view.getTag()).intValue();
        this.appState.iconSelectTag = iIntValue;
        Constants.IconID iconID = this.appState.iconState.get(this.appState.iconSelectTag);
        View viewInflate = getLayoutInflater().inflate(R.layout.content_icon_select, (ViewGroup) null, false);
        float f = getResources().getDisplayMetrics().density;
        GridView gridView = (GridView) viewInflate.findViewById(R.id.icon_grid);
        gridView.setNumColumns(4);
        int i = (int) ((7.0f * f) + 0.5f);
        gridView.setHorizontalSpacing(i);
        gridView.setVerticalSpacing(i);
        int i2 = (int) ((5.0f * f) + 0.5f);
        gridView.setPadding(i2, 0, i2, i2);
        gridView.setColumnWidth((int) ((Constants.iconSelectIconSize * f) + 0.5f));
        gridView.setGravity(17);
        gridView.setWillNotDraw(false);
        gridView.setFocusableInTouchMode(true);
        gridView.setClickable(true);
        gridView.setAdapter((ListAdapter) new IconAdapter(this, iconID, iIntValue + 1));
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: tchelicon.com.blenderappandroid.MixActivity.5
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> adapterView, View view2, int i3, long j) {
                Constants.IconID iconID2 = Constants.CustomIcon.CustomIcons[i3].id;
                MixActivity.this.appState.iconState.set(MixActivity.this.appState.iconSelectTag, iconID2);
                Log.d(MixActivity.TAG, "iconSelected " + iconID2.name());
                MixActivity.this.popupWindow.dismiss();
                MixActivity.this.updateToValues();
                FlurryAnalytics.analytics("Icon Select", "Selected", iconID2.name());
                MixActivity.this.appState.appDetailChanges.add(new Tuple(Constants.ParameterID.iconChange.getId(), (byte) MixActivity.this.appState.iconSelectTag, iconID2.getId()));
            }
        });
        int i3 = (int) ((245.0f * f) + 0.5f);
        int i4 = (int) ((f * 363.0f) + 0.5f);
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        iArr[0] = iArr[0] - (((int) (((double) i3) * 0.5d)) - ((int) (((double) view.getHeight()) * 0.5d)));
        iArr[1] = (iArr[1] - i4) - 16;
        this.popupWindow = new PopupWindow(viewInflate, i3, i4, false);
        this.popupWindow.setTouchable(true);
        this.popupWindow.setFocusable(true);
        this.popupWindow.setOutsideTouchable(true);
        this.popupWindow.setElevation(10.0f);
        View viewFindViewById = findViewById(R.id.sliderContainer);
        if (viewFindViewById != null) {
            this.popupWindow.showAtLocation(viewFindViewById, 51, iArr[0], iArr[1]);
        }
        gridView.scrollTo(0, 0);
    }

    public void useDefaultIconPressed(View view) {
        this.appState.iconState.set(this.appState.iconSelectTag, Constants.IconID.defaultIcon);
        this.popupWindow.dismiss();
        updateToValues();
        FlurryAnalytics.analytics("Icon Select", "Selected", "Use Default Icon");
        this.appState.appDetailChanges.add(new Tuple(Constants.ParameterID.iconChange.getId(), (byte) this.appState.iconSelectTag, Constants.IconID.defaultIcon.getId()));
    }

    public void menuPressed(View view) {
        showInfoMenu();
    }

    public void bluetoothPressed(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.inflate(R.menu.menu_bluetooth);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: tchelicon.com.blenderappandroid.MixActivity.6
            @Override // android.support.v7.widget.PopupMenu.OnMenuItemClickListener
            public boolean onMenuItemClick(MenuItem menuItem) {
                MixActivity.this.menuSelected(menuItem);
                return true;
            }
        });
        tintMenuItems(popupMenu, R.id.disconnect_menu);
        MenuPopupHelper menuPopupHelper = new MenuPopupHelper(this, (MenuBuilder) popupMenu.getMenu(), view);
        menuPopupHelper.setForceShowIcon(true);
        menuPopupHelper.show();
    }

    public void menuSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.add_a_friend_menu /* 2131296294 */:
                connectPressed();
                FlurryAnalytics.analytics("Menu", "Selected", "Add a Friend");
                break;
            case R.id.disconnect_menu /* 2131296370 */:
                disconnectPressed();
                FlurryAnalytics.analytics("Menu", "Selected", "disconnect bluetooth");
                break;
            case R.id.info_menu /* 2131296408 */:
                showInfoMenu();
                FlurryAnalytics.analytics("Menu", "Selected", "Info");
                break;
            case R.id.load_preset_menu /* 2131296427 */:
                showLoadPreset();
                FlurryAnalytics.analytics("Menu", "Selected", "Load Preset");
                break;
            case R.id.reset_to_default_menu /* 2131296472 */:
                this.appState.resetToDefault();
                FlurryAnalytics.analytics("Menu", "Selected", "Reset to default");
                break;
            case R.id.save_preset_menu /* 2131296482 */:
                FlurryAnalytics.analytics("Preset", "Details", "Saved Preset");
                this.appState.savePreset(this);
                break;
        }
    }

    public void showLoadPreset() {
        ImageButton imageButton = (ImageButton) findViewById(R.id.menu_button);
        this.loadPresetView = getLayoutInflater().inflate(R.layout.content_load_preset, (ViewGroup) null, false);
        this.loadPresetView.setBackgroundResource(R.color.popupBackground);
        this.isPresetEditMode = false;
        ImageButton imageButton2 = (ImageButton) this.loadPresetView.findViewById(R.id.preset_delete_button);
        imageButton2.setVisibility(4);
        imageButton2.setImageTintList(ContextCompat.getColorStateList(this, R.color.mainTint));
        ((Button) this.loadPresetView.findViewById(R.id.preset_edit_done_button)).setTextColor(ContextCompat.getColorStateList(this, R.color.mainTint));
        float f = getResources().getDisplayMetrics().density;
        ListView listView = (ListView) this.loadPresetView.findViewById(R.id.preset_list_view);
        int i = (int) ((22.0f * f) + 0.5f);
        listView.setPadding(i, 0, i, i);
        listView.setWillNotDraw(false);
        listView.setFocusableInTouchMode(true);
        listView.setClickable(true);
        listView.setAdapter((ListAdapter) new PresetAdapter(this));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: tchelicon.com.blenderappandroid.MixActivity.7
            @Override // android.widget.AdapterView.OnItemClickListener
            public void onItemClick(AdapterView<?> adapterView, View view, int i2, long j) {
                if (MixActivity.this.isPresetEditMode) {
                    return;
                }
                MixActivity.this.appState.loadPreset(adapterView.getContext(), ((PresetAdapter) ((ListView) MixActivity.this.loadPresetView.findViewById(R.id.preset_list_view)).getAdapter()).fileNames[i2]);
                FlurryAnalytics.analytics("Preset", "Details", "Load Preset");
            }
        });
        int i2 = (int) ((456.0f * f) + 0.5f);
        int[] iArr = new int[2];
        imageButton.getLocationOnScreen(iArr);
        iArr[0] = iArr[0] + ((int) ((f * 50.0f) + 0.5f));
        iArr[1] = iArr[1] - (i2 / 2);
        this.popupWindow = new PopupWindow(this.loadPresetView, (int) ((280.0f * f) + 0.5f), i2, false);
        this.popupWindow.setTouchable(true);
        this.popupWindow.setFocusable(true);
        this.popupWindow.setOutsideTouchable(true);
        this.popupWindow.setElevation(10.0f);
        this.popupWindow.setBackgroundDrawable(new ColorDrawable(-1));
        this.popupWindow.showAtLocation(imageButton, 51, iArr[0], iArr[1]);
    }

    public void presetDeletePressed(View view) {
        PresetAdapter presetAdapter = (PresetAdapter) ((ListView) this.loadPresetView.findViewById(R.id.preset_list_view)).getAdapter();
        ArrayList arrayList = new ArrayList();
        int i = 0;
        for (boolean z : presetAdapter.selected) {
            if (z) {
                arrayList.add(presetAdapter.fileNames[i]);
            }
            i++;
        }
        if (arrayList.size() > 0) {
            FileSystem fileSystem = new FileSystem();
            Iterator it = arrayList.iterator();
            while (it.hasNext()) {
                fileSystem.deletePreset(this, (String) it.next());
            }
            presetAdapter.setupData(this);
            presetAdapter.notifyDataSetChanged();
        }
    }

    public void presetEditDonePressed(View view) {
        this.isPresetEditMode = !this.isPresetEditMode;
        PresetAdapter presetAdapter = (PresetAdapter) ((ListView) this.loadPresetView.findViewById(R.id.preset_list_view)).getAdapter();
        presetAdapter.setEditMode(this.isPresetEditMode);
        presetAdapter.notifyDataSetChanged();
        ImageButton imageButton = (ImageButton) this.loadPresetView.findViewById(R.id.preset_delete_button);
        if (this.isPresetEditMode) {
            imageButton.setVisibility(0);
            ((TextView) view).setText("DONE");
        } else {
            imageButton.setVisibility(4);
            ((TextView) view).setText("EDIT");
        }
    }

    public void showInfoMenu() {
        View viewInflate = getLayoutInflater().inflate(R.layout.bottom_sheet_grid, (ViewGroup) null);
        this.infoMenu = new BottomSheetDialog(this);
        this.infoMenu.setContentView(viewInflate);
        this.infoMenu.show();
    }

    public void tutorialPressed(View view) {
        FlurryAnalytics.analytics("Info", "Selected", "Youtube Tutorial");
        openWebpage(Constants.outlinkTutorial);
    }

    public void manualPressed(View view) {
        FlurryAnalytics.analytics("Info", "Selected", "Manual");
        openWebpage(Constants.outlinkManual);
    }

    public void tcInfoPressed(View view) {
        FlurryAnalytics.analytics("Info", "Selected", "Website");
        openWebpage(Constants.outlinkWebsite);
    }

    public void facebookPressed(View view) {
        FlurryAnalytics.analytics("Info", "Selected", "Facebook");
        openWebpage(Constants.outlinkFacebook);
    }

    public void instagramPressed(View view) {
        FlurryAnalytics.analytics("Info", "Selected", "Instagram");
        openWebpage(Constants.outlinkInstagram);
    }

    public void twitterPressed(View view) {
        FlurryAnalytics.analytics("Info", "Selected", "Twitter");
        openWebpage(Constants.outlinkTwitter);
    }

    public void openWebpage(String str) {
        startActivity(new Intent("android.intent.action.VIEW", Uri.parse(str)));
    }

    public void disconnectPressed() {
        if (this.appState.connectionState == ConnectionState.central) {
            this.central.stopScan();
            this.central.disconnectEverything();
        } else if (this.appState.connectionState == ConnectionState.peripheral) {
            this.peripheral.stopAdvertising();
            this.appState.appDetailChanges.add(new Tuple(Constants.ParameterID.disconnectPeripheral.getId(), (byte) 0, (byte) 0));
        }
    }

    public void tintMenuItems(PopupMenu popupMenu, int i) {
        MenuItem menuItemFindItem = popupMenu.getMenu().findItem(i);
        Drawable drawableWrap = DrawableCompat.wrap(menuItemFindItem.getIcon());
        DrawableCompat.setTintList(drawableWrap, ResourcesCompat.getColorStateList(getResources(), R.color.mainTint, getTheme()));
        menuItemFindItem.setIcon(drawableWrap);
        SpannableString spannableString = new SpannableString(menuItemFindItem.getTitle().toString());
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.mainTint)), 0, spannableString.length(), 33);
        menuItemFindItem.setTitle(spannableString);
    }

    public void updateToValues() {
        MixController mixController = (MixController) findViewById(R.id.mixController);
        if (mixController != null) {
            mixController.updateToValues();
        } else {
            Log.d(TAG, "updateToValues() mixController is null");
        }
        setMenuColours();
    }

    public void setMenuColours() {
        ((ImageButton) findViewById(R.id.menu_button)).setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.c13));
        ((ImageButton) findViewById(R.id.bluetooth_button)).setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.c13));
    }

    public void setProgressText(String str) {
        TextView textView = (TextView) findViewById(R.id.progressText);
        if (textView != null) {
            textView.setText(str);
        }
    }
}
