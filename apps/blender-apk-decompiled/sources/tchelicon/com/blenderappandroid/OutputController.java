package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import java.util.Arrays;
import java.util.Iterator;

/* JADX INFO: loaded from: classes.dex */
public class OutputController extends ConstraintLayout {
    private static final String TAG = "OutputController";
    AppState appState;
    private View.OnClickListener outputPressed;
    private View.OnClickListener talkPressed;

    public OutputController(Context context) {
        super(context);
        this.outputPressed = new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.OutputController.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Log.d(OutputController.TAG, "outputPressed tag = " + view.getTag());
                OutputController.this.appState.output = ((Byte) view.getTag()).byteValue();
                switch (OutputController.this.appState.output) {
                    case 0:
                        FlurryAnalytics.analytics("Output", "Selected", "A");
                        break;
                    case 1:
                        FlurryAnalytics.analytics("Output", "Selected", "B");
                        break;
                    case 2:
                        FlurryAnalytics.analytics("Output", "Selected", "C");
                        break;
                    case 3:
                        FlurryAnalytics.analytics("Output", "Selected", "D");
                        break;
                }
                OutputController.this.appState.appStateCallback.callbackUpdateToValues();
            }
        };
        this.talkPressed = new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.OutputController.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                OutputController.this.appState.setTalk(!OutputController.this.appState.checkTalk().booleanValue());
                OutputController.this.updateTalkButton();
                OutputController.this.appState.appStateCallback.callbackUpdateToValues();
            }
        };
        Log.d(TAG, "context init");
        setup();
    }

    public OutputController(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.outputPressed = new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.OutputController.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Log.d(OutputController.TAG, "outputPressed tag = " + view.getTag());
                OutputController.this.appState.output = ((Byte) view.getTag()).byteValue();
                switch (OutputController.this.appState.output) {
                    case 0:
                        FlurryAnalytics.analytics("Output", "Selected", "A");
                        break;
                    case 1:
                        FlurryAnalytics.analytics("Output", "Selected", "B");
                        break;
                    case 2:
                        FlurryAnalytics.analytics("Output", "Selected", "C");
                        break;
                    case 3:
                        FlurryAnalytics.analytics("Output", "Selected", "D");
                        break;
                }
                OutputController.this.appState.appStateCallback.callbackUpdateToValues();
            }
        };
        this.talkPressed = new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.OutputController.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                OutputController.this.appState.setTalk(!OutputController.this.appState.checkTalk().booleanValue());
                OutputController.this.updateTalkButton();
                OutputController.this.appState.appStateCallback.callbackUpdateToValues();
            }
        };
        Log.d(TAG, "context/attrs init");
        setup();
    }

    public OutputController(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.outputPressed = new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.OutputController.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                Log.d(OutputController.TAG, "outputPressed tag = " + view.getTag());
                OutputController.this.appState.output = ((Byte) view.getTag()).byteValue();
                switch (OutputController.this.appState.output) {
                    case 0:
                        FlurryAnalytics.analytics("Output", "Selected", "A");
                        break;
                    case 1:
                        FlurryAnalytics.analytics("Output", "Selected", "B");
                        break;
                    case 2:
                        FlurryAnalytics.analytics("Output", "Selected", "C");
                        break;
                    case 3:
                        FlurryAnalytics.analytics("Output", "Selected", "D");
                        break;
                }
                OutputController.this.appState.appStateCallback.callbackUpdateToValues();
            }
        };
        this.talkPressed = new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.OutputController.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                OutputController.this.appState.setTalk(!OutputController.this.appState.checkTalk().booleanValue());
                OutputController.this.updateTalkButton();
                OutputController.this.appState.appStateCallback.callbackUpdateToValues();
            }
        };
        Log.d(TAG, "context/attrs/defStyle init");
        setup();
    }

    public void onCreate(AppState appState) {
        Log.d(TAG, "onCreate()");
        this.appState = appState;
        setup();
    }

    private void setup() {
        setBackgroundColor(getResources().getColor(R.color.seperator, null));
        setButtonActions();
    }

    public void updateToValues() {
        if (this.appState == null) {
            Log.d(TAG, "updateToValues: appstate was null");
        } else {
            updateOutputButtons();
            updateTalkButton();
        }
    }

    private void updateOutputButtons() {
        ImageView[] imageViewArr = {(ImageView) findViewById(R.id.a_state), (ImageView) findViewById(R.id.b_state), (ImageView) findViewById(R.id.c_state), (ImageView) findViewById(R.id.d_state)};
        ImageView[] imageViewArr2 = {(ImageView) findViewById(R.id.jack_1), (ImageView) findViewById(R.id.jack_2), (ImageView) findViewById(R.id.jack_3), (ImageView) findViewById(R.id.jack_4)};
        int[] iArr = {R.drawable.a_blue, R.drawable.b_blue, R.drawable.c_blue, R.drawable.d_blue};
        int[] iArr2 = {R.drawable.a_grey, R.drawable.b_grey, R.drawable.c_grey, R.drawable.d_grey};
        for (int i = 0; i < 4; i++) {
            if (this.appState.output == i) {
                imageViewArr[i].setVisibility(0);
                imageViewArr[i].setImageResource(iArr[i]);
            } else if (!this.appState.jackSenseStateFor((byte) i)) {
                imageViewArr[i].setVisibility(0);
                imageViewArr[i].setImageResource(iArr2[i]);
            } else {
                imageViewArr[i].setVisibility(4);
            }
            if (this.appState.jackSenseStateFor((byte) i)) {
                imageViewArr2[i].setVisibility(0);
            } else {
                imageViewArr2[i].setVisibility(4);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void updateTalkButton() {
        ImageView imageView = (ImageView) findViewById(R.id.talk_state);
        if (imageView != null) {
            if (this.appState.checkTalk().booleanValue()) {
                imageView.setVisibility(0);
            } else {
                imageView.setVisibility(4);
            }
        }
    }

    private void setButtonActions() {
        byte b = 0;
        Iterator it = Arrays.asList(Integer.valueOf(R.id.Abutton), Integer.valueOf(R.id.Bbutton), Integer.valueOf(R.id.Cbutton), Integer.valueOf(R.id.Dbutton)).iterator();
        while (it.hasNext()) {
            Button button = (Button) findViewById(((Integer) it.next()).intValue());
            if (button != null) {
                button.setTag(Byte.valueOf(b));
                button.setOnClickListener(this.outputPressed);
            } else {
                Log.d(TAG, "setButtonActions output button was null");
            }
            b = (byte) (b + 1);
        }
        Button talkButton = getTalkButton();
        if (talkButton != null) {
            talkButton.setOnClickListener(this.talkPressed);
        }
    }

    private Button getOutputButton(int i) {
        switch (i) {
            case 0:
                return (Button) findViewById(R.id.Abutton);
            case 1:
                return (Button) findViewById(R.id.Bbutton);
            case 2:
                return (Button) findViewById(R.id.Cbutton);
            case 3:
                return (Button) findViewById(R.id.Dbutton);
            default:
                return null;
        }
    }

    private Button getTalkButton() {
        return (Button) findViewById(R.id.talkButton);
    }
}
