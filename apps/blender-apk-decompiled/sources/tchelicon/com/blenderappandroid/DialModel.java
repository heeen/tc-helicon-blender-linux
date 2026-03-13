package tchelicon.com.blenderappandroid;

import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class DialModel {
    int background;
    int darkTrack;
    Constants.ParameterID id;
    String label;
    int labelTint;
    float maxAngle;
    float minAngle;
    Constants.ParameterID muteId;
    boolean muted;
    int mutedColour;
    int mutedIcon;
    int mutedTint;
    int thumb;
    int thumbBackground;
    int tint;
    int track;
    int unmutedColour;
    int unmutedIcon;
    Float value;
    static Float defaultValue = Float.valueOf(0.5f);
    public static DialModel[] defaultDialModels = {new DialModel(Constants.ParameterID.level, Constants.ParameterID.muteOutput, "LEVEL", defaultValue, 165.0f, 375.0f, false, R.drawable.knob_dot, R.drawable.knob, R.drawable.knob_track_white, R.drawable.mute_icon_font, R.drawable.mute_icon_font, R.color.c30, R.color.c31, R.color.c29, R.color.c1, R.color.c6, R.color.c7, R.color.c29), new DialModel(Constants.ParameterID.compressor, Constants.ParameterID.compressorOnOff, "COMPRESSOR", defaultValue, 165.0f, 375.0f, false, R.drawable.knob_dot, R.drawable.knob, R.drawable.knob_track_white, R.drawable.compress_icon_font, R.drawable.compress_icon_font, R.color.c30, R.color.c31, R.color.c29, R.color.c1, R.color.c6, R.color.c7, R.color.c23), new DialModel(Constants.ParameterID.micGain, Constants.ParameterID.talk, "ROOM MIC", defaultValue, 165.0f, 375.0f, false, R.drawable.knob_dot, R.drawable.knob, R.drawable.knob_track_white, R.drawable.talk_icon_font, R.drawable.talk_icon_font, R.color.c30, R.color.c31, R.color.c29, R.color.c1, R.color.c6, R.color.c7, R.color.c29)};

    public DialModel(Constants.ParameterID parameterID, Constants.ParameterID parameterID2, String str, Float f, float f2, float f3, boolean z, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12) {
        this.id = parameterID;
        this.muteId = parameterID2;
        this.label = str;
        this.value = f;
        this.minAngle = f2;
        this.maxAngle = f3;
        this.background = i9;
        this.labelTint = i10;
        this.muted = z;
        this.thumb = i;
        this.thumbBackground = i2;
        this.track = i3;
        this.unmutedIcon = i4;
        this.mutedIcon = i5;
        this.tint = i6;
        this.darkTrack = i7;
        this.mutedTint = i8;
        this.background = i9;
        this.labelTint = i10;
        this.unmutedColour = i11;
        this.mutedColour = i12;
    }
}
