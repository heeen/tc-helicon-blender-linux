package tchelicon.com.blenderappandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.UUID;
import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class Peripheral {
    private static final String TAG = "Peripheral";
    private static Peripheral instance = new Peripheral();
    public AppState appState;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BluetoothManager bluetoothManager;
    private ArrayList<BluetoothDevice> connectedDevices;
    private Context context;
    private BluetoothGattServer gattServer;
    BLECommandQueue commandQueue = new BLECommandQueue();
    private Handler mHandler = new Handler();
    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() { // from class: tchelicon.com.blenderappandroid.Peripheral.1
        @Override // android.bluetooth.BluetoothGattServerCallback
        public void onConnectionStateChange(BluetoothDevice bluetoothDevice, int i, int i2) {
            super.onConnectionStateChange(bluetoothDevice, i, i2);
            if (i != 0) {
                if (i == 257) {
                    FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "onConnectionStateChange GATT_FAILURE");
                    return;
                }
                String str = "Error:" + i;
                return;
            }
            if (i2 == 2) {
                Peripheral.this.connectedDevices.add(bluetoothDevice);
                Peripheral.this.appState.setConnectionStatus(ConnectionState.peripheral);
            } else if (i2 == 0) {
                Peripheral.this.gattServer.close();
                Peripheral.this.connectedDevices.remove(bluetoothDevice);
                if (Peripheral.this.connectedDevices.size() == 0) {
                    Peripheral.this.appState.setConnectionStatus(ConnectionState.notConnected);
                    Peripheral.this.appState.setHasBlender(false);
                }
            }
        }

        @Override // android.bluetooth.BluetoothGattServerCallback
        public void onCharacteristicReadRequest(BluetoothDevice bluetoothDevice, int i, int i2, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
            byte[] bArr;
            super.onCharacteristicReadRequest(bluetoothDevice, i, i2, bluetoothGattCharacteristic);
            if (BluetoothConstants.ParameterCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                bArr = new byte[0];
            } else if (BluetoothConstants.ParameterCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                bArr = new byte[0];
            } else {
                bArr = new byte[0];
            }
            Peripheral.this.gattServer.sendResponse(bluetoothDevice, i, 0, i2, bArr);
        }

        @Override // android.bluetooth.BluetoothGattServerCallback
        public void onCharacteristicWriteRequest(BluetoothDevice bluetoothDevice, int i, BluetoothGattCharacteristic bluetoothGattCharacteristic, boolean z, boolean z2, int i2, byte[] bArr) {
            super.onCharacteristicWriteRequest(bluetoothDevice, i, bluetoothGattCharacteristic, z, z2, i2, bArr);
            Log.d(Peripheral.TAG, "onCharacteristicWriteRequest");
            try {
                if (BluetoothConstants.ParameterCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                    Log.d(Peripheral.TAG, "WRITE request for ParameterCharacteristic");
                    Peripheral.this.appState.processParameter(bArr, true);
                    Peripheral.this.appState.appStateCallback.callbackUpdateToValues();
                } else if (BluetoothConstants.AppDetailCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                    Log.d(Peripheral.TAG, "WRITE request for AppDetailCharacteristic");
                    Peripheral.this.appState.processAppDetails(bArr, true);
                    Peripheral.this.appState.appStateCallback.callbackUpdateToValues();
                }
            } finally {
                if (z2) {
                    Peripheral.this.gattServer.sendResponse(bluetoothDevice, i, 0, i2, bArr);
                }
            }
        }

        @Override // android.bluetooth.BluetoothGattServerCallback
        public void onDescriptorWriteRequest(BluetoothDevice bluetoothDevice, int i, BluetoothGattDescriptor bluetoothGattDescriptor, boolean z, boolean z2, int i2, byte[] bArr) {
            if (z2) {
                Peripheral.this.gattServer.sendResponse(bluetoothDevice, i, 0, i2, bArr);
            }
        }
    };
    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() { // from class: tchelicon.com.blenderappandroid.Peripheral.2
        @Override // android.bluetooth.le.AdvertiseCallback
        public void onStartSuccess(AdvertiseSettings advertiseSettings) {
            super.onStartSuccess(advertiseSettings);
            Log.d(Peripheral.TAG, "Peripheral Advertise Started.");
            Peripheral.this.appState.setAdvertising(true);
        }

        @Override // android.bluetooth.le.AdvertiseCallback
        public void onStartFailure(int i) {
            String str;
            String str2;
            super.onStartFailure(i);
            switch (i) {
                case 1:
                    str = "Bluetooth failed to start error: " + i;
                    str2 = "ADVERTISE_FAILED_DATA_TOO_LARGE";
                    break;
                case 2:
                    str = "Advertising failed, too many advertisers";
                    str2 = "ADVERTISE_FAILED_TOO_MANY_ADVERTISERS";
                    break;
                case 3:
                    str = "Failed Advertising already started";
                    str2 = "ADVERTISE_FAILED_ALREADY_STARTED";
                    break;
                case 4:
                    str = "Advertising failed internal error";
                    str2 = "ADVERTISE_FAILED_INTERNAL_ERROR";
                    break;
                case 5:
                    str = "Advertising is not supported on this device";
                    str2 = "ADVERTISE_FAILED_FEATURE_UNSUPPORTED";
                    break;
                default:
                    str = "Advertising failed";
                    str2 = "ADVERTISE_FAILED_UNCAUGHT_EXCEPTION";
                    break;
            }
            Log.d(Peripheral.TAG, str2);
            Peripheral.this.postStatusMessage(str);
            FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "advertiseCallback onStartFailure error: " + str2);
        }
    };

    public void sendAppDetailChanges() {
    }

    private Peripheral() {
    }

    public static Peripheral getInstance() {
        if (instance == null) {
            synchronized (Peripheral.class) {
                if (instance == null) {
                    instance = new Peripheral();
                }
            }
        }
        return instance;
    }

    public void init(Context context, AppState appState) {
        if (context == null) {
            Log.d(TAG, "Init error Context cannot be null!!");
            FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "init context null");
            return;
        }
        if (appState == null) {
            Log.d(TAG, "Invalid appState!");
            FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "init appState null");
            return;
        }
        this.context = context;
        this.appState = appState;
        this.connectedDevices = new ArrayList<>();
        this.bluetoothManager = (BluetoothManager) context.getSystemService("bluetooth");
        this.bluetoothAdapter = this.bluetoothManager.getAdapter();
        if (!context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            Log.d(TAG, "Bluetooth LE is not supported in this devices!!");
            postStatusMessage("Bluetooth LE may not be supported on this device");
            FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "init BLE feature not supported");
        } else if (this.bluetoothAdapter == null || !this.bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth not supported in this device!!");
            postStatusMessage("Bluetooth LE may not be supported on this device");
            FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "init bluetoothAdapter null, or not enabled");
        }
    }

    public void disconnect() {
        Log.d(TAG, "disconnect");
        stopAdvertising();
        this.appState.appDetailChanges.add(new Tuple(Constants.ParameterID.disconnectPeripheral.getId(), (byte) 0, (byte) 0));
        this.appState.setHasBlender(false);
    }

    private void initService() {
        this.gattServer = this.bluetoothManager.openGattServer(this.context, this.gattServerCallback);
        BluetoothGattService bluetoothGattService = new BluetoothGattService(BluetoothConstants.ParameterService_UUID, 0);
        BluetoothGattCharacteristic bluetoothGattCharacteristic = new BluetoothGattCharacteristic(BluetoothConstants.ParameterCharacteristic_UUID, 26, 17);
        BluetoothGattDescriptor bluetoothGattDescriptor = new BluetoothGattDescriptor(UUID.fromString("00002902-0000-1000-8000-00805F9B34FB"), 17);
        bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        bluetoothGattCharacteristic.addDescriptor(bluetoothGattDescriptor);
        bluetoothGattCharacteristic.setWriteType(16);
        bluetoothGattService.addCharacteristic(bluetoothGattCharacteristic);
        this.gattServer.addService(bluetoothGattService);
    }

    private void advertiseService() {
        this.bluetoothLeAdvertiser.startAdvertising(new AdvertiseSettings.Builder().setAdvertiseMode(2).setTxPowerLevel(3).setConnectable(true).setTimeout(0).build(), new AdvertiseData.Builder().addServiceUuid(new ParcelUuid(BluetoothConstants.ParameterService_UUID)).build(), this.advertiseCallback);
    }

    public void startAdvertising() {
        Log.d(TAG, "startAdvertising");
        if (!this.bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Log.d(TAG, "Start Advertising: MultipleAdvertisement Peripheral mode is not supported in this device");
            postStatusMessage("Peripheral mode is not supported in this device");
            FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "startAdvertising Peripheral mode not supported");
            return;
        }
        if (this.bluetoothLeAdvertiser == null) {
            if (this.bluetoothAdapter == null) {
                Log.d(TAG, "Start Advertising: bluetoothAdapter is null");
                postStatusMessage("Error bluetooth failed");
                FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "startAdvertising bluetoothAdapter null");
            }
            this.bluetoothLeAdvertiser = this.bluetoothAdapter.getBluetoothLeAdvertiser();
            if (this.bluetoothLeAdvertiser == null) {
                Log.d(TAG, "Start Advertising: Peripheral mode is not supported in this device");
                postStatusMessage("Error bluetooth failed");
                FlurryAnalytics.analytics("Bluetooth", "Peripheral Errors", "startAdvertising bluetoothLeAdvertiser null");
                return;
            }
        }
        initService();
        advertiseService();
    }

    public void stopAdvertising() {
        if (this.bluetoothLeAdvertiser == null) {
            return;
        }
        this.bluetoothLeAdvertiser.stopAdvertising(this.advertiseCallback);
        this.appState.setAdvertising(false);
        if (this.appState.hasBlender.booleanValue()) {
            FlurryAnalytics.analytics("Bluetooth", TAG, "Join a network success");
        } else {
            FlurryAnalytics.analytics("Bluetooth", TAG, "Join a network failure");
        }
    }

    public void sendChanges() {
        if (this.appState.sendingChanges.booleanValue() || this.connectedDevices.size() == 0) {
            return;
        }
        this.appState.sendingChanges = true;
        if (this.appState.parameterChanges.size() > 0) {
            Log.d(TAG, "Sending ParameterChanges");
            this.appState.parameterChangesSnapShot = new ArrayList(this.appState.parameterChanges);
            this.appState.parameterChanges.clear();
            sendParameterChanges();
        }
        if (this.appState.advertising.booleanValue() && this.appState.waitingForBlenderState.booleanValue()) {
            Log.d(TAG, "requestBlenderStateFromCentral because waitingForBlenderState");
            if (this.appState.requestBlenderStateCount == 0) {
                this.appState.requestBlenderStateFromCentral();
                this.appState.requestBlenderStateCount = this.appState.requestBlenderStateFloodStopCount;
            } else {
                this.appState.requestBlenderStateCount--;
            }
        }
        if (this.appState.appDetailChanges.size() > 0) {
            Log.d(TAG, "Sending appDetailChanges");
            this.appState.appDetailChangesSnapShot = new ArrayList(this.appState.appDetailChanges);
            this.appState.appDetailChanges.clear();
            sendAppDetailChanges();
        }
        this.appState.sendingChanges = false;
    }

    public void sendParameterChanges() {
        Log.d(TAG, "SendParameterChanges");
        byte[] bArrGenerateParameterChangesData = this.appState.generateParameterChangesData();
        if (bArrGenerateParameterChangesData.length == 0) {
            Log.d(TAG, "PARAMETER DATA EMPTY");
            return;
        }
        if (this.gattServer == null) {
            Log.d(TAG, "sendParameterChanges gattServer null");
            return;
        }
        if (Constants.broadcastToOneAtATime) {
            for (BluetoothDevice bluetoothDevice : this.connectedDevices) {
                BluetoothGattService service = this.gattServer.getService(BluetoothConstants.ParameterService_UUID);
                if (service == null) {
                    Log.d(TAG, "sendParameterChanges ParameterService null");
                    return;
                }
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(BluetoothConstants.ParameterCharacteristic_UUID);
                if (characteristic != null) {
                    characteristic.setValue(bArrGenerateParameterChangesData);
                    if (!this.gattServer.notifyCharacteristicChanged(bluetoothDevice, characteristic, true)) {
                        Log.d(TAG, "sendParameterChanges sending was false");
                    }
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void postStatusMessage(final String str) {
        this.mHandler.post(new Runnable() { // from class: tchelicon.com.blenderappandroid.Peripheral.3
            @Override // java.lang.Runnable
            public void run() {
                Toast.makeText(Peripheral.this.context, str, 0).show();
            }
        });
    }
}
