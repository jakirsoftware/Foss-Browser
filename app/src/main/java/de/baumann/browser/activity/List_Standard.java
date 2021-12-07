package de.baumann.browser.activity;

import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.Objects;

import de.baumann.browser.R;
import de.baumann.browser.browser.List_standard;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.WhitelistAdapter;
import de.baumann.browser.view.NinjaToast;

public class List_Standard extends AppCompatActivity {

    private WhitelistAdapter adapter;
    private List<String> list;
    private List_standard DOM;

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
        setContentView(R.layout.activity_settings_profile_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        DOM = new List_standard(List_Standard.this);

        RecordAction action = new RecordAction(this);
        action.open(false);
        list = action.listDomains(RecordUnit.TABLE_STANDARD);
        action.close();

        ListView listView = findViewById(R.id.whitelist);
        listView.setEmptyView(findViewById(R.id.whitelist_empty));

        //noinspection NullableProblems
        adapter = new WhitelistAdapter(this, list){
            @Override
            public View getView (final int position, View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                ImageButton whitelist_item_cancel = v.findViewById(R.id.whitelist_item_cancel);
                whitelist_item_cancel.setOnClickListener(v1 -> {
                    DOM.removeDomain(list.get(position));
                    list.remove(position);
                    notifyDataSetChanged();
                    NinjaToast.show(List_Standard.this, R.string.toast_delete_successful);
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
                NinjaToast.show(List_Standard.this, R.string.toast_input_empty);
            } else if (!BrowserUnit.isURL(domain)) {
                NinjaToast.show(List_Standard.this, R.string.toast_invalid_domain);
            } else {
                RecordAction action1 = new RecordAction(List_Standard.this);
                action1.open(true);
                if (action1.checkDomain(domain, RecordUnit.TABLE_STANDARD)) {
                    NinjaToast.show(List_Standard.this, R.string.toast_domain_already_exists);
                } else {
                    DOM.addDomain(domain.trim());
                    list.add(0, domain.trim());
                    adapter.notifyDataSetChanged();
                    NinjaToast.show(List_Standard.this, R.string.toast_add_whitelist_successful);
                }
                action1.close();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_whitelist, menu);
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
                List_standard DOM = new List_standard(List_Standard.this);
                DOM.clearDomains();
                list.clear();
                adapter.notifyDataSetChanged();
            });
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
        return true;
    }
}