package de.baumann.browser.activity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;
import java.util.Objects;

import de.baumann.browser.browser.List_protected;
import de.baumann.browser.browser.List_standard;
import de.baumann.browser.browser.List_trusted;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.R;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.ProfilesListAdapter;
import de.baumann.browser.view.NinjaToast;

public class ProfilesList extends AppCompatActivity {

    private ProfilesListAdapter adapter;
    private List<String> list;
    private List_protected listProtected;
    private List_standard listStandard;
    private List_trusted listTrusted;

    private String listToLoad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.md_theme_light_onBackground));

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        HelperUnit.initTheme(this);
        setContentView(R.layout.activity_settings_profile_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        listToLoad = sp.getString("listToLoad", "standard");

        if (listToLoad != null) {
            switch (listToLoad) {
                case "protected":
                    setTitle(R.string.setting_title_profiles_protectedList);
                    break;
                case "standard":
                    setTitle(R.string.setting_title_profiles_standardList);
                    break;
                case "trusted":
                    setTitle(R.string.setting_title_profiles_trustedList);
                    break;
            }
        }

        listProtected = new List_protected(this);
        listStandard = new List_standard(this);
        listTrusted = new List_trusted(this);

        RecordAction action = new RecordAction(this);
        action.open(false);

        switch (listToLoad) {
            case "protected":
                list = action.listDomains(RecordUnit.TABLE_PROTECTED);
                break;
            case "standard":
                list = action.listDomains(RecordUnit.TABLE_STANDARD);
                break;
            case "trusted":
                list = action.listDomains(RecordUnit.TABLE_TRUSTED);
                break;
        }

        action.close();

        ListView listView = findViewById(R.id.whitelist);
        listView.setEmptyView(findViewById(R.id.whitelist_empty));

        //noinspection NullableProblems
        adapter = new ProfilesListAdapter(this, list){
            @Override
            public View getView (final int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                Button whitelist_item_cancel = v.findViewById(R.id.whitelist_item_cancel);
                whitelist_item_cancel.setOnClickListener(v1 -> {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ProfilesList.this);
                    builder.setIcon(R.drawable.icon_alert);
                    builder.setTitle(R.string.menu_delete);
                    builder.setMessage(R.string.hint_database);
                    builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {

                        switch (listToLoad) {
                            case "protected":
                                listProtected.removeDomain(list.get(position));
                                break;
                            case "standard":
                                listStandard.removeDomain(list.get(position));
                                break;
                            case "trusted":
                                listTrusted.removeDomain(list.get(position));
                                break;
                        }
                        list.remove(position);
                        notifyDataSetChanged();
                        NinjaToast.show(ProfilesList.this, R.string.toast_delete_successful);
                    });
                    builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    HelperUnit.setupDialog(ProfilesList.this, dialog);
                });
                return v;
            }
        };
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        Button button = findViewById(R.id.whitelist_add);
        button.setOnClickListener(v -> {
            EditText editText = findViewById(R.id.whitelist_edit);
            String domain = editText.getText().toString().trim();
            if (domain.isEmpty()) {
                NinjaToast.show(ProfilesList.this, R.string.toast_input_empty);
            } else if (!BrowserUnit.isURL(domain)) {
                NinjaToast.show(ProfilesList.this, R.string.toast_invalid_domain);
            } else {
                RecordAction action1 = new RecordAction(ProfilesList.this);
                action1.open(true);
                if (action1.checkDomain(domain, RecordUnit.TABLE_PROTECTED)) {
                    NinjaToast.show(ProfilesList.this, R.string.toast_domain_already_exists);
                } else {
                    switch (listToLoad) {
                        case "protected":
                            listProtected.addDomain(domain.trim());
                            break;
                        case "standard":
                            listStandard.addDomain(domain.trim());
                            break;
                        case "trusted":
                            listTrusted.addDomain(domain.trim());
                            break;
                    }
                    list.add(0, domain.trim());
                    adapter.notifyDataSetChanged();
                    NinjaToast.show(ProfilesList.this, R.string.toast_add_whitelist_successful);
                }
                action1.close();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {

        if (menuItem.getItemId() == android.R.id.home) {
            finish();
        } else if (menuItem.getItemId() == R.id.menu_clear) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setIcon(R.drawable.icon_alert);
            builder.setTitle(R.string.menu_delete);
            builder.setMessage(R.string.hint_database);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                switch (listToLoad) {
                    case "protected":
                        listProtected.clearDomains();
                        break;
                    case "standard":
                        listStandard.clearDomains();
                        break;
                    case "trusted":
                        listTrusted.clearDomains();
                        break;
                }
                list.clear();
                adapter.notifyDataSetChanged();
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            HelperUnit.setupDialog(this, dialog);
        } else if (menuItem.getItemId() == R.id.menu_help) {
            Uri webpage = Uri.parse("https://github.com/scoute-dich/browser/wiki/Profile-list");
            BrowserUnit.intentURL(this, webpage);
        }
        return true;
    }
}