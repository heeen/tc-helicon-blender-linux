package tchelicon.com.blenderappandroid;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.Arrays;

/* JADX INFO: loaded from: classes.dex */
public class PresetAdapter extends ArrayAdapter {
    Context context;
    int editTextID;
    String[] fileNames;
    boolean ignoreTextChange;
    boolean isPresetEditMode;
    private final LayoutInflater layoutInflater;
    String[] presetNames;
    boolean[] selected;

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public Object getItem(int i) {
        return null;
    }

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public long getItemId(int i) {
        return 0L;
    }

    PresetAdapter(Context context) {
        super(context, 0);
        this.fileNames = new String[0];
        this.presetNames = new String[0];
        this.isPresetEditMode = false;
        this.ignoreTextChange = false;
        this.editTextID = 0;
        this.context = context;
        this.layoutInflater = LayoutInflater.from(context);
        setupData(context);
    }

    public void setupData(Context context) {
        FileSystem fileSystem = new FileSystem();
        String[] strArrListPresetFiles = fileSystem.listPresetFiles(context);
        if (strArrListPresetFiles != null) {
            this.fileNames = strArrListPresetFiles;
            String[] strArrListPresetNames = fileSystem.listPresetNames(context, this.fileNames);
            if (strArrListPresetNames != null) {
                this.presetNames = strArrListPresetNames;
            }
        }
        this.selected = new boolean[this.presetNames.length];
        Arrays.fill(this.selected, false);
    }

    public void setEditMode(boolean z) {
        this.isPresetEditMode = z;
        Arrays.fill(this.selected, false);
    }

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public int getCount() {
        if (this.presetNames != null) {
            return this.presetNames.length;
        }
        return 0;
    }

    @Override // android.widget.ArrayAdapter, android.widget.Adapter
    public View getView(final int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = this.layoutInflater.inflate(R.layout.preset_edit, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.editText = (EditText) view.findViewById(R.id.edit_text);
            viewHolder.selection_dot = (ImageButton) view.findViewById(R.id.selection_dot);
            viewHolder.textView = (TextView) view.findViewById(R.id.text_view);
            view.setTag(viewHolder);
            viewHolder.editText.setOnTouchListener(new View.OnTouchListener() { // from class: tchelicon.com.blenderappandroid.PresetAdapter.1
                @Override // android.view.View.OnTouchListener
                public boolean onTouch(View view2, MotionEvent motionEvent) {
                    PresetAdapter.this.editTextID = view2.getId();
                    return false;
                }
            });
            viewHolder.editText.addTextChangedListener(new TextWatcher() { // from class: tchelicon.com.blenderappandroid.PresetAdapter.2
                @Override // android.text.TextWatcher
                public void afterTextChanged(Editable editable) {
                }

                @Override // android.text.TextWatcher
                public void beforeTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
                }

                @Override // android.text.TextWatcher
                public void onTextChanged(CharSequence charSequence, int i2, int i3, int i4) {
                    if (PresetAdapter.this.ignoreTextChange) {
                        PresetAdapter.this.ignoreTextChange = false;
                    } else if (charSequence.length() > 0) {
                        FileSystem fileSystem = new FileSystem();
                        PresetAdapter.this.presetNames[PresetAdapter.this.editTextID] = charSequence.toString();
                        fileSystem.saveNewPresetName(PresetAdapter.this.context, PresetAdapter.this.fileNames[PresetAdapter.this.editTextID], charSequence.toString());
                    }
                }
            });
            viewHolder.textView.setTextColor(ContextCompat.getColorStateList(this.context, R.color.mainTint));
            viewHolder.editText.setTextColor(ContextCompat.getColorStateList(this.context, R.color.mainTint));
            viewHolder.selection_dot.setImageTintList(ContextCompat.getColorStateList(this.context, R.color.mainTint));
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        if (this.selected[i]) {
            viewHolder.selection_dot.setImageResource(R.drawable.dot_on);
        } else {
            viewHolder.selection_dot.setImageResource(R.drawable.dot_off);
        }
        viewHolder.editText.setId(i);
        viewHolder.selection_dot.setOnClickListener(new View.OnClickListener() { // from class: tchelicon.com.blenderappandroid.PresetAdapter.3
            @Override // android.view.View.OnClickListener
            public void onClick(View view2) {
                PresetAdapter.this.selected[i] = !PresetAdapter.this.selected[i];
                if (PresetAdapter.this.selected[i]) {
                    ((ImageButton) view2).setImageResource(R.drawable.dot_on);
                } else {
                    ((ImageButton) view2).setImageResource(R.drawable.dot_off);
                }
            }
        });
        if (this.isPresetEditMode) {
            viewHolder.textView.setVisibility(8);
            viewHolder.editText.setVisibility(0);
            this.ignoreTextChange = true;
            viewHolder.editText.setText(this.presetNames[i]);
            viewHolder.selection_dot.setVisibility(0);
            viewHolder.selection_dot.setId(i);
        } else {
            viewHolder.textView.setVisibility(0);
            viewHolder.editText.setVisibility(8);
            viewHolder.textView.setText(this.presetNames[i]);
            viewHolder.selection_dot.setVisibility(8);
        }
        return view;
    }

    private static class ViewHolder {
        EditText editText;
        ImageButton selection_dot;
        TextView textView;

        private ViewHolder() {
        }
    }
}
