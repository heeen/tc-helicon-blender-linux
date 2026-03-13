package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class AppState {
    private static final String TAG = "AppState";
    public AppStateCallback appStateCallback;
    public int iconSelectTag;
    public ConnectionState connectionState = ConnectionState.notConnected;
    public Boolean isConnected = false;
    public Boolean advertising = false;
    public Boolean scanning = false;
    public Boolean scanningForMore = false;
    public Boolean hasBlender = false;
    public Boolean waitingForBlenderState = true;
    public Boolean sendingChanges = false;
    public byte output = 0;
    public List<Byte> slidersBeingEdited = new ArrayList();
    public List<Constants.IconID> iconState = Arrays.asList(Constants.IconID.defaultIcon, Constants.IconID.defaultIcon, Constants.IconID.defaultIcon, Constants.IconID.defaultIcon, Constants.IconID.defaultIcon, Constants.IconID.defaultIcon, Constants.IconID.defaultIcon, Constants.IconID.defaultIcon, Constants.IconID.defaultIcon);
    public List<Tuple> parameterValues = Constants.Parameter.defaultParameterValues();
    public List<Tuple> parameterChanges = new ArrayList();
    public List<Tuple> parameterChangesSnapShot = new ArrayList();
    public List<Tuple> appDetailChanges = new ArrayList();
    public List<Tuple> appDetailChangesSnapShot = new ArrayList();
    public Boolean isTalkMode = false;
    public int requestBlenderStateCount = 0;
    public int requestBlenderStateFloodStopCount = 3;

    public boolean getValueOfBit(byte b, byte b2) {
        return ((b >> b2) & 1) > 1;
    }

    public void init() {
    }

    public int unsignedIntFromByte(byte b) {
        return b & com.flurry.android.Constants.UNKNOWN;
    }

    public void setScanning(Boolean bool) {
        this.scanning = bool;
        this.appStateCallback.connectionStatusChanged();
    }

    public void setScanForMore(Boolean bool) {
        if (this.scanningForMore != bool) {
            this.scanningForMore = bool;
            this.appStateCallback.connectionStatusChanged();
        }
    }

    public void setAdvertising(Boolean bool) {
        if (this.advertising != bool) {
            this.advertising = bool;
            this.appStateCallback.connectionStatusChanged();
        }
    }

    public void setHasBlender(Boolean bool) {
        if (this.hasBlender != bool) {
            Log.d(TAG, "setHasBlender = " + Boolean.toString(bool.booleanValue()));
            this.hasBlender = bool;
            if (this.hasBlender.booleanValue()) {
                this.appStateCallback.toast("Connected", 0);
            } else {
                this.appStateCallback.toast("Disconnected", 0);
            }
            this.appStateCallback.connectionStatusChanged();
        }
    }

    public void setConnectionStatus(ConnectionState connectionState) {
        if (this.connectionState != ConnectionState.notConnected && connectionState == ConnectionState.notConnected) {
            this.connectionState = ConnectionState.notConnected;
            this.isConnected = false;
            this.appStateCallback.connectionStatusChanged();
            return;
        }
        if (this.connectionState == ConnectionState.notConnected && connectionState == ConnectionState.central) {
            this.connectionState = ConnectionState.central;
            this.isConnected = true;
            this.appStateCallback.connectionStatusChanged();
            return;
        }
        if (this.connectionState == ConnectionState.notConnected && connectionState == ConnectionState.peripheral) {
            this.connectionState = ConnectionState.peripheral;
            this.isConnected = true;
            this.appStateCallback.connectionStatusChanged();
        } else {
            if (this.connectionState == ConnectionState.peripheral && connectionState == ConnectionState.peripheral) {
                return;
            }
            if (this.connectionState == ConnectionState.central && connectionState == ConnectionState.central) {
                return;
            }
            if (this.connectionState == ConnectionState.central && connectionState == ConnectionState.peripheral) {
                Log.d(TAG, "ERROR already central trying to set peripheral");
            } else if (this.connectionState == ConnectionState.peripheral && connectionState == ConnectionState.central) {
                Log.d(TAG, "ERROR already peripheral trying to set central");
            }
        }
    }

    public void broadcastParameterState() {
        this.parameterChanges.clear();
        this.parameterChanges.addAll(this.parameterValues);
    }

    public void updateParameter(byte b, byte b2, byte b3, Boolean bool) {
        Tuple tuple = new Tuple(b, b2, b3);
        if (bool.booleanValue()) {
            Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(b), Byte.valueOf(b2), true);
            if (numParameterSearchForIndexOf != null) {
                if (this.connectionState == ConnectionState.peripheral) {
                    this.parameterChanges.remove(numParameterSearchForIndexOf);
                    return;
                }
                return;
            } else {
                if (this.slidersBeingEdited.contains(Byte.valueOf(b)) && b2 == this.output) {
                    return;
                }
                updateParameterValues((Tuple) tuple.clone());
                if (this.connectionState == ConnectionState.central) {
                    updateParameterChanges((Tuple) tuple.clone());
                    return;
                }
                return;
            }
        }
        updateParameterValues((Tuple) tuple.clone());
        updateParameterChanges((Tuple) tuple.clone());
    }

    public void updateParameterValues(Tuple tuple) {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(tuple.one), Byte.valueOf(tuple.two), false);
        if (numParameterSearchForIndexOf != null) {
            this.parameterValues.set(numParameterSearchForIndexOf.intValue(), (Tuple) tuple.clone());
        } else {
            this.parameterValues.add((Tuple) tuple.clone());
        }
    }

    public void updateParameterChanges(Tuple tuple) {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(tuple.one), Byte.valueOf(tuple.two), true);
        if (numParameterSearchForIndexOf != null) {
            this.parameterChanges.set(numParameterSearchForIndexOf.intValue(), tuple);
        } else {
            this.parameterChanges.add(tuple);
        }
    }

    public Integer parameterSearchForIndexOf(Byte b, Byte b2, Boolean bool) {
        Boolean boolValueOf = false;
        Constants.ParameterID parameterIDFor = Constants.ParameterID.getParameterIDFor(b.byteValue());
        if (parameterIDFor != null) {
            boolValueOf = Boolean.valueOf(Constants.ParameterID.SliderParameterIDs.contains(parameterIDFor));
        }
        List<Tuple> list = this.parameterValues;
        if (bool.booleanValue()) {
            list = this.parameterChanges;
        }
        if (list == null || list.size() == 0) {
            return null;
        }
        for (int i = 0; i < list.size(); i++) {
            if (boolValueOf.booleanValue() && list.get(i).one == b.byteValue() && list.get(i).two == b2.byteValue()) {
                return Integer.valueOf(i);
            }
            if (!boolValueOf.booleanValue() && list.get(i).one == b.byteValue()) {
                return Integer.valueOf(i);
            }
        }
        return null;
    }

    public Tuple getParameterGroupFor(Byte b, Byte b2, Boolean bool) {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(b, b2, bool);
        if (numParameterSearchForIndexOf == null) {
            return null;
        }
        if (bool.booleanValue()) {
            return this.parameterChanges.get(numParameterSearchForIndexOf.intValue());
        }
        return this.parameterValues.get(numParameterSearchForIndexOf.intValue());
    }

    public Boolean basicParameterErrorChecking(List<Byte> list) {
        if (list.get(0).byteValue() <= Constants.ParameterID.micGain.getId() && list.get(1).byteValue() > 3) {
            Log.d(TAG, "check found garbage parameter " + list);
            return false;
        }
        return true;
    }

    public void processParameter(byte[] bArr, boolean z) {
        ArrayList arrayList = new ArrayList();
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
            if (arrayList.size() == 3) {
                if (!basicParameterErrorChecking(arrayList).booleanValue()) {
                    arrayList.clear();
                } else if (arrayList.get(0).equals(Byte.valueOf(Constants.ParameterID.version.getId()))) {
                    arrayList.clear();
                } else if (arrayList.get(0).equals(Constants.ParameterID.isBlender)) {
                    Log.d(TAG, "Process parameter received isBlender");
                    setHasBlender(Boolean.valueOf(arrayList.get(2).byteValue() == 1));
                    arrayList.clear();
                } else if (arrayList.get(0).equals(Constants.ParameterID.requestBlenderState)) {
                    Log.d(TAG, "blenderState from a peripheral");
                    broadcastHasBlenderState();
                    arrayList.clear();
                } else {
                    updateParameter(arrayList.get(0).byteValue(), arrayList.get(1).byteValue(), arrayList.get(2).byteValue(), Boolean.valueOf(z));
                    if (this.connectionState == ConnectionState.central && arrayList.get(0).byteValue() == Constants.ParameterID.blenderState.getId()) {
                        blenderStateAnalytics();
                    }
                    arrayList.clear();
                }
            }
        }
        if (arrayList.size() > 0) {
            Log.d(TAG, "ERROR we only got part of a message");
        }
    }

    public byte[] generateParameterChangesData() {
        byte[] bArr = new byte[this.parameterChangesSnapShot.size() * 3];
        int i = 0;
        for (Tuple tuple : this.parameterChangesSnapShot) {
            bArr[i] = tuple.one;
            int i2 = i + 1;
            bArr[i2] = tuple.two;
            int i3 = i2 + 1;
            bArr[i3] = tuple.three;
            i = i3 + 1;
        }
        return bArr;
    }

    public Boolean checkTalk() {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(Constants.ParameterID.talk.getId()), (byte) 0, false);
        if (numParameterSearchForIndexOf != null && this.parameterValues.get(numParameterSearchForIndexOf.intValue()).three == 1) {
            return true;
        }
        return false;
    }

    public void setTalk(boolean z) {
        updateParameter(Constants.ParameterID.talk.getId(), (byte) 0, z ? (byte) 1 : (byte) 0, false);
    }

    public Boolean checkMuted(byte b) {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(Constants.ParameterID.muteOutput.getId()), (byte) 0, false);
        if (numParameterSearchForIndexOf == null) {
            return false;
        }
        BitSet bitSetValueOf = BitSet.valueOf(new byte[]{this.parameterValues.get(numParameterSearchForIndexOf.intValue()).three});
        switch (b) {
        }
        return false;
    }

    public void setMute(byte b, boolean z) {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(Constants.ParameterID.muteOutput.getId()), (byte) 0, false);
        if (numParameterSearchForIndexOf == null) {
            return;
        }
        BitSet bitSetValueOf = BitSet.valueOf(new byte[]{this.parameterValues.get(numParameterSearchForIndexOf.intValue()).three});
        switch (b) {
            case 0:
                bitSetValueOf.set(3, z);
                break;
            case 1:
                bitSetValueOf.set(2, z);
                break;
            case 2:
                bitSetValueOf.set(1, z);
                break;
            case 3:
                bitSetValueOf.set(0, z);
                break;
            default:
                return;
        }
        updateParameter(Constants.ParameterID.muteOutput.getId(), (byte) 0, byteFromBitSet(bitSetValueOf), false);
    }

    public boolean checkCompressorOn(byte b) {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(Constants.ParameterID.compressorOnOff.getId()), (byte) 0, false);
        if (numParameterSearchForIndexOf == null) {
            return false;
        }
        BitSet bitSetValueOf = BitSet.valueOf(new byte[]{this.parameterValues.get(numParameterSearchForIndexOf.intValue()).three});
        switch (b) {
        }
        return false;
    }

    public void setCompressorOnOff(byte b, boolean z) {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(Constants.ParameterID.compressorOnOff.getId()), (byte) 0, false);
        if (numParameterSearchForIndexOf == null) {
            return;
        }
        BitSet bitSetValueOf = BitSet.valueOf(new byte[]{this.parameterValues.get(numParameterSearchForIndexOf.intValue()).three});
        switch (b) {
            case 0:
                bitSetValueOf.set(3, z);
                break;
            case 1:
                bitSetValueOf.set(2, z);
                break;
            case 2:
                bitSetValueOf.set(1, z);
                break;
            case 3:
                bitSetValueOf.set(0, z);
                break;
            default:
                return;
        }
        updateParameter(Constants.ParameterID.compressorOnOff.getId(), (byte) 0, byteFromBitSet(bitSetValueOf), false);
    }

    public byte byteFromBitSet(BitSet bitSet) {
        byte[] byteArray = bitSet.toByteArray();
        if (byteArray.length > 0) {
            return byteArray[0];
        }
        return (byte) 0;
    }

    public boolean jackSenseStateFor(Constants.ParameterID parameterID) {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(Constants.ParameterID.blenderState.getId()), (byte) 0, false);
        if (numParameterSearchForIndexOf == null) {
            return false;
        }
        Tuple tuple = this.parameterValues.get(numParameterSearchForIndexOf.intValue());
        BitSet bitSetValueOf = BitSet.valueOf(new byte[]{tuple.two});
        BitSet.valueOf(new byte[]{tuple.three});
        switch (parameterID) {
        }
        return false;
    }

    public boolean jackSenseStateFor(byte b) {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(Constants.ParameterID.blenderState.getId()), (byte) 0, false);
        if (numParameterSearchForIndexOf == null) {
            return false;
        }
        BitSet bitSetValueOf = BitSet.valueOf(new byte[]{this.parameterValues.get(numParameterSearchForIndexOf.intValue()).three});
        switch (b) {
        }
        return false;
    }

    public boolean jackSenseStateForSDCard() {
        Integer numParameterSearchForIndexOf = parameterSearchForIndexOf(Byte.valueOf(Constants.ParameterID.blenderState.getId()), (byte) 0, false);
        if (numParameterSearchForIndexOf == null) {
            return false;
        }
        Tuple tuple = this.parameterValues.get(numParameterSearchForIndexOf.intValue());
        BitSet.valueOf(new byte[]{tuple.two});
        return BitSet.valueOf(new byte[]{tuple.three}).get(4);
    }

    public void blenderStateAnalytics() {
        StringBuilder sb = new StringBuilder();
        sb.append("Inputs: ");
        sb.append(jackSenseStateFor(Constants.ParameterID.input1) ? "1," : "");
        String string = sb.toString();
        StringBuilder sb2 = new StringBuilder();
        sb2.append(string);
        sb2.append(jackSenseStateFor(Constants.ParameterID.input2) ? "2," : "");
        String string2 = sb2.toString();
        StringBuilder sb3 = new StringBuilder();
        sb3.append(string2);
        sb3.append(jackSenseStateFor(Constants.ParameterID.input3) ? "3," : "");
        String string3 = sb3.toString();
        StringBuilder sb4 = new StringBuilder();
        sb4.append(string3);
        sb4.append(jackSenseStateFor(Constants.ParameterID.input4) ? "4," : "");
        String string4 = sb4.toString();
        StringBuilder sb5 = new StringBuilder();
        sb5.append(string4);
        sb5.append(jackSenseStateFor(Constants.ParameterID.input5) ? "5," : "");
        String string5 = sb5.toString();
        StringBuilder sb6 = new StringBuilder();
        sb6.append(string5);
        sb6.append(jackSenseStateFor(Constants.ParameterID.input6) ? "6" : "");
        String string6 = sb6.toString();
        FlurryAnalytics.analytics("Blender State", "Inputs JackSense", string6);
        StringBuilder sb7 = new StringBuilder();
        sb7.append("Outputs: ");
        sb7.append(jackSenseStateFor((byte) 0) ? "A," : "");
        String string7 = sb7.toString();
        StringBuilder sb8 = new StringBuilder();
        sb8.append(string7);
        sb8.append(jackSenseStateFor((byte) 1) ? "B," : "");
        String string8 = sb8.toString();
        StringBuilder sb9 = new StringBuilder();
        sb9.append(string8);
        sb9.append(jackSenseStateFor((byte) 2) ? "C," : "");
        String string9 = sb9.toString();
        StringBuilder sb10 = new StringBuilder();
        sb10.append(string9);
        sb10.append(jackSenseStateFor((byte) 3) ? "D" : "");
        String string10 = sb10.toString();
        FlurryAnalytics.analytics("Blender State", "Outputs JackSense", string10);
        FlurryAnalytics.analytics("Blender State", "SDCard JackSense", jackSenseStateForSDCard() ? "Has SDCard" : "No SDCard");
        FlurryAnalytics.analytics("Blender State", "All JackSense", string6 + " | " + string10);
    }

    public void broadcastAppDetailState() {
        for (int i = 0; i < this.iconState.size(); i++) {
            this.appDetailChanges.add(new Tuple(Constants.ParameterID.iconChange.getId(), (byte) i, this.iconState.get(i).getId()));
        }
        broadcastHasBlenderState();
    }

    public void broadcastHasBlenderState() {
        if (this.connectionState == ConnectionState.central && this.hasBlender.booleanValue()) {
            Log.d(TAG, "broadcastHasBlenderState");
            Iterator<Tuple> it = this.appDetailChanges.iterator();
            boolean z = false;
            while (it.hasNext()) {
                if (it.next().one == Constants.ParameterID.hasBlender.getId()) {
                    z = true;
                }
            }
            if (z) {
                return;
            }
            this.appDetailChanges.add(new Tuple(Constants.ParameterID.hasBlender.getId(), (byte) 0, this.hasBlender.booleanValue() ? (byte) 1 : (byte) 0));
        }
    }

    public void requestBlenderState() {
        Log.d(TAG, "requestBlenderState");
        this.parameterChanges.add(new Tuple(Constants.ParameterID.requestBlenderState.getId(), (byte) 0, (byte) 0));
    }

    public void requestBlenderStateFromCentral() {
        Log.d(TAG, "requestBlenderStateFromCentral");
        this.parameterChanges.add(new Tuple(Constants.ParameterID.requestBlenderState.getId(), (byte) 0, (byte) 0));
    }

    public void processAppDetails(byte[] bArr, boolean z) {
        ArrayList arrayList = new ArrayList();
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
            if (arrayList.size() == 3) {
                Log.d(TAG, "processAppDetails (" + arrayList.get(0) + "," + arrayList.get(1) + "," + arrayList.get(2) + ")");
                if (((Byte) arrayList.get(0)).equals(Byte.valueOf(Constants.ParameterID.requestBlenderState.getId()))) {
                    Log.d(TAG, "Received requestBlenderState Message");
                    if (this.connectionState == ConnectionState.central) {
                        broadcastHasBlenderState();
                    }
                } else if (((Byte) arrayList.get(0)).equals(Byte.valueOf(Constants.ParameterID.hasBlender.getId()))) {
                    Log.d(TAG, "process appDetail hasBlender = " + Boolean.toString(((Byte) arrayList.get(2)).equals((byte) 1)) + "  connectionState = " + this.connectionState.name());
                    if (this.connectionState != ConnectionState.central) {
                        Log.d(TAG, "process appDetail hasBlender = " + Boolean.toString(((Byte) arrayList.get(2)).equals(1)) + "  connectionState = " + this.connectionState.name());
                        setHasBlender(Boolean.valueOf(((Byte) arrayList.get(2)).equals((byte) 1)));
                        this.waitingForBlenderState = false;
                    }
                } else {
                    updateToAppDetail(((Byte) arrayList.get(0)).byteValue(), ((Byte) arrayList.get(1)).byteValue(), ((Byte) arrayList.get(2)).byteValue(), Boolean.valueOf(z));
                }
                arrayList.clear();
            }
        }
        if (arrayList.size() > 0) {
            Log.d(TAG, "ERROR we only got part of a message");
        }
    }

    public void updateToAppDetail(byte b, byte b2, byte b3, Boolean bool) {
        if (b == Constants.ParameterID.scanForMore.getId()) {
            this.appStateCallback.scanForMore();
            return;
        }
        if (b != Constants.ParameterID.iconChange.getId() || b2 > ((byte) (this.iconState.size() - 1))) {
            return;
        }
        for (Constants.CustomIcon customIcon : Constants.CustomIcon.CustomIcons) {
            if (customIcon.id.getId() == b3) {
                this.iconState.set(b2, customIcon.id);
                if (bool.booleanValue() && this.connectionState == ConnectionState.central) {
                    this.appDetailChanges.add(new Tuple(b, b2, b3));
                }
            }
        }
    }

    public byte[] generateAppDetailChangesData() {
        Log.d(TAG, "generateAppDetailChangesData");
        byte[] bArr = new byte[this.appDetailChangesSnapShot.size() * 3];
        int i = 0;
        for (Tuple tuple : this.appDetailChangesSnapShot) {
            bArr[i] = tuple.one;
            int i2 = i + 1;
            bArr[i2] = tuple.two;
            int i3 = i2 + 1;
            bArr[i3] = tuple.three;
            i = i3 + 1;
            Log.d(TAG, "(" + ((int) tuple.one) + "," + ((int) tuple.two) + "," + ((int) tuple.three) + ")");
        }
        return bArr;
    }

    public void savePreset(Context context) {
        ArrayList arrayList = new ArrayList();
        for (Tuple tuple : this.parameterValues) {
            if (tuple.one == Constants.ParameterID.input1.getId() || tuple.one == Constants.ParameterID.input2.getId() || tuple.one == Constants.ParameterID.input3.getId() || tuple.one == Constants.ParameterID.input4.getId() || tuple.one == Constants.ParameterID.input5.getId() || tuple.one == Constants.ParameterID.input6.getId() || tuple.one == Constants.ParameterID.level.getId() || tuple.one == Constants.ParameterID.compressor.getId() || tuple.one == Constants.ParameterID.micGain.getId()) {
                arrayList.add(Byte.valueOf(tuple.one));
                arrayList.add(Byte.valueOf(tuple.two));
                arrayList.add(Byte.valueOf(tuple.three));
            }
        }
        int i = 0;
        String[] strArr = new String[arrayList.size()];
        Iterator it = arrayList.iterator();
        while (it.hasNext()) {
            strArr[i] = Byte.toString(((Byte) it.next()).byteValue());
            i++;
        }
        new FileSystem().savePreset(context, strArr);
    }

    public void loadPreset(Context context, String str) {
        JSONObject jSONObjectLoadPreset = new FileSystem().loadPreset(context, str);
        ArrayList arrayList = new ArrayList();
        try {
            JSONArray jSONArray = jSONObjectLoadPreset.getJSONArray(Constants.kPresetValues);
            Log.d(TAG, "JSONValues set, length " + Integer.toString(jSONArray.length()));
            int length = jSONArray.length();
            for (int i = 0; i < length; i++) {
                try {
                    arrayList.add(Byte.valueOf(Byte.parseByte(jSONArray.getString(i))));
                } catch (JSONException e) {
                    Log.d(TAG, "Load preset failed to get value from jsonArray: " + e.toString());
                    e.printStackTrace();
                }
            }
            byte[] bArr = new byte[arrayList.size()];
            int size = arrayList.size();
            for (int i2 = 0; i2 < size; i2++) {
                bArr[i2] = ((Byte) arrayList.get(i2)).byteValue();
            }
            processParameter(bArr, false);
            this.appStateCallback.callbackUpdateToValues();
        } catch (JSONException e2) {
            Log.d(TAG, "loadPreset get json array failed: " + e2.toString());
        }
    }

    public void resetToDefault() {
        this.parameterValues = Constants.Parameter.defaultParameterValues();
        this.parameterChanges = Constants.Parameter.defaultParameterValues();
        this.appStateCallback.callbackUpdateToValues();
    }

    public void printTupleList(List<Tuple> list) {
        for (Tuple tuple : list) {
            Log.d(TAG, "printTupleList (" + Byte.toString(tuple.one) + ", " + Byte.toString(tuple.two) + ", " + Byte.toString(tuple.three) + ")");
        }
    }
}
