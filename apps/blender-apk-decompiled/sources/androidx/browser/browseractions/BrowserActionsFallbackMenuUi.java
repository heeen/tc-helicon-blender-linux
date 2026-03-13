package androidx.browser.browseractions;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.customtabs.R;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.ActivityChooserView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
class BrowserActionsFallbackMenuUi implements AdapterView.OnItemClickListener {
    private static final String TAG = "BrowserActionskMenuUi";
    private BrowserActionsFallbackMenuDialog mBrowserActionsDialog;
    private final Context mContext;
    private final List<BrowserActionItem> mMenuItems;
    private BrowserActionsFallMenuUiListener mMenuUiListener;
    private final Uri mUri;

    @VisibleForTesting
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    interface BrowserActionsFallMenuUiListener {
        void onMenuShown(View view);
    }

    BrowserActionsFallbackMenuUi(Context context, Uri uri, List<BrowserActionItem> list) {
        this.mContext = context;
        this.mUri = uri;
        this.mMenuItems = list;
    }

    @VisibleForTesting
    @RestrictTo({RestrictTo.Scope.LIBRARY_GROUP})
    void setMenuUiListener(BrowserActionsFallMenuUiListener browserActionsFallMenuUiListener) {
        this.mMenuUiListener = browserActionsFallMenuUiListener;
    }

    public void displayMenu() {
        final View viewInflate = LayoutInflater.from(this.mContext).inflate(R.layout.browser_actions_context_menu_page, (ViewGroup) null);
        this.mBrowserActionsDialog = new BrowserActionsFallbackMenuDialog(this.mContext, initMenuView(viewInflate));
        this.mBrowserActionsDialog.setContentView(viewInflate);
        if (this.mMenuUiListener != null) {
            this.mBrowserActionsDialog.setOnShowListener(new DialogInterface.OnShowListener() { // from class: androidx.browser.browseractions.BrowserActionsFallbackMenuUi.1
                @Override // android.content.DialogInterface.OnShowListener
                public void onShow(DialogInterface dialogInterface) {
                    BrowserActionsFallbackMenuUi.this.mMenuUiListener.onMenuShown(viewInflate);
                }
            });
        }
        this.mBrowserActionsDialog.show();
    }

    private BrowserActionsFallbackMenuView initMenuView(View view) {
        BrowserActionsFallbackMenuView browserActionsFallbackMenuView = (BrowserActionsFallbackMenuView) view.findViewById(R.id.browser_actions_menu_view);
        final TextView textView = (TextView) view.findViewById(R.id.browser_actions_header_text);
        textView.setText(this.mUri.toString());
        textView.setOnClickListener(new View.OnClickListener() { // from class: androidx.browser.browseractions.BrowserActionsFallbackMenuUi.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view2) {
                if (TextViewCompat.getMaxLines(textView) == Integer.MAX_VALUE) {
                    textView.setMaxLines(1);
                    textView.setEllipsize(TextUtils.TruncateAt.END);
                } else {
                    textView.setMaxLines(ActivityChooserView.ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED);
                    textView.setEllipsize(null);
                }
            }
        });
        ListView listView = (ListView) view.findViewById(R.id.browser_actions_menu_items);
        listView.setAdapter((ListAdapter) new BrowserActionsFallbackMenuAdapter(this.mMenuItems, this.mContext));
        listView.setOnItemClickListener(this);
        return browserActionsFallbackMenuView;
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        try {
            this.mMenuItems.get(i).getAction().send();
            this.mBrowserActionsDialog.dismiss();
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Failed to send custom item action", e);
        }
    }
}
