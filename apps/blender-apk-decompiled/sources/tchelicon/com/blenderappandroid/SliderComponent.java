package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;
import io.github.douglasjunior.androidSimpleTooltip.SimpleTooltip;
import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class SliderComponent extends ConstraintLayout {
    private static final String TAG = "SliderComponent";
    AppState appState;
    SliderModel model;
    private SeekBar.OnSeekBarChangeListener seekBarListener;

    public SliderComponent(Context context) {
        super(context);
        this.seekBarListener = new SeekBar.OnSeekBarChangeListener() { // from class: tchelicon.com.blenderappandroid.SliderComponent.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
                FlurryAnalytics.analytics("Levels", "Edited", Constants.Parameter.parameter(SliderComponent.this.model.id).name.name());
                if (SliderComponent.this.model.id.getId() <= Constants.ParameterID.input6.getId()) {
                    FlurryAnalytics.analyticsLevels("Levels", "Input Values", SliderComponent.this.model.value.floatValue());
                } else if (SliderComponent.this.model.id == Constants.ParameterID.level) {
                    FlurryAnalytics.analyticsLevels("Levels", "Level Values", SliderComponent.this.model.value.floatValue());
                } else if (SliderComponent.this.model.id == Constants.ParameterID.compressor) {
                    FlurryAnalytics.analyticsLevels("Levels", "Compressor Values", SliderComponent.this.model.value.floatValue());
                } else if (SliderComponent.this.model.id == Constants.ParameterID.micGain) {
                    FlurryAnalytics.analyticsLevels("Levels", "MicGain Values", SliderComponent.this.model.value.floatValue());
                }
                int iIndexOf = SliderComponent.this.appState.slidersBeingEdited.indexOf(Byte.valueOf(SliderComponent.this.model.id.getId()));
                if (iIndexOf != -1) {
                    SliderComponent.this.appState.slidersBeingEdited.remove(iIndexOf);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (SliderComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(SliderComponent.this.model.id.getId()))) {
                    return;
                }
                SliderComponent.this.appState.slidersBeingEdited.add(Byte.valueOf(SliderComponent.this.model.id.getId()));
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                SliderComponent.this.model.value = Float.valueOf(seekBar.getProgress() / seekBar.getMax());
                byte bFloatValue = (byte) (((double) (SliderComponent.this.model.value.floatValue() * Constants.MaxByteValueAsFloat)) + 0.5d);
                if (SliderComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(SliderComponent.this.model.id.getId()))) {
                    SliderComponent.this.appState.updateParameter(SliderComponent.this.model.id.getId(), SliderComponent.this.appState.output, bFloatValue, false);
                }
            }
        };
        Log.d(TAG, "context init");
        setup();
    }

    public SliderComponent(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.seekBarListener = new SeekBar.OnSeekBarChangeListener() { // from class: tchelicon.com.blenderappandroid.SliderComponent.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
                FlurryAnalytics.analytics("Levels", "Edited", Constants.Parameter.parameter(SliderComponent.this.model.id).name.name());
                if (SliderComponent.this.model.id.getId() <= Constants.ParameterID.input6.getId()) {
                    FlurryAnalytics.analyticsLevels("Levels", "Input Values", SliderComponent.this.model.value.floatValue());
                } else if (SliderComponent.this.model.id == Constants.ParameterID.level) {
                    FlurryAnalytics.analyticsLevels("Levels", "Level Values", SliderComponent.this.model.value.floatValue());
                } else if (SliderComponent.this.model.id == Constants.ParameterID.compressor) {
                    FlurryAnalytics.analyticsLevels("Levels", "Compressor Values", SliderComponent.this.model.value.floatValue());
                } else if (SliderComponent.this.model.id == Constants.ParameterID.micGain) {
                    FlurryAnalytics.analyticsLevels("Levels", "MicGain Values", SliderComponent.this.model.value.floatValue());
                }
                int iIndexOf = SliderComponent.this.appState.slidersBeingEdited.indexOf(Byte.valueOf(SliderComponent.this.model.id.getId()));
                if (iIndexOf != -1) {
                    SliderComponent.this.appState.slidersBeingEdited.remove(iIndexOf);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (SliderComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(SliderComponent.this.model.id.getId()))) {
                    return;
                }
                SliderComponent.this.appState.slidersBeingEdited.add(Byte.valueOf(SliderComponent.this.model.id.getId()));
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                SliderComponent.this.model.value = Float.valueOf(seekBar.getProgress() / seekBar.getMax());
                byte bFloatValue = (byte) (((double) (SliderComponent.this.model.value.floatValue() * Constants.MaxByteValueAsFloat)) + 0.5d);
                if (SliderComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(SliderComponent.this.model.id.getId()))) {
                    SliderComponent.this.appState.updateParameter(SliderComponent.this.model.id.getId(), SliderComponent.this.appState.output, bFloatValue, false);
                }
            }
        };
        Log.d(TAG, "context/attrs init");
        setup();
    }

    public SliderComponent(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.seekBarListener = new SeekBar.OnSeekBarChangeListener() { // from class: tchelicon.com.blenderappandroid.SliderComponent.1
            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStopTrackingTouch(SeekBar seekBar) {
                FlurryAnalytics.analytics("Levels", "Edited", Constants.Parameter.parameter(SliderComponent.this.model.id).name.name());
                if (SliderComponent.this.model.id.getId() <= Constants.ParameterID.input6.getId()) {
                    FlurryAnalytics.analyticsLevels("Levels", "Input Values", SliderComponent.this.model.value.floatValue());
                } else if (SliderComponent.this.model.id == Constants.ParameterID.level) {
                    FlurryAnalytics.analyticsLevels("Levels", "Level Values", SliderComponent.this.model.value.floatValue());
                } else if (SliderComponent.this.model.id == Constants.ParameterID.compressor) {
                    FlurryAnalytics.analyticsLevels("Levels", "Compressor Values", SliderComponent.this.model.value.floatValue());
                } else if (SliderComponent.this.model.id == Constants.ParameterID.micGain) {
                    FlurryAnalytics.analyticsLevels("Levels", "MicGain Values", SliderComponent.this.model.value.floatValue());
                }
                int iIndexOf = SliderComponent.this.appState.slidersBeingEdited.indexOf(Byte.valueOf(SliderComponent.this.model.id.getId()));
                if (iIndexOf != -1) {
                    SliderComponent.this.appState.slidersBeingEdited.remove(iIndexOf);
                }
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (SliderComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(SliderComponent.this.model.id.getId()))) {
                    return;
                }
                SliderComponent.this.appState.slidersBeingEdited.add(Byte.valueOf(SliderComponent.this.model.id.getId()));
            }

            @Override // android.widget.SeekBar.OnSeekBarChangeListener
            public void onProgressChanged(SeekBar seekBar, int i2, boolean z) {
                SliderComponent.this.model.value = Float.valueOf(seekBar.getProgress() / seekBar.getMax());
                byte bFloatValue = (byte) (((double) (SliderComponent.this.model.value.floatValue() * Constants.MaxByteValueAsFloat)) + 0.5d);
                if (SliderComponent.this.appState.slidersBeingEdited.contains(Byte.valueOf(SliderComponent.this.model.id.getId()))) {
                    SliderComponent.this.appState.updateParameter(SliderComponent.this.model.id.getId(), SliderComponent.this.appState.output, bFloatValue, false);
                }
            }
        };
        Log.d(TAG, "context/attrs/defStyle init");
        setup();
    }

    public void onCreate(AppState appState, SliderModel sliderModel) {
        this.appState = appState;
        this.model = sliderModel;
        setup();
        setBackgroundColor(sliderModel.background);
    }

    private void setup() {
        updateSlider();
        ImageButton iconButton = getIconButton();
        if (iconButton != null) {
            iconButton.setTag(Integer.valueOf(this.model.id.getId()));
        }
    }

    private void updateSlider() {
        VerticalSeekBar slider = getSlider();
        if (slider != null) {
            slider.setOnSeekBarChangeListener(this.seekBarListener);
            if (this.model.jackSensed) {
                switch (this.model.id) {
                    case input1:
                        slider.setProgressDrawable(getResources().getDrawable(R.drawable.slider_1));
                        slider.setThumb(getResources().getDrawable(R.drawable.slider_thumb_1));
                        break;
                    case input2:
                        slider.setProgressDrawable(getResources().getDrawable(R.drawable.slider_2));
                        slider.setThumb(getResources().getDrawable(R.drawable.slider_thumb_2));
                        break;
                    case input3:
                        slider.setProgressDrawable(getResources().getDrawable(R.drawable.slider_3));
                        slider.setThumb(getResources().getDrawable(R.drawable.slider_thumb_3));
                        break;
                    case input4:
                        slider.setProgressDrawable(getResources().getDrawable(R.drawable.slider_4));
                        slider.setThumb(getResources().getDrawable(R.drawable.slider_thumb_4));
                        break;
                    case input5:
                        slider.setProgressDrawable(getResources().getDrawable(R.drawable.slider_5));
                        slider.setThumb(getResources().getDrawable(R.drawable.slider_thumb_5));
                        break;
                    case input6:
                        slider.setProgressDrawable(getResources().getDrawable(R.drawable.slider_6));
                        slider.setThumb(getResources().getDrawable(R.drawable.slider_thumb_6));
                        break;
                }
            }
            slider.setProgressDrawable(getResources().getDrawable(R.drawable.slider_no_jacksense));
            slider.setThumb(getResources().getDrawable(R.drawable.slider_thumb_no_jacksense));
            return;
        }
        Log.d(TAG, "Setup Slider was null");
    }

    public void updateToValues() {
        if (this.model == null) {
            return;
        }
        if (this.model.jackSensed != this.appState.jackSenseStateFor(this.model.id)) {
            this.model.jackSensed = this.appState.jackSenseStateFor(this.model.id);
            SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyPreferences", 0);
            SharedPreferences.Editor editorEdit = sharedPreferences.edit();
            int i = sharedPreferences.getInt(Constants.kInputTipShowCount, 0);
            boolean z = sharedPreferences.getBoolean(Constants.kInputTipShowThisLaunch, false);
            if (i < Constants.maxNumberOfInputTipShowCount && !z) {
                editorEdit.putInt(Constants.kInputTipShowCount, i + 1);
                editorEdit.putBoolean(Constants.kInputTipShowThisLaunch, true);
                editorEdit.apply();
                showTip();
            }
            updateSlider();
        }
        if (this.appState.getParameterGroupFor(Byte.valueOf(this.model.id.getId()), Byte.valueOf(this.appState.output), false) != null) {
            float fUnsignedIntFromByte = this.appState.unsignedIntFromByte(r0.three) / Constants.MaxByteValueAsFloat;
            if (this.model.value.floatValue() > fUnsignedIntFromByte && this.model.value.floatValue() < fUnsignedIntFromByte + (1.0f / Constants.MaxByteValueAsFloat)) {
                return;
            }
            this.model.value = Float.valueOf(this.appState.unsignedIntFromByte(r0.three) / Constants.MaxByteValueAsFloat);
        }
        int iIndexOf = Constants.ParameterID.InputParameterIDs.indexOf(this.model.id);
        if (iIndexOf != -1 && iIndexOf < 6) {
            this.model.icon = this.appState.iconState.get(iIndexOf);
        }
        updateUI();
    }

    public void updateUI() {
        ImageButton iconButton = getIconButton();
        if (iconButton != null) {
            boolean z = this.model.icon.getId() >= Constants.IconID.A.getId() && this.model.icon.getId() <= Constants.IconID.Z.getId();
            TextView iconText = getIconText();
            if (iconText != null) {
                if (this.model.jackSensed) {
                    iconText.setTextColor(this.model.tint);
                } else {
                    iconText.setTextColor(this.model.disabledIcon);
                }
                if (this.model.icon == Constants.IconID.defaultIcon) {
                    iconText.setText(Integer.toString(this.model.id.getId() + 1));
                } else if (z) {
                    iconText.setText(Character.toString("ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(this.model.icon.getId() - 1)));
                } else {
                    iconText.setText("");
                }
            }
            if (this.model.icon == Constants.IconID.defaultIcon || z) {
                iconButton.setImageResource(android.R.color.transparent);
            } else {
                Constants.CustomIcon[] customIconArr = Constants.CustomIcon.CustomIcons;
                int length = customIconArr.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    Constants.CustomIcon customIcon = customIconArr[i];
                    if (customIcon.id == this.model.icon) {
                        iconButton.setImageResource(customIcon.icon);
                        int[][] iArr = {new int[0]};
                        if (this.model.jackSensed) {
                            iconButton.setImageTintList(new ColorStateList(iArr, new int[]{this.model.tint}));
                        } else {
                            iconButton.setImageTintList(new ColorStateList(iArr, new int[]{this.model.disabledIcon}));
                        }
                    } else {
                        i++;
                    }
                }
            }
        }
        updateTo(this.model.value);
    }

    private void showTip() {
        ImageButton iconButton = getIconButton();
        if (iconButton != null) {
            new SimpleTooltip.Builder(getContext()).anchorView(iconButton).text("Tap to label your inputs").gravity(48).animated(false).transparentOverlay(true).backgroundColor(getResources().getColor(R.color.c14)).arrowColor(getResources().getColor(R.color.c14)).textColor(getResources().getColor(R.color.c13)).build().show();
        }
    }

    private void updateTo(Float f) {
        VerticalSeekBar slider = getSlider();
        if (slider != null) {
            slider.setProgress((int) (this.model.value.floatValue() * slider.getMax()));
        }
    }

    private VerticalSeekBar getSlider() {
        return (VerticalSeekBar) findViewById(R.id.slider);
    }

    private ImageButton getIconButton() {
        return (ImageButton) findViewById(R.id.iconButton);
    }

    private TextView getIconText() {
        return (TextView) findViewById(R.id.iconText);
    }
}
