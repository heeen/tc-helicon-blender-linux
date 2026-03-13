package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class DialsController extends ConstraintLayout {
    private static final String TAG = "DialsController";
    AppState appState;

    public void updateUI() {
    }

    public DialsController(Context context) {
        super(context);
        Log.d(TAG, "context init");
        setup();
    }

    public DialsController(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Log.d(TAG, "context/attrs init");
        setup();
    }

    public DialsController(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        Log.d(TAG, "context/attrs/defStyle init");
        setup();
    }

    public void onCreate(AppState appState) {
        Log.d(TAG, "onCreate()");
        this.appState = appState;
        for (DialModel dialModel : DialModel.defaultDialModels) {
            DialComponent dialComponent = getDialComponent(dialModel.id);
            if (dialComponent != null) {
                dialComponent.setBackgroundColor(dialModel.background);
                dialComponent.onCreate(appState, dialModel);
            }
        }
        setup();
    }

    private void setup() {
        setBackgroundColor(getResources().getColor(R.color.seperator, null));
    }

    public void updateToValues() {
        for (Constants.ParameterID parameterID : new Constants.ParameterID[]{Constants.ParameterID.level, Constants.ParameterID.compressor, Constants.ParameterID.micGain}) {
            DialComponent dialComponent = getDialComponent(parameterID);
            if (dialComponent != null) {
                dialComponent.updateToValues();
            }
        }
    }

    private DialComponent getDialComponent(Constants.ParameterID parameterID) {
        switch (parameterID) {
            case level:
                return (DialComponent) findViewById(R.id.dial_component_level);
            case compressor:
                return (DialComponent) findViewById(R.id.dial_component_compressor);
            case micGain:
                return (DialComponent) findViewById(R.id.dial_component_roommic);
            default:
                return null;
        }
    }
}
