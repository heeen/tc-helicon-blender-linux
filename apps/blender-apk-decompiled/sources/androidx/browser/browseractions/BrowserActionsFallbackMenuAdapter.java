package androidx.browser.browseractions;

import android.content.Context;
import android.support.customtabs.R;
import android.support.v4.content.res.ResourcesCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
class BrowserActionsFallbackMenuAdapter extends BaseAdapter {
    private final Context mContext;
    private final List<BrowserActionItem> mMenuItems;

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return i;
    }

    BrowserActionsFallbackMenuAdapter(List<BrowserActionItem> list, Context context) {
        this.mMenuItems = list;
        this.mContext = context;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.mMenuItems.size();
    }

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return this.mMenuItems.get(i);
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolderItem viewHolderItem;
        BrowserActionItem browserActionItem = this.mMenuItems.get(i);
        if (view == null) {
            view = LayoutInflater.from(this.mContext).inflate(R.layout.browser_actions_context_menu_row, (ViewGroup) null);
            viewHolderItem = new ViewHolderItem();
            viewHolderItem.mIcon = (ImageView) view.findViewById(R.id.browser_actions_menu_item_icon);
            viewHolderItem.mText = (TextView) view.findViewById(R.id.browser_actions_menu_item_text);
            view.setTag(viewHolderItem);
        } else {
            viewHolderItem = (ViewHolderItem) view.getTag();
        }
        viewHolderItem.mText.setText(browserActionItem.getTitle());
        if (browserActionItem.getIconId() != 0) {
            viewHolderItem.mIcon.setImageDrawable(ResourcesCompat.getDrawable(this.mContext.getResources(), browserActionItem.getIconId(), null));
        } else {
            viewHolderItem.mIcon.setImageDrawable(null);
        }
        return view;
    }

    private static class ViewHolderItem {
        ImageView mIcon;
        TextView mText;

        private ViewHolderItem() {
        }
    }
}
