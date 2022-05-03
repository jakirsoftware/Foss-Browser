package de.baumann.browser.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import de.baumann.browser.R;

public class EditTextSwitchPreference extends EditTextPreference {

    private String EditTextSwitchKey;
    private boolean EditTextSwitchKeyDefaultValue;
    private boolean switchAttached = false;

    public EditTextSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        EditTextSwitchKey = null;
        EditTextSwitchKeyDefaultValue = false;
        TypedArray valueArray;
        if (attrs != null) {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.EditTextSwitchPreference, 0, 0);
            EditTextSwitchKey = valueArray.getString(R.styleable.EditTextSwitchPreference_editTextSwitchKey);
            EditTextSwitchKeyDefaultValue = valueArray.getBoolean(R.styleable.EditTextSwitchPreference_editTextSwitchKeyDefaultValue, false);
            valueArray.recycle();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        final ViewGroup rootView;
        final CheckBox onOffSwitch;
        final CompoundButton.OnCheckedChangeListener checkedChangeListener;
        Context context = getContext();
        super.onBindViewHolder(holder);
        rootView = (ViewGroup) holder.itemView;


        //holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.red, null));

        if (!switchAttached && (EditTextSwitchKey != null)) {
            onOffSwitch = new CheckBox(context);
            rootView.addView(onOffSwitch);
            switchAttached = true;
            onOffSwitch.setChecked(sp.getBoolean(EditTextSwitchKey, EditTextSwitchKeyDefaultValue));

            if (EditTextSwitchKey.equals("filter_01")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.red, null));
            } else if (EditTextSwitchKey.equals("filter_02")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.pink, null));
            } else if (EditTextSwitchKey.equals("filter_03")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.purple, null));
            } else if (EditTextSwitchKey.equals("filter_04")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.blue, null));
            } else if (EditTextSwitchKey.equals("filter_05")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.teal, null));
            } else if (EditTextSwitchKey.equals("filter_06")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.green, null));
            } else if (EditTextSwitchKey.equals("filter_07")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.lime, null));
            } else if (EditTextSwitchKey.equals("filter_08")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.yellow, null));
            } else if (EditTextSwitchKey.equals("filter_09")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.orange, null));
            } else if (EditTextSwitchKey.equals("filter_10")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.brown, null));
            } else if (EditTextSwitchKey.equals("filter_11")) {
                holder.itemView.setBackgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.grey, null));
            } else if (EditTextSwitchKey.equals("filter_12")) {
                TypedValue typedValue = new TypedValue();
                context.getTheme().resolveAttribute(R.attr.colorSurfaceVariant, typedValue, true);
                int color = typedValue.data;
                holder.itemView.setBackgroundColor(color);
            }

            checkedChangeListener = (buttonView, isChecked) -> {
                if (EditTextSwitchKey != null) {
                    sp.edit().putBoolean(EditTextSwitchKey, isChecked).apply();
                }
            };


            onOffSwitch.setOnCheckedChangeListener(checkedChangeListener);
            checkedChangeListener.onCheckedChanged(onOffSwitch, onOffSwitch.isChecked());
        }
    }
}
