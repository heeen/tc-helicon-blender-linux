package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class SlidersController extends ConstraintLayout {
    private static final String TAG = "SlidersController";
    AppState appState;

    private void updateTo(Float f) {
    }

    public void updateUI() {
    }

    public SlidersController(Context context) {
        super(context);
        Log.d(TAG, "context init");
        setup();
    }

    public SlidersController(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Log.d(TAG, "context/attrs init");
        setup();
    }

    public SlidersController(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        Log.d(TAG, "context/attrs/defStyle init");
        setup();
    }

    public void onCreate(AppState appState) {
        Log.d(TAG, "onCreate()");
        this.appState = appState;
        for (SliderModel sliderModel : SliderModel.defaultSliderModels) {
            SliderComponent sliderComponent = getSliderComponent(sliderModel.id);
            if (sliderComponent != null) {
                sliderComponent.onCreate(appState, sliderModel);
            }
        }
        setup();
    }

    private void setup() {
        setBackgroundColor(getResources().getColor(R.color.seperator, null));
    }

    public void updateToValues() {
        for (Constants.ParameterID parameterID : new Constants.ParameterID[]{Constants.ParameterID.input1, Constants.ParameterID.input2, Constants.ParameterID.input3, Constants.ParameterID.input4, Constants.ParameterID.input4, Constants.ParameterID.input5, Constants.ParameterID.input6}) {
            SliderComponent sliderComponent = getSliderComponent(parameterID);
            if (sliderComponent != null) {
                sliderComponent.updateToValues();
            }
        }
    }

    private SliderComponent getSliderComponent(Constants.ParameterID parameterID) {
        switch (parameterID) {
            case input1:
                return (SliderComponent) findViewById(R.id.slider1Component);
            case input2:
                return (SliderComponent) findViewById(R.id.slider2Component);
            case input3:
                return (SliderComponent) findViewById(R.id.slider3Component);
            case input4:
                return (SliderComponent) findViewById(R.id.slider4Component);
            case input5:
                return (SliderComponent) findViewById(R.id.slider5Component);
            case input6:
                return (SliderComponent) findViewById(R.id.slider6Component);
            default:
                return null;
        }
    }
}
