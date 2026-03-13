package tchelicon.com.blenderappandroid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/* JADX INFO: loaded from: classes.dex */
public class PeripheralDevice {
    public BluetoothDevice device;
    public BluetoothGatt gatt;
    public boolean isBlender = false;
    public boolean hasConnected = false;
    public boolean hasDiscoveredServices = false;
    public int connectionAttemptsCount = 0;
    public int discoveryAttemptsCount = 0;

    PeripheralDevice(BluetoothDevice bluetoothDevice) {
        this.device = bluetoothDevice;
    }

    PeripheralDevice(BluetoothDevice bluetoothDevice, BluetoothGatt bluetoothGatt) {
        this.device = bluetoothDevice;
        this.gatt = bluetoothGatt;
    }
}
