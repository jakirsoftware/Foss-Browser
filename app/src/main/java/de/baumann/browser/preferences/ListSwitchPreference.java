package de.baumann.browser.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceViewHolder;

import de.baumann.browser.R;

public class ListSwitchPreference extends ListPreference {

    private String ListSwitchKey;
    private boolean ListSwitchKeyDefaultValue;
    private boolean switchAttached = false;

    public ListSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        ListSwitchKey = null;
        ListSwitchKeyDefaultValue = false;
        TypedArray valueArray;
        if (attrs != null) {
            valueArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ListSwitchPreference, 0, 0);
            ListSwitchKey = valueArray.getString(R.styleable.ListSwitchPreference_listSwitchKey);
            ListSwitchKeyDefaultValue = valueArray.getBoolean(R.styleable.ListSwitchPreference_listSwitchKeyDefaultValue, false);
            valueArray.recycle();
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {

        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getContext());
        final ViewGroup rootView;
        final CheckBox onOffSwitch;
        final CompoundButton.OnCheckedChangeListener checkedChangeListener;
        super.onBindViewHolder(holder);

        rootView = (ViewGroup) holder.itemView;
        if (!switchAttached && (ListSwitchKey != null)) {

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0,0,25,0);

            onOffSwitch = new CheckBox(getContext());
            onOffSwitch.setLayoutParams(params);
            rootView.addView(onOffSwitch);
            switchAttached = true;
            onOffSwitch.setChecked(sp.getBoolean(ListSwitchKey, ListSwitchKeyDefaultValue));
            checkedChangeListener = (buttonView, isChecked) -> {
                if (ListSwitchKey != null) {
                    sp.edit().putBoolean(ListSwitchKey, isChecked).apply();
                }
            };
            onOffSwitch.setOnCheckedChangeListener(checkedChangeListener);
            checkedChangeListener.onCheckedChanged(onOffSwitch, onOffSwitch.isChecked());
        }
    }
}
