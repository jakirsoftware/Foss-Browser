package de.baumann.browser.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.color.DynamicColors;

import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.fragment.Fragment_settings_Profile;
import de.baumann.browser.unit.HelperUnit;

public class Settings_Profile extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.md_theme_light_onBackground));

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        HelperUnit.initTheme(this);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new Fragment_settings_Profile())
                .commit();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String profile = sp.getString("profileToEdit", "profileStandard");

        switch (Objects.requireNonNull(profile)) {
            case "profileStandard":
                setTitle(getString(R.string.setting_title_profiles_standard));
                break;
            case "profileTrusted":
                setTitle(getString(R.string.setting_title_profiles_trusted));
                break;
            case "profileProtected":
                setTitle(getString(R.string.setting_title_profiles_protected));
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}
