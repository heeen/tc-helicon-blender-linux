package tchelicon.com.blenderappandroid;

import android.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

/* JADX INFO: loaded from: classes.dex */
public abstract class ServiceFragment extends Fragment {

    public interface ServiceFragmentDelegate {
        void sendNotificationToDevices(BluetoothGattCharacteristic bluetoothGattCharacteristic);
    }

    public abstract BluetoothGattService getBluetoothGattService();

    public abstract ParcelUuid getServiceUUID();

    public int writeCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic, int i, byte[] bArr) {
        throw new UnsupportedOperationException("Method writeCharacteristic not overridden");
    }

    public void notificationsDisabled(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        throw new UnsupportedOperationException("Method notificationsDisabled not overridden");
    }

    public void notificationsEnabled(BluetoothGattCharacteristic bluetoothGattCharacteristic, boolean z) {
        throw new UnsupportedOperationException("Method notificationsEnabled not overridden");
    }
}
