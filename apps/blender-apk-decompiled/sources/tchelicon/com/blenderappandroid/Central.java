package tchelicon.com.blenderappandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import tchelicon.com.blenderappandroid.BLECommand;
import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class Central {
    private static final String TAG = "Central";
    private static Central instance = new Central();
    public AppState appState;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private Context context;
    public List<PeripheralDevice> peripherals = new ArrayList();
    BLECommandQueue commandQueue = new BLECommandQueue();
    private int parameterReSendAttempts = 0;
    private int appDetailReSendAttempts = 0;
    private int attempsPerDevice = 1;
    private ScanCallback scanCallback = new ScanCallback() { // from class: tchelicon.com.blenderappandroid.Central.1
        @Override // android.bluetooth.le.ScanCallback
        public void onScanResult(int i, final ScanResult scanResult) {
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: tchelicon.com.blenderappandroid.Central.1.1
                @Override // java.lang.Runnable
                public void run() {
                    processScanResult(scanResult);
                }
            });
        }

        @Override // android.bluetooth.le.ScanCallback
        public void onBatchScanResults(final List<ScanResult> list) {
            new Handler(Looper.getMainLooper()).post(new Runnable() { // from class: tchelicon.com.blenderappandroid.Central.1.2
                @Override // java.lang.Runnable
                public void run() {
                    Iterator it = list.iterator();
                    while (it.hasNext()) {
                        processScanResult((ScanResult) it.next());
                    }
                }
            });
        }

        @Override // android.bluetooth.le.ScanCallback
        public void onScanFailed(int i) {
            Log.w(Central.TAG, "LE Scan Failed: " + i);
            if (i == 4) {
                Toast.makeText(Central.this.context, "Scanning unsupported on this device", 0).show();
                FlurryAnalytics.analytics("Bluetooth", "Central Errors", "scan failed feature unsupported");
            } else if (i == 3) {
                Toast.makeText(Central.this.context, "Scanning failed internal error", 0).show();
                FlurryAnalytics.analytics("Bluetooth", "Central Errors", "scan failed internal error");
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public void processScanResult(ScanResult scanResult) {
            BluetoothDevice device = scanResult.getDevice();
            Log.d(Central.TAG, "scan found " + device.getAddress());
            for (PeripheralDevice peripheralDevice : Central.this.peripherals) {
                if (peripheralDevice.device.getAddress().equals(device.getAddress())) {
                    if (peripheralDevice.hasConnected) {
                        return;
                    }
                    Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.connect, peripheralDevice));
                    return;
                }
            }
            PeripheralDevice peripheralDevice2 = new PeripheralDevice(device);
            Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.connect, peripheralDevice2));
            Central.this.peripherals.add(peripheralDevice2);
        }
    };
    private BLECommandQueueCallback commandQueueCallback = new BLECommandQueueCallback() { // from class: tchelicon.com.blenderappandroid.Central.2
        @Override // tchelicon.com.blenderappandroid.BLECommandQueueCallback
        public void executeCommand(BLECommand bLECommand) {
            if (bLECommand.peripheral == null) {
                Log.d(Central.TAG, "commandQueueCallback peripheral was null");
                Central.this.commandQueue.commandCompleted();
            } else if (bLECommand.peripheral.gatt == null) {
                Log.d(Central.TAG, "commandQueueCallback gatt was null");
                Central.this.commandQueue.commandCompleted();
            }
            switch (AnonymousClass4.$SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[bLECommand.command.ordinal()]) {
                case 1:
                    if (!bLECommand.peripheral.hasConnected) {
                        bLECommand.peripheral.gatt = bLECommand.peripheral.device.connectGatt(Central.this.context, false, Central.this.gattCallback, 2);
                    } else {
                        Log.d(Central.TAG, "Connection ignore already connected");
                        Central.this.commandQueue.commandCompleted();
                    }
                    break;
                case 2:
                    if (bLECommand.peripheral.hasDiscoveredServices) {
                        Log.d(Central.TAG, "Discover ignore already discovered");
                        Central.this.commandQueue.commandCompleted();
                    } else {
                        bLECommand.peripheral.gatt.discoverServices();
                        Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.requestMTUSize, bLECommand.peripheral));
                    }
                    break;
                case 4:
                    if (bLECommand.descriptor != null) {
                        Log.d(Central.TAG, "writeDescriptionResult = " + bLECommand.peripheral.gatt.writeDescriptor(bLECommand.descriptor));
                    }
                    break;
                case 5:
                    Central.this.disconnectPeripheral(bLECommand.peripheral);
                    break;
                case 6:
                    Log.d(Central.TAG, "requestMTUSize result " + bLECommand.peripheral.gatt.requestMtu(180));
                    break;
                case 7:
                    Central.this.sendParameterChanges(bLECommand.peripheral);
                    break;
                case 8:
                    Central.this.sendAppDetailChanges(bLECommand.peripheral);
                    break;
                case 9:
                    if (bLECommand.peripheral == null || bLECommand.peripheral.gatt == null) {
                        Log.d(Central.TAG, "requestConnectionPriority peripheral or gatt was null");
                    } else {
                        Log.d(Central.TAG, "requestConnectionPriority result = " + Boolean.toString(Boolean.valueOf(bLECommand.peripheral.gatt.requestConnectionPriority(1)).booleanValue()));
                    }
                    Central.this.commandQueue.commandCompleted();
                    break;
            }
        }
    };
    public BluetoothGattCallback gattCallback = new BluetoothGattCallback() { // from class: tchelicon.com.blenderappandroid.Central.3
        @Override // android.bluetooth.BluetoothGattCallback
        public void onConnectionStateChange(BluetoothGatt bluetoothGatt, int i, int i2) {
            super.onConnectionStateChange(bluetoothGatt, i, i2);
            Log.d(Central.TAG, "onConnectionStateChange status = " + Integer.toString(i) + ", newState = " + Integer.toString(i2));
            if (i == 0) {
                Log.d(Central.TAG, "status change GATT_SUCCESS");
            } else if (i == 257) {
                Log.d(Central.TAG, "status change GATT_FAILURE");
                FlurryAnalytics.analytics("Bluetooth", "Central Errors", "onConnectionStateChange GATT_FAILURE " + Central.this.commandQueue.lastCommandAttemptedToSend.command.name());
            } else if (i == 143) {
                Log.d(Central.TAG, "status change GATT_CONNECTION_CONGESTED");
            } else {
                Log.d(Central.TAG, "status change UN-Handled status = " + Integer.toString(i));
            }
            if (i2 == 2) {
                Log.d(Central.TAG, "calling setConnectionStatus central");
                Central.this.appState.setConnectionStatus(ConnectionState.central);
                int indexOfPeripheral = Central.this.getIndexOfPeripheral(bluetoothGatt.getDevice().getAddress());
                if (indexOfPeripheral != -1) {
                    Central.this.peripherals.get(indexOfPeripheral).hasConnected = true;
                    Central.this.peripherals.get(indexOfPeripheral).discoveryAttemptsCount++;
                    Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.requestConnectionPriority, Central.this.peripherals.get(indexOfPeripheral)));
                    Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.discover, Central.this.peripherals.get(indexOfPeripheral)));
                }
            } else if (i2 == 0) {
                Log.d(Central.TAG, "STATE_DISCONNECTED");
                String address = bluetoothGatt.getDevice().getAddress();
                bluetoothGatt.close();
                Central.this.removePeripheralFromPeripherals(address);
            } else if (i2 == 1) {
                Log.d(Central.TAG, "State change STATE_CONNECTING");
            } else if (i2 == 3) {
                Log.d(Central.TAG, "State change STATE_DISCONNECTING");
            } else {
                Log.d(Central.TAG, "State change Unhandled state = " + Integer.toString(i2));
            }
            Central.this.commandQueue.commandCompleted();
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onServicesDiscovered(BluetoothGatt bluetoothGatt, int i) {
            super.onServicesDiscovered(bluetoothGatt, i);
            int indexOfPeripheral = Central.this.getIndexOfPeripheral(bluetoothGatt.getDevice().getAddress());
            Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.requestConnectionPriority, Central.this.peripherals.get(indexOfPeripheral)));
            if (i == 0) {
                Log.d(Central.TAG, "onServicesDiscovered GATT_SUCCESS");
                if (indexOfPeripheral != -1) {
                    Central.this.peripherals.get(indexOfPeripheral).hasDiscoveredServices = true;
                }
            } else if (i == 257) {
                FlurryAnalytics.analytics("Bluetooth", "Central Errors", "onServicesDiscovered GATT_FAILURE");
                Log.d(Central.TAG, "onServicesDiscovered GATT_FAILURE");
                if (indexOfPeripheral != -1) {
                    Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.discover, Central.this.peripherals.get(indexOfPeripheral)));
                    return;
                }
                return;
            }
            for (BluetoothGattService bluetoothGattService : bluetoothGatt.getServices()) {
                if (BluetoothConstants.ParameterService_UUID.equals(bluetoothGattService.getUuid())) {
                    Log.d(Central.TAG, "onServicesDiscovered ParameterService_UUID");
                    BluetoothGattCharacteristic characteristic = bluetoothGattService.getCharacteristic(BluetoothConstants.ParameterCharacteristic_UUID);
                    Log.d(Central.TAG, "onServicesDiscovered ParameterService_UUID setCharacteristicNotificationResult = " + bluetoothGatt.setCharacteristicNotification(characteristic, true));
                    if (indexOfPeripheral != -1) {
                        for (BluetoothGattDescriptor bluetoothGattDescriptor : characteristic.getDescriptors()) {
                            Log.d(Central.TAG, "Parameter characteristic descriptor.setValue result " + bluetoothGattDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
                            Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.writeDescriptor, Central.this.peripherals.get(indexOfPeripheral), bluetoothGattDescriptor));
                        }
                    }
                    characteristic.setWriteType(2);
                    Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.requestMTUSize, Central.this.peripherals.get(indexOfPeripheral)));
                    if (Central.this.appState.scanningForMore.booleanValue()) {
                        Central.this.appState.setScanForMore(false);
                    }
                } else if (BluetoothConstants.AppDetailService_UUID.equals(bluetoothGattService.getUuid())) {
                    Log.d(Central.TAG, "onServicesDiscovered AppDetailService_UUID");
                    BluetoothGattCharacteristic characteristic2 = bluetoothGattService.getCharacteristic(BluetoothConstants.AppDetailCharacteristic_UUID);
                    Log.d(Central.TAG, "onServicesDiscovered AppDetailService_UUID setCharacteristicNotificationResult = " + bluetoothGatt.setCharacteristicNotification(characteristic2, true));
                    if (indexOfPeripheral != -1) {
                        for (BluetoothGattDescriptor bluetoothGattDescriptor2 : characteristic2.getDescriptors()) {
                            Log.d(Central.TAG, "AppDetail characteristic descriptor.setValue result " + bluetoothGattDescriptor2.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
                            Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.writeDescriptor, Central.this.peripherals.get(indexOfPeripheral), bluetoothGattDescriptor2));
                        }
                    }
                    characteristic2.setWriteType(2);
                    Central.this.appState.broadcastAppDetailState();
                }
            }
            Central.this.commandQueue.commandCompleted();
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onCharacteristicRead(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {
            super.onCharacteristicRead(bluetoothGatt, bluetoothGattCharacteristic, i);
            if (i == 0) {
                Log.d(Central.TAG, "onCharacteristicRead: gatt success");
            } else {
                Log.d(Central.TAG, "onCharacteristicRead error: " + i);
            }
            if (BluetoothConstants.ParameterCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
            }
            if (BluetoothConstants.AppDetailCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
            }
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onCharacteristicWrite(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic, int i) {
            super.onCharacteristicWrite(bluetoothGatt, bluetoothGattCharacteristic, i);
            if (BluetoothConstants.ParameterCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                if (i == 0) {
                    Log.d(Central.TAG, "On Write Parameter GATT_SUCCESS");
                } else {
                    Log.d(Central.TAG, "On Write Parameter GATT_FAILURE, status = " + i);
                }
                Central.this.commandQueue.commandCompleted();
            }
            if (BluetoothConstants.AppDetailCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                if (i != 0) {
                    Log.d(Central.TAG, "On Write AppDetail GATT_FAILURE, status = " + i);
                }
                Central.this.commandQueue.commandCompleted();
            }
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onReliableWriteCompleted(BluetoothGatt bluetoothGatt, int i) {
            super.onReliableWriteCompleted(bluetoothGatt, i);
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onReadRemoteRssi(BluetoothGatt bluetoothGatt, int i, int i2) {
            super.onReadRemoteRssi(bluetoothGatt, i, i2);
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onDescriptorWrite(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor bluetoothGattDescriptor, int i) {
            super.onDescriptorWrite(bluetoothGatt, bluetoothGattDescriptor, i);
            if (i == 0) {
                Log.d(Central.TAG, "onDescriptorWrite: Wrote GATT Descriptor successfully.");
            } else {
                Log.d(Central.TAG, "onDescriptorWrite: Error writing GATT Descriptor: " + i);
            }
            Central.this.commandQueue.commandCompleted();
            if (Central.this.bluetoothAdapter == null) {
                Log.d(Central.TAG, "onDescriptorWrite: BluetoothAdapter not initialized");
            } else if (bluetoothGatt == null) {
                Log.d(Central.TAG, "onDescriptorWrite: gatt not initialized");
            }
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onDescriptorRead(BluetoothGatt bluetoothGatt, BluetoothGattDescriptor bluetoothGattDescriptor, int i) {
            super.onDescriptorRead(bluetoothGatt, bluetoothGattDescriptor, i);
            if (i == 0) {
                Log.d(Central.TAG, "onDescriptorRead: Gatt success");
            } else {
                Log.d(Central.TAG, "onDescriptorRead: error code " + i);
            }
            if (Central.this.bluetoothAdapter == null) {
                Log.d(Central.TAG, "onDescriptorRead: BluetoothAdapter not initialized");
            } else if (bluetoothGatt == null) {
                Log.d(Central.TAG, "onDescriptorRead: gatt not initialized");
            }
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onCharacteristicChanged(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
            super.onCharacteristicChanged(bluetoothGatt, bluetoothGattCharacteristic);
            Log.d(Central.TAG, "\n\n\nNotification of message characteristic changed on server.");
            if (BluetoothConstants.ParameterCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                ArrayList arrayList = new ArrayList();
                byte[] value = bluetoothGattCharacteristic.getValue();
                int length = value.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    arrayList.add(Byte.valueOf(value[i]));
                    if (arrayList.size() == 3) {
                        Log.d(Central.TAG, "received (" + arrayList.get(0) + "," + arrayList.get(1) + "," + arrayList.get(2) + ")");
                        if (((Byte) arrayList.get(0)).byteValue() == Constants.ParameterID.isBlender.getId() && ((Byte) arrayList.get(1)).byteValue() == 0 && ((Byte) arrayList.get(2)).byteValue() == 0) {
                            Log.d(Central.TAG, "received isBlender");
                            Central.this.appState.setHasBlender(true);
                            if (Central.this.appState.waitingForBlenderState.booleanValue()) {
                                Central.this.appState.waitingForBlenderState = false;
                                Central.this.appState.requestBlenderState();
                            }
                            for (PeripheralDevice peripheralDevice : Central.this.peripherals) {
                                if (peripheralDevice.device.getAddress().equals(bluetoothGatt.getDevice().getAddress())) {
                                    peripheralDevice.isBlender = true;
                                }
                            }
                        } else {
                            arrayList.clear();
                        }
                    }
                    i++;
                }
                Central.this.appState.processParameter(bluetoothGattCharacteristic.getValue(), true);
                Central.this.appState.appStateCallback.callbackUpdateToValues();
                return;
            }
            if (BluetoothConstants.AppDetailCharacteristic_UUID.equals(bluetoothGattCharacteristic.getUuid())) {
                ArrayList arrayList2 = new ArrayList();
                byte[] value2 = bluetoothGattCharacteristic.getValue();
                int length2 = value2.length;
                int i2 = 0;
                while (true) {
                    if (i2 >= length2) {
                        break;
                    }
                    arrayList2.add(Byte.valueOf(value2[i2]));
                    if (arrayList2.size() == 3) {
                        if (((Byte) arrayList2.get(0)).byteValue() == Constants.ParameterID.disconnectPeripheral.getId() && ((Byte) arrayList2.get(1)).byteValue() == 0 && ((Byte) arrayList2.get(2)).byteValue() == 0) {
                            Iterator<PeripheralDevice> it = Central.this.peripherals.iterator();
                            while (true) {
                                if (!it.hasNext()) {
                                    break;
                                }
                                PeripheralDevice next = it.next();
                                if (next.device.getAddress().equals(bluetoothGatt.getDevice().getAddress())) {
                                    Log.d(Central.TAG, "onCharacteristicChanged appDetail received Disconnect command");
                                    Central.this.commandQueue.queue(new BLECommand(BLECommand.Command.disconnect, next));
                                    break;
                                }
                            }
                        } else {
                            arrayList2.clear();
                        }
                    }
                    i2++;
                }
                Central.this.appState.processAppDetails(bluetoothGattCharacteristic.getValue(), true);
                Central.this.appState.appStateCallback.callbackUpdateToValues();
            }
        }

        @Override // android.bluetooth.BluetoothGattCallback
        public void onMtuChanged(BluetoothGatt bluetoothGatt, int i, int i2) {
            String string;
            super.onMtuChanged(bluetoothGatt, i, i2);
            if (i2 == 0) {
                string = "GATT_SUCCESS";
            } else {
                string = i2 == 257 ? "GATT_FAILURE" : Integer.toString(i2);
            }
            Log.d(Central.TAG, "onMtuChanged status = " + string + ", MTU = " + Integer.toString(i));
            Central.this.commandQueue.commandCompleted();
        }
    };

    public void sendAppDetailChanges(PeripheralDevice peripheralDevice) {
    }

    private Central() {
    }

    public static Central getInstance() {
        if (instance == null) {
            synchronized (Central.class) {
                if (instance == null) {
                    instance = new Central();
                }
            }
        }
        return instance;
    }

    public void init(Context context, AppState appState) {
        this.commandQueue.init(this.commandQueueCallback);
        this.commandQueue.start();
        if (context == null) {
            Log.d(TAG, "Invalid Context!");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "init context null");
            return;
        }
        if (appState == null) {
            Log.d(TAG, "Invalid appState!");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "init appState null");
            return;
        }
        this.context = context;
        this.appState = appState;
        this.bluetoothManager = (BluetoothManager) context.getSystemService("bluetooth");
        this.bluetoothAdapter = this.bluetoothManager.getAdapter();
        if (!context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le")) {
            Log.d(TAG, "Bluetooth LE is not supported in this device!");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "init BLE feature not supported");
            Toast.makeText(context, "Bluetooth LE may not be supported on this device", 0).show();
        } else if (this.bluetoothAdapter == null || !this.bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth not supported in this device!!");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "init bluetoothAdapter null, or not enabled");
        }
    }

    public int getIndexOfPeripheral(String str) {
        Iterator<PeripheralDevice> it = this.peripherals.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().device.getAddress().equals(str)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public void startScan() {
        ArrayList arrayList = new ArrayList();
        arrayList.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(BluetoothConstants.ParameterService_UUID)).build());
        ScanSettings scanSettingsBuild = new ScanSettings.Builder().setScanMode(2).build();
        if (this.scanCallback == null) {
            Log.d(TAG, "scanCallback is null");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "start Scan scanCallback null");
        } else if (this.bluetoothAdapter == null) {
            Log.d(TAG, "bluetoothAdapter is NULL");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "start Scan bluetoothAdapter null");
        } else if (this.bluetoothAdapter.getBluetoothLeScanner() != null) {
            this.bluetoothAdapter.getBluetoothLeScanner().startScan(arrayList, scanSettingsBuild, this.scanCallback);
            this.appState.setScanning(true);
        } else {
            Log.d(TAG, "Scanner is NULL!");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "start Scan scanner null");
        }
    }

    public void stopScan() {
        Log.d(TAG, "stopScan");
        if (this.appState.scanningForMore.booleanValue()) {
            FlurryAnalytics.analytics("Bluetooth", TAG, "scan for more failure");
        } else if (this.appState.scanning.booleanValue() && !this.appState.hasBlender.booleanValue()) {
            FlurryAnalytics.analytics("Bluetooth", TAG, "connect to blender failure");
        } else if (this.appState.scanning.booleanValue() && this.appState.hasBlender.booleanValue()) {
            FlurryAnalytics.analytics("Bluetooth", TAG, "connect to blender success");
        }
        this.bluetoothAdapter.getBluetoothLeScanner().stopScan(this.scanCallback);
        this.appState.setScanning(false);
        this.appState.setScanForMore(false);
        if (this.appState.hasBlender.booleanValue()) {
            return;
        }
        disconnectEverything();
    }

    /* JADX INFO: renamed from: tchelicon.com.blenderappandroid.Central$4, reason: invalid class name */
    static /* synthetic */ class AnonymousClass4 {
        static final /* synthetic */ int[] $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command = new int[BLECommand.Command.values().length];

        static {
            try {
                $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[BLECommand.Command.connect.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[BLECommand.Command.discover.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[BLECommand.Command.setCharacteristicNotification.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[BLECommand.Command.writeDescriptor.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
            try {
                $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[BLECommand.Command.disconnect.ordinal()] = 5;
            } catch (NoSuchFieldError unused5) {
            }
            try {
                $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[BLECommand.Command.requestMTUSize.ordinal()] = 6;
            } catch (NoSuchFieldError unused6) {
            }
            try {
                $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[BLECommand.Command.sendParameterChanges.ordinal()] = 7;
            } catch (NoSuchFieldError unused7) {
            }
            try {
                $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[BLECommand.Command.sendAppDetailChanges.ordinal()] = 8;
            } catch (NoSuchFieldError unused8) {
            }
            try {
                $SwitchMap$tchelicon$com$blenderappandroid$BLECommand$Command[BLECommand.Command.requestConnectionPriority.ordinal()] = 9;
            } catch (NoSuchFieldError unused9) {
            }
        }
    }

    public void disconnectEverything() {
        Iterator<PeripheralDevice> it = this.peripherals.iterator();
        while (it.hasNext()) {
            this.commandQueue.queue(new BLECommand(BLECommand.Command.disconnect, it.next()));
        }
    }

    public void disconnectPeripheral(PeripheralDevice peripheralDevice) {
        if (peripheralDevice.gatt != null) {
            peripheralDevice.gatt.disconnect();
            removePeripheralFromPeripherals(peripheralDevice.device.getAddress());
        } else {
            Log.d(TAG, "disconnectPeripheral gatt was null");
        }
    }

    public void removePeripheralFromPeripherals(String str) {
        boolean z = false;
        for (int size = this.peripherals.size() - 1; size >= 0; size--) {
            if (this.peripherals.get(size).device.getAddress().equals(str)) {
                this.peripherals.remove(size);
            } else if (this.peripherals.get(size).isBlender) {
                z = true;
            }
        }
        if (this.peripherals.size() == 0) {
            this.appState.setConnectionStatus(ConnectionState.notConnected);
        }
        if (z) {
            return;
        }
        this.appState.setHasBlender(false);
    }

    public void reconnectDevice() {
        for (BluetoothDevice bluetoothDevice : ((BluetoothManager) this.context.getSystemService("bluetooth")).getConnectedDevices(7)) {
            Log.d(TAG, "reconnectDevice " + bluetoothDevice.getAddress());
            if (bluetoothDevice.getType() == 2) {
                this.commandQueue.queue(new BLECommand(BLECommand.Command.connect, new PeripheralDevice(bluetoothDevice)));
            }
        }
    }

    public void sendChanges() {
        if (this.appState.sendingChanges.booleanValue() || this.peripherals.size() == 0) {
            return;
        }
        if (!this.commandQueue.checkQueueFor(BLECommand.Command.sendParameterChanges) && this.peripherals.size() > 0) {
            if (this.appState.waitingForBlenderState.booleanValue()) {
                Log.d(TAG, "CENTRAL requestBlenderState");
                this.appState.requestBlenderState();
            }
            if (this.appState.parameterChanges.size() > 0) {
                Log.d(TAG, "Sending ParameterChanges");
                this.parameterReSendAttempts = this.attempsPerDevice * this.peripherals.size();
                this.appState.parameterChangesSnapShot = new ArrayList(this.appState.parameterChanges);
                this.appState.parameterChanges.clear();
                Iterator<PeripheralDevice> it = this.peripherals.iterator();
                while (it.hasNext()) {
                    this.commandQueue.queue(new BLECommand(BLECommand.Command.sendParameterChanges, it.next()));
                }
            }
        }
        if (this.appState.appDetailChanges.size() <= 0 || this.commandQueue.checkQueueFor(BLECommand.Command.sendAppDetailChanges)) {
            return;
        }
        this.appDetailReSendAttempts = this.attempsPerDevice * this.peripherals.size();
        this.appState.appDetailChangesSnapShot = new ArrayList(this.appState.appDetailChanges);
        this.appState.appDetailChanges.clear();
        if (this.peripherals.size() > 0) {
            Iterator<PeripheralDevice> it2 = this.peripherals.iterator();
            while (it2.hasNext()) {
                this.commandQueue.queue(new BLECommand(BLECommand.Command.sendAppDetailChanges, it2.next()));
            }
        }
    }

    public void sendParameterChanges(PeripheralDevice peripheralDevice) {
        byte[] bArrGenerateParameterChangesData = this.appState.generateParameterChangesData();
        if (bArrGenerateParameterChangesData.length == 0) {
            Log.d(TAG, "PARAMETER DATA EMPTY");
            return;
        }
        if (writeParameter(peripheralDevice, bArrGenerateParameterChangesData)) {
            return;
        }
        this.commandQueue.commandCompleted();
        if (this.parameterReSendAttempts > 0) {
            this.parameterReSendAttempts--;
            this.commandQueue.queue(new BLECommand(BLECommand.Command.sendParameterChanges, peripheralDevice));
            Log.d(TAG, "Parameter Adding resend attempt");
            return;
        }
        Log.d(TAG, "Parameter resend attempts maxed");
    }

    public boolean writeParameter(PeripheralDevice peripheralDevice, byte[] bArr) {
        if (peripheralDevice.gatt == null) {
            Log.d("TAG", "Error writeParameter Gatt null");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "writeParameter peripheral.gatt null");
            return false;
        }
        BluetoothGattService service = peripheralDevice.gatt.getService(BluetoothConstants.ParameterService_UUID);
        if (service == null) {
            Log.d("TAG", "Error writeParameter service null");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "writeParameter service null");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(BluetoothConstants.ParameterCharacteristic_UUID);
        if (characteristic == null) {
            Log.d("TAG", "Error writeParameter characteristic null");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "writeParameter characteristic null");
            return false;
        }
        characteristic.setWriteType(2);
        characteristic.setValue(bArr);
        if (peripheralDevice.gatt.writeCharacteristic(characteristic)) {
            return true;
        }
        Log.d(TAG, "parameter write failed to begin sending data!");
        FlurryAnalytics.analytics("Bluetooth", "Central Errors", "writeParameter failed to start sending data");
        return false;
    }

    public boolean writeAppDetail(PeripheralDevice peripheralDevice, byte[] bArr) {
        if (peripheralDevice.gatt == null) {
            Log.d("TAG", "Error writeAppDetail Gatt null");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "writeAppDetail peripheral.gatt null");
            return false;
        }
        BluetoothGattService service = peripheralDevice.gatt.getService(BluetoothConstants.AppDetailService_UUID);
        if (service == null) {
            Log.d("TAG", "Error writeAppDetail service null");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "writeAppDetail getService null");
            return false;
        }
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(BluetoothConstants.AppDetailCharacteristic_UUID);
        if (characteristic == null) {
            Log.d("TAG", "Error writeAppDetail characteristic null");
            FlurryAnalytics.analytics("Bluetooth", "Central Errors", "writeAppDetail characteristic null");
            return false;
        }
        characteristic.setWriteType(2);
        characteristic.setValue(bArr);
        if (peripheralDevice.gatt.writeCharacteristic(characteristic)) {
            return true;
        }
        Log.d(TAG, "write appDetail failed to begin sending data!");
        FlurryAnalytics.analytics("Bluetooth", "Central Errors", "writeAppDetail failed to start sending data");
        return false;
    }
}
