package tchelicon.com.blenderappandroid;

import android.graphics.Color;
import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class SliderModel {
    int background;
    int darkTrack;
    int disabledIcon;
    Constants.IconID icon;
    int iconBackground;
    Constants.ParameterID id;
    boolean jackSensed;
    int tint;
    Float value;
    static Float defaultValue = Float.valueOf(0.5f);
    public static SliderModel[] defaultSliderModels = {new SliderModel(Constants.ParameterID.input1, defaultValue, Constants.IconID.defaultIcon, Color.parseColor("#21DEA3"), Color.parseColor("#1C1C1F"), Color.parseColor("#2E2F33"), Color.parseColor("#33343A"), Color.parseColor("#4D5058"), false), new SliderModel(Constants.ParameterID.input2, defaultValue, Constants.IconID.defaultIcon, Color.parseColor("#B7DE21"), Color.parseColor("#1C1C1F"), Color.parseColor("#33343A"), Color.parseColor("#33343A"), Color.parseColor("#4D5058"), false), new SliderModel(Constants.ParameterID.input3, defaultValue, Constants.IconID.defaultIcon, Color.parseColor("#CA21DE"), Color.parseColor("#1C1C1F"), Color.parseColor("#2E2F33"), Color.parseColor("#33343A"), Color.parseColor("#4D5058"), false), new SliderModel(Constants.ParameterID.input4, defaultValue, Constants.IconID.defaultIcon, Color.parseColor("#DE7D21"), Color.parseColor("#1C1C1F"), Color.parseColor("#33343A"), Color.parseColor("#33343A"), Color.parseColor("#4D5058"), false), new SliderModel(Constants.ParameterID.input5, defaultValue, Constants.IconID.defaultIcon, Color.parseColor("#DE2189"), Color.parseColor("#1C1C1F"), Color.parseColor("#2E2F33"), Color.parseColor("#33343A"), Color.parseColor("#4D5058"), false), new SliderModel(Constants.ParameterID.input6, defaultValue, Constants.IconID.defaultIcon, Color.parseColor("#62DE21"), Color.parseColor("#1C1C1F"), Color.parseColor("#33343A"), Color.parseColor("#33343A"), Color.parseColor("#4D5058"), false)};

    public SliderModel(Constants.ParameterID parameterID, Float f, Constants.IconID iconID, int i, int i2, int i3, int i4, int i5, boolean z) {
        this.id = parameterID;
        this.value = f;
        this.icon = iconID;
        this.tint = i;
        this.darkTrack = i2;
        this.background = i3;
        this.iconBackground = i4;
        this.disabledIcon = i5;
        this.jackSensed = z;
    }
}
