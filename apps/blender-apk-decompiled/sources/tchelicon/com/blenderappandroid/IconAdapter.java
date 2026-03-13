package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import tchelicon.com.blenderappandroid.Constants;

/* JADX INFO: loaded from: classes.dex */
public class IconAdapter extends BaseAdapter {
    private Context context;
    Constants.CustomIcon[] customIcons = Constants.CustomIcon.CustomIcons;
    int iconSize;
    Constants.IconID selectedID;
    int sliderNumber;

    @Override // android.widget.Adapter
    public Object getItem(int i) {
        return null;
    }

    @Override // android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    public IconAdapter(Context context, Constants.IconID iconID, int i) {
        this.iconSize = 50;
        this.iconSize = (int) ((Constants.iconSelectIconSize * context.getResources().getDisplayMetrics().density) + 0.5f);
        this.context = context;
        this.selectedID = iconID;
        this.sliderNumber = i;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.customIcons.length;
    }

    @Override // android.widget.Adapter
    public View getView(int i, View view, ViewGroup viewGroup) {
        ImageView imageView;
        TextView textView;
        TextView textView2;
        if (this.customIcons[i].id == Constants.IconID.defaultIcon) {
            if (view == null) {
                textView2 = new TextView(this.context);
                textView2.setLayoutParams(new AbsListView.LayoutParams(this.iconSize, this.iconSize));
                textView2.setGravity(17);
                textView2.setTextSize(32.0f);
            } else {
                textView2 = (TextView) view;
            }
            textView2.setText("" + this.sliderNumber);
            if (this.customIcons[i].id == this.selectedID) {
                textView2.setBackgroundResource(R.color.c26);
                textView2.setTextColor(ContextCompat.getColorStateList(this.context, R.color.c15));
            } else {
                textView2.setBackgroundResource(R.color.c19);
                textView2.setTextColor(ContextCompat.getColorStateList(this.context, R.color.c28));
            }
            return textView2;
        }
        if (this.customIcons[i].id.getId() >= Constants.IconID.A.getId() && this.customIcons[i].id.getId() <= Constants.IconID.Z.getId()) {
            if (view == null || !(view instanceof TextView)) {
                textView = new TextView(this.context);
                textView.setLayoutParams(new AbsListView.LayoutParams(this.iconSize, this.iconSize));
                textView.setGravity(17);
                textView.setTextSize(32.0f);
            } else {
                textView = (TextView) view;
            }
            textView.setText(Character.toString("ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(this.customIcons[i].id.getId() - 1)));
            if (this.customIcons[i].id == this.selectedID) {
                textView.setBackgroundResource(R.color.c26);
                textView.setTextColor(ContextCompat.getColorStateList(this.context, R.color.c15));
            } else {
                textView.setBackgroundResource(R.color.c19);
                textView.setTextColor(ContextCompat.getColorStateList(this.context, R.color.c28));
            }
            return textView;
        }
        if (view == null || !(view instanceof ImageView)) {
            imageView = new ImageView(this.context);
            imageView.setLayoutParams(new AbsListView.LayoutParams(this.iconSize, this.iconSize));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) view;
        }
        if (this.customIcons[i].id == this.selectedID) {
            imageView.setBackgroundResource(R.color.c26);
            imageView.setImageTintList(ContextCompat.getColorStateList(this.context, R.color.c15));
        } else {
            imageView.setBackgroundResource(R.color.c19);
            imageView.setImageTintList(ContextCompat.getColorStateList(this.context, R.color.c28));
        }
        imageView.setImageResource(this.customIcons[i].icon);
        return imageView;
    }
}
