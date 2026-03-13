package tchelicon.com.blenderappandroid;

import android.bluetooth.BluetoothGattDescriptor;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public class BLECommand {
    UUID characteristicUUID;
    public Command command;
    BluetoothGattDescriptor descriptor;
    public PeripheralDevice peripheral;

    enum Command {
        connect,
        discover,
        setCharacteristicNotification,
        writeDescriptor,
        disconnect,
        requestMTUSize,
        sendParameterChanges,
        sendAppDetailChanges,
        requestConnectionPriority
    }

    BLECommand(Command command, PeripheralDevice peripheralDevice) {
        this.command = command;
        this.peripheral = peripheralDevice;
    }

    BLECommand(Command command, PeripheralDevice peripheralDevice, UUID uuid) {
        this.command = command;
        this.peripheral = peripheralDevice;
        this.characteristicUUID = uuid;
    }

    BLECommand(Command command, PeripheralDevice peripheralDevice, BluetoothGattDescriptor bluetoothGattDescriptor) {
        this.command = command;
        this.peripheral = peripheralDevice;
        this.descriptor = bluetoothGattDescriptor;
    }
}
