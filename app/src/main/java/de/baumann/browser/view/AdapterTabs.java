package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import de.baumann.browser.R;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserContainer;
import de.baumann.browser.browser.BrowserController;

class AdapterTabs {

    private final Context context;
    private final AlbumController albumController;

    private View albumView;
    private TextView albumTitle;
    private BrowserController browserController;
    private MaterialCardView albumCardView;

    AdapterTabs(Context context, AlbumController albumController, BrowserController browserController) {
        this.context = context;
        this.albumController = albumController;
        this.browserController = browserController;
        initUI();
    }

    View getAlbumView() {
        return albumView;
    }

    void setAlbumTitle(String title) {
        albumTitle.setText(title);
    }

    void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
    }

    @SuppressLint("InflateParams")
    private void initUI() {
        albumView = LayoutInflater.from(context).inflate(R.layout.item_list, null, false);
        albumCardView = albumView.findViewById(R.id.albumCardView);
        albumTitle = albumView.findViewById(R.id.titleView);

        albumTitle.setOnLongClickListener(view -> {
            browserController.showContextMenuTabs (albumTitle.getText().toString(), browserController.getUrl());
            return false;
        });

        Button albumClose = albumView.findViewById(R.id.cancelButton);
        albumClose.setVisibility(View.VISIBLE);
        albumClose.setOnClickListener(view -> {
            browserController.removeAlbum(albumController);
            if (BrowserContainer.size() < 2) { browserController.hideOverview();}
        });
    }

    public void activate() {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorSecondaryContainer, typedValue, true);
        int color = typedValue.data;
        albumCardView.setCardBackgroundColor(color);
        albumTitle.setOnClickListener(view -> {
            albumCardView.setCardBackgroundColor(color);
            browserController.hideOverview();
        });
    }

    void deactivate() {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorSurfaceVariant, typedValue, true);
        int color = typedValue.data;
        albumCardView.setCardBackgroundColor(color);
        albumTitle.setOnClickListener(view -> {
            browserController.showAlbum(albumController);
            browserController.hideOverview();
        });
    }
}