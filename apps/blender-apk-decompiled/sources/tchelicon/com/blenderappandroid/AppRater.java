package tchelicon.com.blenderappandroid;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/* JADX INFO: loaded from: classes.dex */
public class AppRater {
    private static final String APP_TITLE = "Blender";
    private static final int DAYS_UNTIL_PROMPT = 3;
    private static final int LAUNCHES_UNTIL_PROMPT = 3;
    private static final boolean alwaysShow = true;

    public static void app_launched(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("apprater", 0);
        SharedPreferences.Editor editorEdit = sharedPreferences.edit();
        editorEdit.putLong("launch_count", sharedPreferences.getLong("launch_count", 0L) + 1);
        if (Long.valueOf(sharedPreferences.getLong("date_firstlaunch", 0L)).longValue() == 0) {
            editorEdit.putLong("date_firstlaunch", Long.valueOf(System.currentTimeMillis()).longValue());
        }
        Log.d("AppRater", "Show Dialog");
        showRateDialog(context, editorEdit);
        editorEdit.commit();
    }

    public static void showRateDialog(final Context context, final SharedPreferences.Editor editor) {
        final Dialog dialog = new Dialog(context);
        dialog.setTitle("Rate Blender");
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(1);
        TextView textView = new TextView(context);
        textView.setText("If you enjoy using Blender, please take a moment to rate it. Thanks for your support!");
        textView.setWidth(400);
        textView.setPadding(10, 0, 10, 10);
        linearLayout.addView(textView);
        Button button = new Button(context);
        button.setText("Rate Blender");
        button.setOnClickListener(new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.AppRater.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                editor.putBoolean("dontshowagain", AppRater.alwaysShow);
                editor.commit();
                context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse("market://details?id=" + context.getPackageName())));
                dialog.dismiss();
            }
        });
        linearLayout.addView(button);
        Button button2 = new Button(context);
        button2.setText("Remind me later");
        button2.setOnClickListener(new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.AppRater.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        linearLayout.addView(button2);
        Button button3 = new Button(context);
        button3.setText("No, thanks");
        button3.setOnClickListener(new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.AppRater.3
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (editor != null) {
                    editor.putBoolean("dontshowagain", AppRater.alwaysShow);
                    editor.commit();
                }
                dialog.dismiss();
            }
        });
        linearLayout.addView(button3);
        dialog.setContentView(linearLayout);
        dialog.show();
    }
}
