package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.google.android.material.chip.Chip;

import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.R;

class AlbumItem {

    private final Context context;
    private final AlbumController albumController;

    private View albumView;
    View getAlbumView() {
        return albumView;
    }

    private Chip albumTitle;
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
        albumView = LayoutInflater.from(context).inflate(R.layout.item_tab, null, false);
        Button albumClose = albumView.findViewById(R.id.whitelist_item_cancel);
        albumClose.setVisibility(View.VISIBLE);
        albumClose.setOnClickListener(v -> browserController.removeAlbum(albumController));
        albumTitle = albumView.findViewById(R.id.whitelist_item_domain);
    }

    public void activate() {
        albumTitle.setChecked(true);
        albumTitle.setOnClickListener(view -> {
            albumTitle.setChecked(true);
            browserController.hideTabView();
        });
    }

    void deactivate() {
        albumTitle.setChecked(false);
        albumTitle.setOnClickListener(view -> {
            browserController.showAlbum(albumController);
            browserController.hideTabView();
        });
    }
}