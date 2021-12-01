package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.Objects;

import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.R;

class AlbumItem {

    private final Context context;
    private final AlbumController albumController;
    private ImageView albumClose;

    private View albumView;
    View getAlbumView() {
        return albumView;
    }

    private TextView albumTitle;
    void setAlbumTitle(String title) {
        albumTitle.setText(title);
    }

    private BrowserController browserController;
    void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }

    AlbumItem(Context context, AlbumController albumController, BrowserController browserController) {
        this.context = context;
        this.albumController = albumController;
        this.browserController = browserController;
        initUI();
    }

    @SuppressLint("InflateParams")
    private void initUI() {
        albumView = LayoutInflater.from(context).inflate(R.layout.item_icon_right, null, false);
        albumView.setOnLongClickListener(v -> {
            browserController.removeAlbum(albumController);
            return true;
        });
        albumClose = albumView.findViewById(R.id.whitelist_item_cancel);
        albumClose.setVisibility(View.VISIBLE);
        albumClose.setOnClickListener(v -> browserController.removeAlbum(albumController));
        albumTitle = albumView.findViewById(R.id.whitelist_item_domain);
    }

    public void activate() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        switch (Objects.requireNonNull(sp.getString("sp_theme", "1"))) {
            case "3":
                albumTitle.setTextColor(ContextCompat.getColor(context, R.color.md_theme_dark_primary));
                albumClose.setImageResource(R.drawable.icon_close_enabled_dark);
                break;
            case "4":
            case "5":
                albumTitle.setTextColor(ContextCompat.getColor(context, R.color.material_dynamic_primary50));
                albumClose.setImageResource(R.drawable.icon_close_enabled_dynamic);
                break;
            default:
                albumTitle.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_primary));
                albumClose.setImageResource(R.drawable.icon_close_enabled);
                break;
        }
        albumView.setOnClickListener(v -> browserController.hideTabView());
    }

    void deactivate() {
        albumTitle.setTextColor(ContextCompat.getColor(context, R.color.md_theme_light_surface));
        albumClose.setImageResource(R.drawable.icon_close_light);
        albumView.setOnClickListener(v -> browserController.showAlbum(albumController));
    }
}