package de.baumann.browser.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.baumann.browser.R;
import de.baumann.browser.unit.HelperUnit;

public class Fragment_settings_Delete extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        setPreferencesFromResource(R.xml.preference_delete, rootKey);
        Activity activity = getActivity();
        assert activity != null;
        Context context = getContext();
        assert context != null;

        Preference sp_deleteDatabase = findPreference("sp_deleteDatabase");
        assert sp_deleteDatabase != null;
        sp_deleteDatabase.setOnPreferenceClickListener(preference -> {
            final SharedPreferences sp = getPreferenceScreen().getSharedPreferences();
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);
            builder.setIcon(R.drawable.icon_alert);
            builder.setTitle(R.string.menu_delete);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                dialog.cancel();
                activity.deleteDatabase("Ninja4.db");
                activity.deleteDatabase("faviconView.db");
                assert sp != null;
                sp.edit().putInt("restart_changed", 1).apply();
                activity.finish();
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(context, dialog);
            return false;
        });
    }
}