package tchelicon.com.blenderappandroid;

import android.support.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class Constants {
    public static final boolean IGNOREJACKSENSE = false;
    public static byte MaxByteValue = -1;
    public static float MaxByteValueAsFloat = 255.0f;
    public static final String PREFS_NAME = "MainPreferences";
    public static final boolean UseAppDetails = false;
    public static final boolean UseP2P = false;
    public static int bluetoothAdminRequestCode = 126;
    public static int bluetoothRequestCode = 125;
    public static long broadcastTime = 120;
    public static boolean broadcastToOneAtATime = true;
    public static int coarseLocationRequestCode = 123;
    public static int fineLocationRequestCode = 124;
    public static float iconSelectIconSize = 50.0f;
    public static String kFlurry = "ZKMHW758WXNJ9TX47C7K";
    public static String kInputTipShowCount = "kInputTipShowCount";
    public static String kInputTipShowThisLaunch = "kInputTipShowThisLaunch";
    public static String kP2P = "79104fd405e1431797a94e72f305ff77";
    public static String kPresetName = "kPresetName";
    public static String kPresetNumber = "kPresetNumber";
    public static String kPresetValues = "kPresetValues";
    public static int maxNumberOfInputTipShowCount = 3;
    public static String outlinkFacebook = "https://www.facebook.com/tchelicon/";
    public static String outlinkInstagram = "https://www.instagram.com/tcheliconofficial/";
    public static String outlinkManual = "https://www.tc-helicon.com/c/Tchelicon/Downloads?text=blender";
    public static String outlinkProductPage = "";
    public static String outlinkSupport = "http://www.tc-helicon.com/support/";
    public static String outlinkTutorial = "https://www.youtube.com/watch?v=DwsVmpp53BA&list=PLR6Yp1ynH1xcJ2S-3EJMmoODfBKps8oLk";
    public static String outlinkTwitter = "https://twitter.com/tchelicon";
    public static String outlinkWebsite = "https://www.tc-helicon.com";
    public static long scanningTimeOutTime = 15000;
    public static int sleepBetweenBroadcastTime = 10;
    public static int turnOnLocationRequestCode = 122;

    enum ParameterID {
        input1(0),
        input2(1),
        input3(2),
        input4(3),
        input5(4),
        input6(5),
        level(6),
        compressor(7),
        micGain(8),
        talk(9),
        blenderState(11),
        version(12),
        iconChange(14),
        scanForMore(15),
        disconnectPeripheral(16),
        isBlender(17),
        hasBlender(18),
        requestBlenderState(19),
        muteOutput(20),
        compressorOnOff(21);

        private final byte id;
        public static ParameterID[] ParameterIDs = {input1, input2, input3, input4, input5, input6, level, compressor, micGain, talk, blenderState, version, muteOutput, compressorOnOff};
        public static List<ParameterID> SliderParameterIDs = Arrays.asList(input1, input2, input3, input4, input5, input6, level, compressor, micGain);
        public static List<ParameterID> InputParameterIDs = Arrays.asList(input1, input2, input3, input4, input5, input6);

        ParameterID(int i) {
            this.id = (byte) i;
        }

        public static ParameterID getParameterIDFor(byte b) {
            for (ParameterID parameterID : values()) {
                if (parameterID.id == b) {
                    return parameterID;
                }
            }
            return null;
        }

        public byte getId() {
            return this.id;
        }
    }

    enum ParameterNames {
        input1("input1"),
        input2("input2"),
        input3("input3"),
        input4("input4"),
        input5("input5"),
        input6("input6"),
        level("level"),
        compressor("compressor"),
        micGain("micGain"),
        talk("talk"),
        blenderState("blenderState"),
        version("version"),
        muteOutput("muteOutput"),
        compressorOnOff("compressorOnOff");

        private final String id;

        ParameterNames(String str) {
            this.id = str;
        }

        public String getId() {
            return this.id;
        }
    }

    public static class Parameter {
        public ParameterID id;
        public ParameterNames name;
        public byte value1;
        public byte value2;
        public static ParameterNames[] SliderParameterNames = {ParameterNames.input1, ParameterNames.input2, ParameterNames.input3, ParameterNames.input4, ParameterNames.input5, ParameterNames.input6, ParameterNames.level, ParameterNames.compressor, ParameterNames.micGain};
        public static ParameterNames[] InputParameterNames = {ParameterNames.input1, ParameterNames.input2, ParameterNames.input3, ParameterNames.input4, ParameterNames.input5, ParameterNames.input6};
        static byte defaultInputGain = -51;
        static byte defaultOutputGain = 82;
        static byte defaultTalkGain = 82;
        static byte defaultCompression = -125;
        public static Parameter[] Parameters = {new Parameter(ParameterNames.input1, ParameterID.input1, (byte) 0, defaultInputGain), new Parameter(ParameterNames.input2, ParameterID.input2, (byte) 0, defaultInputGain), new Parameter(ParameterNames.input3, ParameterID.input3, (byte) 0, defaultInputGain), new Parameter(ParameterNames.input4, ParameterID.input4, (byte) 0, defaultInputGain), new Parameter(ParameterNames.input5, ParameterID.input5, (byte) 0, defaultInputGain), new Parameter(ParameterNames.input5, ParameterID.input6, (byte) 0, defaultInputGain), new Parameter(ParameterNames.level, ParameterID.level, (byte) 0, defaultOutputGain), new Parameter(ParameterNames.compressor, ParameterID.compressor, (byte) 0, defaultCompression), new Parameter(ParameterNames.micGain, ParameterID.micGain, (byte) 0, defaultTalkGain), new Parameter(ParameterNames.talk, ParameterID.talk, (byte) 0, (byte) 0), new Parameter(ParameterNames.blenderState, ParameterID.blenderState, (byte) 0, (byte) 0), new Parameter(ParameterNames.version, ParameterID.version, (byte) 0, (byte) 0), new Parameter(ParameterNames.muteOutput, ParameterID.muteOutput, (byte) 0, (byte) 0), new Parameter(ParameterNames.compressorOnOff, ParameterID.compressorOnOff, (byte) 0, (byte) 0)};

        public Parameter(ParameterNames parameterNames, ParameterID parameterID, byte b, byte b2) {
            this.name = parameterNames;
            this.id = parameterID;
            this.value1 = b;
            this.value2 = b2;
        }

        public static List<Tuple> defaultParameterValues() {
            ArrayList arrayList = new ArrayList();
            for (ParameterID parameterID : ParameterID.ParameterIDs) {
                Parameter parameter = parameter(parameterID);
                if (parameter != null) {
                    if (ParameterID.SliderParameterIDs.contains(parameterID)) {
                        for (int i = 0; i < 4; i++) {
                            arrayList.add(new Tuple(parameter.id.id, (byte) i, parameter.value2));
                        }
                    } else {
                        arrayList.add(new Tuple(parameter.id.id, parameter.value1, parameter.value2));
                    }
                }
            }
            return arrayList;
        }

        @Nullable
        public static Parameter parameter(ParameterID parameterID) {
            for (Parameter parameter : Parameters) {
                if (parameterID == parameter.id) {
                    return parameter;
                }
            }
            return null;
        }
    }

    enum IconID {
        defaultIcon(0),
        A(1),
        B(2),
        C(3),
        D(4),
        E(5),
        F(6),
        G(7),
        H(8),
        I(9),
        J(10),
        K(11),
        L(12),
        M(13),
        N(14),
        O(15),
        P(16),
        Q(17),
        R(18),
        S(19),
        T(20),
        U(21),
        V(22),
        W(23),
        X(24),
        Y(25),
        Z(26),
        drum(27),
        guitar_acoustic(28),
        guitar_electric(29),
        keyboard(30),
        laptop(31),
        mic(32),
        phone_icon(33),
        voice_live_play(34),
        mixer_board(35),
        instrument_cable(36),
        sampler_board(37);

        private final byte id;

        IconID(int i) {
            this.id = (byte) i;
        }

        public byte getId() {
            return this.id;
        }
    }

    public static class CustomIcon {
        public static CustomIcon[] CustomIcons = {new CustomIcon(R.drawable.mic, IconID.defaultIcon), new CustomIcon(R.drawable.drum_set, IconID.drum), new CustomIcon(R.drawable.guitar_acoustic, IconID.guitar_acoustic), new CustomIcon(R.drawable.guitar_electric, IconID.guitar_electric), new CustomIcon(R.drawable.keyboard, IconID.keyboard), new CustomIcon(R.drawable.laptop, IconID.laptop), new CustomIcon(R.drawable.mic, IconID.mic), new CustomIcon(R.drawable.phone_icon, IconID.phone_icon), new CustomIcon(R.drawable.voice_live_play, IconID.voice_live_play), new CustomIcon(R.drawable.mixer_board, IconID.mixer_board), new CustomIcon(R.drawable.instrument_cable, IconID.instrument_cable), new CustomIcon(R.drawable.sampler_board, IconID.sampler_board), new CustomIcon(R.drawable.mic, IconID.A), new CustomIcon(R.drawable.mic, IconID.B), new CustomIcon(R.drawable.mic, IconID.C), new CustomIcon(R.drawable.mic, IconID.D), new CustomIcon(R.drawable.mic, IconID.E), new CustomIcon(R.drawable.mic, IconID.F), new CustomIcon(R.drawable.mic, IconID.G), new CustomIcon(R.drawable.mic, IconID.H), new CustomIcon(R.drawable.mic, IconID.I), new CustomIcon(R.drawable.mic, IconID.J), new CustomIcon(R.drawable.mic, IconID.K), new CustomIcon(R.drawable.mic, IconID.L), new CustomIcon(R.drawable.mic, IconID.M), new CustomIcon(R.drawable.mic, IconID.N), new CustomIcon(R.drawable.mic, IconID.O), new CustomIcon(R.drawable.mic, IconID.P), new CustomIcon(R.drawable.mic, IconID.Q), new CustomIcon(R.drawable.mic, IconID.R), new CustomIcon(R.drawable.mic, IconID.S), new CustomIcon(R.drawable.mic, IconID.T), new CustomIcon(R.drawable.mic, IconID.U), new CustomIcon(R.drawable.mic, IconID.V), new CustomIcon(R.drawable.mic, IconID.W), new CustomIcon(R.drawable.mic, IconID.X), new CustomIcon(R.drawable.mic, IconID.Y), new CustomIcon(R.drawable.mic, IconID.Z)};
        public int icon;
        public IconID id;

        public CustomIcon(int i, IconID iconID) {
            this.icon = i;
            this.id = iconID;
        }
    }
}
