package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;

/* JADX INFO: loaded from: classes.dex */
public class MixController extends ConstraintLayout {
    private static final String TAG = "MixController";
    AppState appState;

    private void setup() {
    }

    public void updateUI() {
    }

    public MixController(Context context) {
        super(context);
        Log.d(TAG, "context init");
        setup();
    }

    public MixController(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        Log.d(TAG, "context/attrs init");
        setup();
    }

    public MixController(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        Log.d(TAG, "context/attrs/defStyle init");
        setup();
    }

    public void onCreate(AppState appState) {
        this.appState = appState;
        Log.d(TAG, "onCreate()");
        if (appState == null) {
            Log.d(TAG, "onCreate() appState is null");
        }
        SlidersController slidersController = getSlidersController();
        if (slidersController != null) {
            slidersController.onCreate(appState);
        } else {
            Log.d(TAG, "onCreate() slidersController is null");
        }
        DialsController dialsController = (DialsController) findViewById(R.id.dials_controller);
        if (dialsController != null) {
            dialsController.onCreate(appState);
        }
        OutputController outputController = (OutputController) findViewById(R.id.output_controller);
        if (outputController != null) {
            outputController.onCreate(appState);
        }
        setup();
        updateToValues();
    }

    public void updateToValues() {
        SlidersController slidersController = getSlidersController();
        if (slidersController != null) {
            slidersController.updateToValues();
        } else {
            Log.d(TAG, "updateToValues() slidersController is null");
        }
        OutputController outputController = (OutputController) findViewById(R.id.output_controller);
        if (outputController != null) {
            outputController.updateToValues();
        } else {
            Log.d(TAG, "updateToValues() outputController is null");
        }
        DialsController dialsController = (DialsController) findViewById(R.id.dials_controller);
        if (dialsController != null) {
            dialsController.updateToValues();
        } else {
            Log.d(TAG, "updateToValues() dialsController is null");
        }
    }

    private void updateTo(Float f) {
        getSlider();
    }

    private SlidersController getSlidersController() {
        return (SlidersController) findViewById(R.id.slidersController);
    }

    private VerticalSlider getSlider() {
        return (VerticalSlider) findViewById(R.id.slider);
    }
}
