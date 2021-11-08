package de.baumann.browser.fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import de.baumann.browser.R;

public class Fragment_settings_Profile extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        assert context != null;

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String profile = sp.getString("profile", "profileStandard");

        switch (Objects.requireNonNull(profile)) {
            case "profileStandard":
                setPreferencesFromResource(R.xml.preference_profile_standard, rootKey);
                PreferenceManager.setDefaultValues(context, R.xml.preference_profile_standard, false);
                break;
            case "profileTrusted":
                setPreferencesFromResource(R.xml.preference_profile_trusted, rootKey);
                PreferenceManager.setDefaultValues(context, R.xml.preference_profile_trusted, false);
                break;
            case "profileProtected":
                setPreferencesFromResource(R.xml.preference_profile_protected, rootKey);
                PreferenceManager.setDefaultValues(context, R.xml.preference_profile_protected, false);
                break;
        }
    }
}