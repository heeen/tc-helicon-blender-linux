package tchelicon.com.blenderappandroid;

import java.util.HashMap;
import java.util.UUID;

/* JADX INFO: loaded from: classes.dex */
public class BluetoothConstants {
    private static HashMap<String, String> attributes = new HashMap<>();
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805F9B34FB";
    public static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG);
    public static String ParameterService = "E71EE188-279F-4ED6-8055-12D77BFD900C";
    public static final UUID ParameterService_UUID = UUID.fromString(ParameterService);
    public static String ParameterCharacteristic = "50E2D021-F23B-46FB-B7E6-FBE12301276A";
    public static final UUID ParameterCharacteristic_UUID = UUID.fromString(ParameterCharacteristic);
    public static String AppDetailService = "F7E58580-9BB5-48A3-B8A4-6BE6A391B8DF";
    public static final UUID AppDetailService_UUID = UUID.fromString(AppDetailService);
    public static String AppDetailCharacteristic = "7BB81501-46CF-4722-AB91-276AF25528EE";
    public static final UUID AppDetailCharacteristic_UUID = UUID.fromString(AppDetailCharacteristic);
    public static String DummyService = "4264DDEF-59BD-49CC-854A-CC09CAA2232A";
    public static final UUID DummyService_UUID = UUID.fromString(DummyService);
    public static String DummyCharacteristic = "435A6273-2810-4136-ACFE-4A0C0BD65186";
    public static final UUID DummyCharacteristic_UUID = UUID.fromString(DummyCharacteristic);
}
