package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import tchelicon.com.blenderappandroid.Constants;
import tchelicon.com.blenderappandroid.Dial;

/* JADX INFO: loaded from: classes.dex */
public class DialComponent extends ConstraintLayout {
    private static final String TAG = "DialComponent";
    AppState appState;
    private Dial.DialListener dialListener;
    Constants.ParameterID id;
    DialModel model;
    private View.OnClickListener mutePressed;

    public DialComponent(Context context) {
        super(context);
        this.dialListener = new Dial.DialListener() { // from class: tchelicon.com.blenderappandroid.DialComponent.1
            @Override // tchelicon.com.blenderappandroid.Dial.DialListener
            public void onStartEditing(DialModel dialModel) {
                if (DialComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(dialModel.id.getId()))) {
                    return;
                }
                DialComponent.this.appState.slidersBeingEdited.add(Byte.valueOf(dialModel.id.getId()));
            }

            @Override // tchelicon.com.blenderappandroid.Dial.DialListener
            public void valueChanged(DialModel dialModel) {
                byte bFloatValue = (byte) (((double) (dialModel.value.floatValue() * Constants.MaxByteValueAsFloat)) + 0.5d);
                if (DialComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(dialModel.id.getId()))) {
                    DialComponent.this.appState.updateParameter(dialModel.id.getId(), DialComponent.this.appState.output, bFloatValue, false);
                }
            }

            @Override // tchelicon.com.blenderappandroid.Dial.DialListener
            public void onEndEditing(DialModel dialModel) {
                FlurryAnalytics.analytics("Levels", "Edited", Constants.Parameter.parameter(dialModel.id).name.name());
                if (dialModel.id.getId() <= Constants.ParameterID.input6.getId()) {
                    FlurryAnalytics.analyticsLevels("Levels", "Input Values", dialModel.value.floatValue());
                } else if (dialModel.id == Constants.ParameterID.level) {
                    FlurryAnalytics.analyticsLevels("Levels", "Level Values", dialModel.value.floatValue());
                } else if (dialModel.id == Constants.ParameterID.compressor) {
                    FlurryAnalytics.analyticsLevels("Levels", "Compressor Values", dialModel.value.floatValue());
                } else if (dialModel.id == Constants.ParameterID.micGain) {
                    FlurryAnalytics.analyticsLevels("Levels", "MicGain Values", dialModel.value.floatValue());
                }
                int iIndexOf = DialComponent.this.appState.slidersBeingEdited.indexOf(Byte.valueOf(dialModel.id.getId()));
                if (iIndexOf != -1) {
                    DialComponent.this.appState.slidersBeingEdited.remove(iIndexOf);
                }
            }
        };
        this.mutePressed = new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.DialComponent.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Log.d(DialComponent.TAG, "mutePressed id " + DialComponent.this.model.id);
                if (DialComponent.this.model.muteId == Constants.ParameterID.muteOutput) {
                    Boolean boolValueOf = Boolean.valueOf(!DialComponent.this.appState.checkMuted(DialComponent.this.appState.output).booleanValue());
                    DialComponent.this.appState.setMute((byte) 0, boolValueOf.booleanValue());
                    DialComponent.this.appState.setMute((byte) 1, boolValueOf.booleanValue());
                    DialComponent.this.appState.setMute((byte) 2, boolValueOf.booleanValue());
                    DialComponent.this.appState.setMute((byte) 3, boolValueOf.booleanValue());
                } else if (DialComponent.this.model.muteId == Constants.ParameterID.compressorOnOff) {
                    DialComponent.this.appState.setCompressorOnOff(DialComponent.this.appState.output, true ^ DialComponent.this.appState.checkCompressorOn(DialComponent.this.appState.output));
                } else if (DialComponent.this.model.muteId == Constants.ParameterID.talk) {
                    DialComponent.this.appState.setTalk(!DialComponent.this.appState.checkTalk().booleanValue());
                }
                DialComponent.this.appState.appStateCallback.callbackUpdateToValues();
            }
        };
        Log.d(TAG, "context init");
        setup();
    }

    public DialComponent(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.dialListener = new Dial.DialListener() { // from class: tchelicon.com.blenderappandroid.DialComponent.1
            @Override // tchelicon.com.blenderappandroid.Dial.DialListener
            public void onStartEditing(DialModel dialModel) {
                if (DialComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(dialModel.id.getId()))) {
                    return;
                }
                DialComponent.this.appState.slidersBeingEdited.add(Byte.valueOf(dialModel.id.getId()));
            }

            @Override // tchelicon.com.blenderappandroid.Dial.DialListener
            public void valueChanged(DialModel dialModel) {
                byte bFloatValue = (byte) (((double) (dialModel.value.floatValue() * Constants.MaxByteValueAsFloat)) + 0.5d);
                if (DialComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(dialModel.id.getId()))) {
                    DialComponent.this.appState.updateParameter(dialModel.id.getId(), DialComponent.this.appState.output, bFloatValue, false);
                }
            }

            @Override // tchelicon.com.blenderappandroid.Dial.DialListener
            public void onEndEditing(DialModel dialModel) {
                FlurryAnalytics.analytics("Levels", "Edited", Constants.Parameter.parameter(dialModel.id).name.name());
                if (dialModel.id.getId() <= Constants.ParameterID.input6.getId()) {
                    FlurryAnalytics.analyticsLevels("Levels", "Input Values", dialModel.value.floatValue());
                } else if (dialModel.id == Constants.ParameterID.level) {
                    FlurryAnalytics.analyticsLevels("Levels", "Level Values", dialModel.value.floatValue());
                } else if (dialModel.id == Constants.ParameterID.compressor) {
                    FlurryAnalytics.analyticsLevels("Levels", "Compressor Values", dialModel.value.floatValue());
                } else if (dialModel.id == Constants.ParameterID.micGain) {
                    FlurryAnalytics.analyticsLevels("Levels", "MicGain Values", dialModel.value.floatValue());
                }
                int iIndexOf = DialComponent.this.appState.slidersBeingEdited.indexOf(Byte.valueOf(dialModel.id.getId()));
                if (iIndexOf != -1) {
                    DialComponent.this.appState.slidersBeingEdited.remove(iIndexOf);
                }
            }
        };
        this.mutePressed = new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.DialComponent.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Log.d(DialComponent.TAG, "mutePressed id " + DialComponent.this.model.id);
                if (DialComponent.this.model.muteId == Constants.ParameterID.muteOutput) {
                    Boolean boolValueOf = Boolean.valueOf(!DialComponent.this.appState.checkMuted(DialComponent.this.appState.output).booleanValue());
                    DialComponent.this.appState.setMute((byte) 0, boolValueOf.booleanValue());
                    DialComponent.this.appState.setMute((byte) 1, boolValueOf.booleanValue());
                    DialComponent.this.appState.setMute((byte) 2, boolValueOf.booleanValue());
                    DialComponent.this.appState.setMute((byte) 3, boolValueOf.booleanValue());
                } else if (DialComponent.this.model.muteId == Constants.ParameterID.compressorOnOff) {
                    DialComponent.this.appState.setCompressorOnOff(DialComponent.this.appState.output, true ^ DialComponent.this.appState.checkCompressorOn(DialComponent.this.appState.output));
                } else if (DialComponent.this.model.muteId == Constants.ParameterID.talk) {
                    DialComponent.this.appState.setTalk(!DialComponent.this.appState.checkTalk().booleanValue());
                }
                DialComponent.this.appState.appStateCallback.callbackUpdateToValues();
            }
        };
        Log.d(TAG, "context/attrs init");
        setup();
    }

    public DialComponent(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.dialListener = new Dial.DialListener() { // from class: tchelicon.com.blenderappandroid.DialComponent.1
            @Override // tchelicon.com.blenderappandroid.Dial.DialListener
            public void onStartEditing(DialModel dialModel) {
                if (DialComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(dialModel.id.getId()))) {
                    return;
                }
                DialComponent.this.appState.slidersBeingEdited.add(Byte.valueOf(dialModel.id.getId()));
            }

            @Override // tchelicon.com.blenderappandroid.Dial.DialListener
            public void valueChanged(DialModel dialModel) {
                byte bFloatValue = (byte) (((double) (dialModel.value.floatValue() * Constants.MaxByteValueAsFloat)) + 0.5d);
                if (DialComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(dialModel.id.getId()))) {
                    DialComponent.this.appState.updateParameter(dialModel.id.getId(), DialComponent.this.appState.output, bFloatValue, false);
                }
            }

            @Override // tchelicon.com.blenderappandroid.Dial.DialListener
            public void onEndEditing(DialModel dialModel) {
                FlurryAnalytics.analytics("Levels", "Edited", Constants.Parameter.parameter(dialModel.id).name.name());
                if (dialModel.id.getId() <= Constants.ParameterID.input6.getId()) {
                    FlurryAnalytics.analyticsLevels("Levels", "Input Values", dialModel.value.floatValue());
                } else if (dialModel.id == Constants.ParameterID.level) {
                    FlurryAnalytics.analyticsLevels("Levels", "Level Values", dialModel.value.floatValue());
                } else if (dialModel.id == Constants.ParameterID.compressor) {
                    FlurryAnalytics.analyticsLevels("Levels", "Compressor Values", dialModel.value.floatValue());
                } else if (dialModel.id == Constants.ParameterID.micGain) {
                    FlurryAnalytics.analyticsLevels("Levels", "MicGain Values", dialModel.value.floatValue());
                }
                int iIndexOf = DialComponent.this.appState.slidersBeingEdited.indexOf(Byte.valueOf(dialModel.id.getId()));
                if (iIndexOf != -1) {
                    DialComponent.this.appState.slidersBeingEdited.remove(iIndexOf);
                }
            }
        };
        this.mutePressed = new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.DialComponent.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Log.d(DialComponent.TAG, "mutePressed id " + DialComponent.this.model.id);
                if (DialComponent.this.model.muteId == Constants.ParameterID.muteOutput) {
                    Boolean boolValueOf = Boolean.valueOf(!DialComponent.this.appState.checkMuted(DialComponent.this.appState.output).booleanValue());
                    DialComponent.this.appState.setMute((byte) 0, boolValueOf.booleanValue());
                    DialComponent.this.appState.setMute((byte) 1, boolValueOf.booleanValue());
                    DialComponent.this.appState.setMute((byte) 2, boolValueOf.booleanValue());
                    DialComponent.this.appState.setMute((byte) 3, boolValueOf.booleanValue());
                } else if (DialComponent.this.model.muteId == Constants.ParameterID.compressorOnOff) {
                    DialComponent.this.appState.setCompressorOnOff(DialComponent.this.appState.output, true ^ DialComponent.this.appState.checkCompressorOn(DialComponent.this.appState.output));
                } else if (DialComponent.this.model.muteId == Constants.ParameterID.talk) {
                    DialComponent.this.appState.setTalk(!DialComponent.this.appState.checkTalk().booleanValue());
                }
                DialComponent.this.appState.appStateCallback.callbackUpdateToValues();
            }
        };
        Log.d(TAG, "context/attrs/defStyle init");
        setup();
    }

    public void onCreate(AppState appState, DialModel dialModel) {
        this.appState = appState;
        this.model = dialModel;
        setup();
        setBackgroundResource(dialModel.background);
    }

    private void setup() {
        Dial dial = getDial();
        if (dial != null) {
            dial.setDialListener(this.dialListener);
            dial.onCreate(this.model);
        }
        TextView label = getLabel();
        if (label != null) {
            label.setText(this.model.label);
            label.setTextColor(getResources().getColor(this.model.labelTint, null));
        }
        ImageButton muteButton = getMuteButton();
        if (muteButton != null) {
            muteButton.setOnClickListener(this.mutePressed);
            muteButton.setImageResource(this.model.mutedIcon);
        }
    }

    public void updateToValues() {
        if (this.model == null) {
            return;
        }
        if (this.appState.getParameterGroupFor(Byte.valueOf(this.model.id.getId()), Byte.valueOf(this.appState.output), false) != null) {
            this.model.value = Float.valueOf(this.appState.unsignedIntFromByte(r0.three) / Constants.MaxByteValueAsFloat);
        }
        updateUI();
    }

    public void updateUI() {
        Dial dial = getDial();
        if (dial != null) {
            dial.model = this.model;
            dial.updateUI();
        }
        updateMuteButton();
    }

    private void updateMuteButton() {
        ImageButton muteButton = getMuteButton();
        if (muteButton != null) {
            if (getMuteState()) {
                muteButton.setImageTintList(ContextCompat.getColorStateList(getContext(), this.model.mutedColour));
            } else {
                muteButton.setImageTintList(ContextCompat.getColorStateList(getContext(), this.model.unmutedColour));
            }
        }
    }

    boolean getMuteState() {
        if (this.model.muteId == Constants.ParameterID.muteOutput) {
            return this.appState.checkMuted(this.appState.output).booleanValue();
        }
        if (this.model.muteId == Constants.ParameterID.compressorOnOff) {
            return this.appState.checkCompressorOn(this.appState.output);
        }
        if (this.model.muteId == Constants.ParameterID.talk) {
            return this.appState.checkTalk().booleanValue();
        }
        return false;
    }

    private Dial getDial() {
        return (Dial) findViewById(R.id.dial);
    }

    private TextView getLabel() {
        return (TextView) findViewById(R.id.label);
    }

    private ImageButton getMuteButton() {
        return (ImageButton) findViewById(R.id.muteButton);
    }
}
