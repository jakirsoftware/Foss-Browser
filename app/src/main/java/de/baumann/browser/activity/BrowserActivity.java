package de.baumann.browser.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DownloadManager;

import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.PopupMenu;
import androidx.preference.PreferenceManager;

import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputLayout;

import androidx.appcompat.app.AppCompatActivity;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.baumann.browser.browser.AdBlock;
import de.baumann.browser.browser.AlbumController;
import de.baumann.browser.browser.BrowserContainer;
import de.baumann.browser.browser.BrowserController;
import de.baumann.browser.browser.Cookie;
import de.baumann.browser.browser.Javascript;
import de.baumann.browser.browser.Remote;
import de.baumann.browser.database.BookmarkList;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.R;
import de.baumann.browser.service.ClearService;
import de.baumann.browser.unit.BrowserUnit;
import de.baumann.browser.unit.HelperUnit;
import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.CompleteAdapter;
import de.baumann.browser.view.GridAdapter;

import de.baumann.browser.view.GridItem;
import de.baumann.browser.view.NinjaToast;
import de.baumann.browser.view.NinjaWebView;
import de.baumann.browser.view.RecordAdapter;
import de.baumann.browser.view.SwipeTouchListener;

import static android.content.ContentValues.TAG;

public class BrowserActivity extends AppCompatActivity implements BrowserController {

    // Menus

    private RecordAdapter adapter;
    private RelativeLayout omniBox;
    private ImageButton omniBox_overview;
    private AutoCompleteTextView omniBox_text;
    private ImageButton tab_openOverView;

    // Views

    private FloatingActionButton fab_overflow;
    private EditText searchBox;
    private BottomSheetDialog bottomSheetDialog_OverView;
    private AlertDialog dialog_tabPreview;
    private NinjaWebView ninjaWebView;
    private View customView;
    private VideoView videoView;

    // Layouts

    private RelativeLayout toolBar;
    private RelativeLayout searchPanel;
    private FrameLayout contentFrame;
    private LinearLayout tab_container;
    private FrameLayout fullscreenHolder;

    // Others

    private int mLastContentHeight = 0;
    private BottomNavigationView bottom_navigation;

    private String overViewTab;
    private BroadcastReceiver downloadReceiver;

    private Activity activity;
    private Context context;
    private SharedPreferences sp;
    private Javascript javaHosts;
    private Cookie cookieHosts;
    private AdBlock adBlock;
    private Remote remote;

    private long newIcon;
    private boolean filter;
    private long filterBy;
    private boolean showOverflow = false;
    private TextView overflow_title;

    private boolean prepareRecord() {
        NinjaWebView webView = (NinjaWebView) currentAlbumController;
        String title = webView.getTitle();
        String url = webView.getUrl();
        return (title == null
                || title.isEmpty()
                || url == null
                || url.isEmpty()
                || url.startsWith(BrowserUnit.URL_SCHEME_ABOUT)
                || url.startsWith(BrowserUnit.URL_SCHEME_MAIL_TO)
                || url.startsWith(BrowserUnit.URL_SCHEME_INTENT));
    }

    private int originalOrientation;
    private boolean searchOnSite;

    private ValueCallback<Uri[]> filePathCallback = null;
    private AlbumController currentAlbumController = null;

    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private ValueCallback<Uri[]> mFilePathCallback;

    // Classes

    private class VideoCompletionListener implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            return false;
        }
        @Override
        public void onCompletion(MediaPlayer mp) {
            onHideCustomView();
        }
    }

    private final ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
            int currentContentHeight = findViewById(Window.ID_ANDROID_CONTENT).getHeight();
            if (mLastContentHeight > currentContentHeight + 100) {
                //Keyboard is open
                mLastContentHeight = currentContentHeight;
            } else if (currentContentHeight > mLastContentHeight + 100) {
                //Keyboard is closed
                mLastContentHeight = currentContentHeight;
                omniBox_text.clearFocus();
            }
        }
    };

    // Overrides

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        context = BrowserActivity.this;
        activity = BrowserActivity.this;

        sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt("restart_changed", 0).apply();
        sp.edit().putBoolean("pdf_create", false).apply();

        switch (Objects.requireNonNull(sp.getString("start_tab", "0"))) {
            case "3":
                overViewTab = getString(R.string.album_title_bookmarks);
                break;
            case "4":
                overViewTab = getString(R.string.album_title_history);
                break;
            default:
                overViewTab = getString(R.string.album_title_home);
                break;
        }
        setContentView(R.layout.activity_main);

        if (Objects.requireNonNull(sp.getString("saved_key_ok", "no")).equals("no")) {
            if (Locale.getDefault().getCountry().equals("CN")) {
                sp.edit().putString(getString(R.string.sp_search_engine), "2").apply();
            }
            sp.edit().putString("saved_key_ok", "yes")
                    .putString("setting_gesture_tb_up", "08")
                    .putString("setting_gesture_tb_down", "01")
                    .putString("setting_gesture_tb_left", "07")
                    .putString("setting_gesture_tb_right", "06")
                    .putString("setting_gesture_nav_up", "04")
                    .putString("setting_gesture_nav_down", "05")
                    .putString("setting_gesture_nav_left", "03")
                    .putString("setting_gesture_nav_right", "02")
                    .putBoolean(getString(R.string.sp_location), false).apply();
        }

        contentFrame = findViewById(R.id.main_content);
        toolBar = findViewById(R.id.toolBar);

        initOmniBox();
        initSearchPanel();
        initOverview();

        new AdBlock(context);
        new Javascript(context);
        new Cookie(context);
        new Remote(context);

        downloadReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                builder.setMessage(R.string.toast_downloadComplete);
                builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
                builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                Dialog dialog = builder.create();
                dialog.show();
                Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadReceiver, filter);
        dispatchIntent(getIntent());

        if (sp.getBoolean("start_tabStart", false)){
            showOverview();
        }

        mLastContentHeight = findViewById(Window.ID_ANDROID_CONTENT).getHeight();
        ninjaWebView.getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }

    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        if(requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] results = null;
        // Check that the response is a good one
        if(resultCode == Activity.RESULT_OK) {
            if(data != null) {
                // If there is not data, then we may have taken a photo
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sp.getInt("restart_changed", 1) == 1) {
            sp.edit().putInt("restart_changed", 0).apply();
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_restart);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> finish());
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
        if (sp.getBoolean("pdf_create", false)) {
            sp.edit().putBoolean("pdf_create", false).apply();
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_downloadComplete);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)));
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
        dispatchIntent(getIntent());
    }

    @Override
    public void onDestroy() {
        if (sp.getBoolean(getString(R.string.sp_clear_quit), false)) {
            Intent toClearService = new Intent(this, ClearService.class);
            startService(toClearService);
        }
        BrowserContainer.clear();
        unregisterReceiver(downloadReceiver);
        ninjaWebView.getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
        finish();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
                showOverflow();
            case KeyEvent.KEYCODE_BACK:
                hideOverview();
                if (fullscreenHolder != null || customView != null || videoView != null) {
                    Log.v(TAG, "FOSS Browser in fullscreen mode");
                } else if (searchPanel.getVisibility() == View.VISIBLE) {
                    searchOnSite = false;
                    searchBox.setText("");
                    showOmniBox();
                } else if (omniBox.getVisibility() == View.GONE && sp.getBoolean("sp_toolbarShow", true)) {
                    showOmniBox();
                } else {
                    if (ninjaWebView.canGoBack()) {
                        ninjaWebView.goBack();
                    } else {
                        removeAlbum(currentAlbumController);
                    }
                }
                return true;
        }
        return false;
    }

    @Override
    public synchronized void showAlbum(AlbumController controller) {
        if (currentAlbumController != null) {
            currentAlbumController.deactivate();
            View av = (View) controller;
            contentFrame.removeAllViews();
            contentFrame.addView(av);
        } else {
            contentFrame.removeAllViews();
            contentFrame.addView((View) controller);
        }
        currentAlbumController = controller;
        currentAlbumController.activate();
        updateOmniBox();
        HelperUnit.initRendering(ninjaWebView, context);
    }

    @Override
    public void updateAutoComplete() {
        RecordAction action = new RecordAction(this);
        action.open(false);
        List<Record> list = action.listEntries(activity);
        action.close();
        CompleteAdapter adapter = new CompleteAdapter(this, R.layout.list_item, list);
        omniBox_text.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        omniBox_text.setThreshold(1);
        omniBox_text.setDropDownVerticalOffset(-16);
        omniBox_text.setDropDownWidth(context.getResources().getDisplayMetrics().widthPixels);
        omniBox_text.setOnItemClickListener((parent, view, position, id) -> {
            String url = ((TextView) view.findViewById(R.id.record_item_time)).getText().toString();
            ninjaWebView.loadUrl(url);
            hideKeyboard();
        });
    }

    private void showOverview() {
        initOverview();
        updateOmniBox();
        bottomSheetDialog_OverView.show();
    }

    public void hideOverview () {
        if (bottomSheetDialog_OverView != null) {
            bottomSheetDialog_OverView.cancel();
        }
    }

    public void hideTabView () {
        if (dialog_tabPreview != null) {
            dialog_tabPreview.hide();
        }
    }

    public void showTabView () {

        if (overViewTab.equals(getString(R.string.album_title_home))) {
            tab_openOverView.setImageResource(R.drawable.icon_web_light);
        } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
            tab_openOverView.setImageResource(R.drawable.icon_bookmark_light);
        } else if (overViewTab.equals(getString(R.string.album_title_history))) {
            tab_openOverView.setImageResource(R.drawable.icon_history_light);
        }
        dialog_tabPreview.show();
    }

    private void printPDF () {
        String title = HelperUnit.fileName(ninjaWebView.getUrl());
        PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter printAdapter = ninjaWebView.createPrintDocumentAdapter(title);
        Objects.requireNonNull(printManager).print(title, printAdapter, new PrintAttributes.Builder().build());
        sp.edit().putBoolean("pdf_create", true).apply();
    }

    private void dispatchIntent(Intent intent) {

        String action = intent.getAction();
        String url = intent.getStringExtra(Intent.EXTRA_TEXT);
        String favoriteURL = sp.getString("favoriteURL", "https://github.com/scoute-dich/browser");

        if ("".equals(action)) {
            Log.i(TAG, "resumed FOSS browser");
        } else if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_WEB_SEARCH)) {
            addAlbum(null, Objects.requireNonNull(intent.getStringExtra(SearchManager.QUERY)), true);
            getIntent().setAction("");
        } else if (filePathCallback != null) {
            filePathCallback = null;
            getIntent().setAction("");
        } else {
            assert favoriteURL != null;
            if (!favoriteURL.isEmpty() && "sc_history".equals(action)) {
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")), true);
                showOverview();
                bottom_navigation.setSelectedItemId(R.id.page_3);
                getIntent().setAction("");
            } else if (!favoriteURL.isEmpty() && "sc_bookmark".equals(action)) {
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")), true);
                showOverview();
                bottom_navigation.setSelectedItemId(R.id.page_2);
                getIntent().setAction("");
            } else if (!favoriteURL.isEmpty() && "sc_startPage".equals(action)) {
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")), true);
                showOverview();
                bottom_navigation.setSelectedItemId(R.id.page_1);
                getIntent().setAction("");
            } else if (url != null && Intent.ACTION_SEND.equals(action)) {
                addAlbum(getString(R.string.app_name), url, true);
                getIntent().setAction("");
            } else if (Intent.ACTION_VIEW.equals(action)) {
                String data = Objects.requireNonNull(getIntent().getData()).toString();
                addAlbum(getString(R.string.app_name), data, true);
                getIntent().setAction("");
            } else if (!favoriteURL.isEmpty() && BrowserContainer.size() < 1) {
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")), true);
                getIntent().setAction("");
            } else if (BrowserContainer.size() < 1) {
                addAlbum(getString(R.string.app_name), "about:blank", true);
                getIntent().setAction("");
                NinjaToast.show(context, getString(R.string.toast_load_error));
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initOmniBox() {

        omniBox = findViewById(R.id.omniBox);
        omniBox_text = findViewById(R.id.omniBox_input);
        omniBox_overview = findViewById(R.id.omnibox_overview);
        ImageButton omniBox_overflow = findViewById(R.id.omnibox_overflow);
        ImageButton omniBox_tab = findViewById(R.id.omniBox_tab);
        omniBox_tab.setOnClickListener(v -> showTabView());

        String nav_position = Objects.requireNonNull(sp.getString("nav_position", "0"));

        switch (nav_position) {
            case "1":
                fab_overflow = findViewById(R.id.fab_imageButtonNav_left);
                break;
            case "2":
                fab_overflow = findViewById(R.id.fab_imageButtonNav_center);
                break;
            case "3":
                fab_overflow = findViewById(R.id.fab_imageButtonNav_null);
                break;
            default:
                fab_overflow = findViewById(R.id.fab_imageButtonNav_right);
                break;
        }

        fab_overflow.setOnLongClickListener(v -> {
            show_dialogFastToggle();
            return false;
        });
        omniBox_overflow.setOnLongClickListener(v -> {
            show_dialogFastToggle();
            return false;
        });

        fab_overflow.setOnClickListener(v -> showOverflow());
        omniBox_overflow.setOnClickListener(v -> showOverflow());

        if (sp.getBoolean("sp_gestures_use", true)) {
            fab_overflow.setOnTouchListener(new SwipeTouchListener(context) {
                public void onSwipeTop() { performGesture("setting_gesture_nav_up"); }
                public void onSwipeBottom() { performGesture("setting_gesture_nav_down"); }
                public void onSwipeRight() { performGesture("setting_gesture_nav_right"); }
                public void onSwipeLeft() { performGesture("setting_gesture_nav_left"); }
            });

            omniBox_overflow.setOnTouchListener(new SwipeTouchListener(context) {
                public void onSwipeTop() { performGesture("setting_gesture_nav_up"); }
                public void onSwipeBottom() { performGesture("setting_gesture_nav_down"); }
                public void onSwipeRight() { performGesture("setting_gesture_nav_right"); }
                public void onSwipeLeft() { performGesture("setting_gesture_nav_left"); }
            });

            omniBox_text.setOnTouchListener(new SwipeTouchListener(context) {
                public void onSwipeTop() { performGesture("setting_gesture_tb_up"); }
                public void onSwipeBottom() { performGesture("setting_gesture_tb_down"); }
                public void onSwipeRight() { performGesture("setting_gesture_tb_right"); }
                public void onSwipeLeft() { performGesture("setting_gesture_tb_left"); }
            });
        }

        omniBox_text.setOnEditorActionListener((v, actionId, event) -> {
            String query = omniBox_text.getText().toString().trim();
            ninjaWebView.loadUrl(query);
            hideKeyboard();
            return false;
        });

        omniBox_text.setOnFocusChangeListener((v, hasFocus) -> {
            if (omniBox_text.hasFocus()) {
                ninjaWebView.stopLoading();
                omniBox_text.setText(ninjaWebView.getUrl());
                omniBox_text.setSelection(0, omniBox_text.getText().toString().length());
            } else {
                omniBox_text.setText(ninjaWebView.getTitle());
                hideKeyboard();
            }
        });

        updateAutoComplete();
        omniBox_overview.setOnClickListener(v -> showOverview());
        omniBox_overview.setOnLongClickListener(v -> {
            if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                bottom_navigation.setSelectedItemId(R.id.page_2);
                showOverview();
                show_dialogFilter();
            }
            return false;
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_tabs, null);

        tab_container = dialogView.findViewById(R.id.tab_container);
        tab_openOverView = dialogView.findViewById(R.id.tab_openOverView);
        tab_openOverView.setOnClickListener(view -> {
            dialog_tabPreview.cancel();
            showOverview();
        });
        
        builder.setView(dialogView);
        dialog_tabPreview = builder.create();
        Objects.requireNonNull(dialog_tabPreview.getWindow()).setGravity(Gravity.BOTTOM);
        dialog_tabPreview.setOnCancelListener(dialog ->
                dialog_tabPreview.hide());
    }

    private void performGesture (String gesture) {
        String gestureAction = Objects.requireNonNull(sp.getString(gesture, "0"));
        AlbumController controller;
        ninjaWebView = (NinjaWebView) currentAlbumController;

        switch (gestureAction) {
            case "01":
                break;
            case "02":
                if (ninjaWebView.canGoForward()) {
                    ninjaWebView.goForward();
                } else {
                    NinjaToast.show(context,R.string.toast_webview_forward);
                }
                break;
            case "03":
                if (ninjaWebView.canGoBack()) {
                    ninjaWebView.goBack();
                } else {
                    removeAlbum(currentAlbumController);
                }
                break;
            case "04":
                ninjaWebView.pageUp(true);
                break;
            case "05":
                ninjaWebView.pageDown(true);
                break;
            case "06":
                controller = nextAlbumController(false);
                showAlbum(controller);
                break;
            case "07":
                controller = nextAlbumController(true);
                showAlbum(controller);
                break;
            case "08":
                showOverview();
                break;
            case "09":
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")), true);
                break;
            case "10":
                removeAlbum(currentAlbumController);
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initOverview() {
        bottomSheetDialog_OverView = new BottomSheetDialog(context);
        View dialogView = View.inflate(context, R.layout.dialog_overview, null);
        ListView listView = dialogView.findViewById(R.id.list_overView);

        // allow scrolling in listView without closing the bottomSheetDialog
        listView.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {// Disallow NestedScrollView to intercept touch events.
                if (listView.canScrollVertically(-1)) {
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
            // Handle ListView touch events.
            v.onTouchEvent(event);
            return true;
        });

        bottomSheetDialog_OverView.setContentView(dialogView);

        BottomNavigationView.OnNavigationItemSelectedListener navListener = menuItem -> {
            if (menuItem.getItemId() == R.id.page_0) {
                hideOverview();
                showTabView();
            } else if (menuItem.getItemId() == R.id.page_1) {
                omniBox_overview.setImageResource(R.drawable.icon_web_light);
                overViewTab = getString(R.string.album_title_home);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list = action.listStartSite(activity);
                action.close();

                adapter = new RecordAdapter(context, list);
                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();

                listView.setOnItemClickListener((parent, view, position, id) -> {
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });

                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    show_contextMenu_list(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position, 0);
                    return true;
                });
            } else if (menuItem.getItemId() == R.id.page_2) {
                omniBox_overview.setImageResource(R.drawable.icon_bookmark_light);
                overViewTab = getString(R.string.album_title_bookmarks);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list;
                list = action.listBookmark(activity, filter, filterBy);
                action.close();

                adapter = new RecordAdapter(context, list){
                    @SuppressWarnings("NullableProblems")
                    @Override
                    public View getView (int position, View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        ImageView record_item_icon = v.findViewById(R.id.record_item_icon);
                        record_item_icon.setVisibility(View.VISIBLE);
                        return v;
                    }
                };

                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                filter = false;
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });

                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    show_contextMenu_list(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position, list.get(position).getTime());
                    return true;
                });
                initBookmarkList();
            } else if (menuItem.getItemId() == R.id.page_3) {
                omniBox_overview.setImageResource(R.drawable.icon_history_light);
                overViewTab = getString(R.string.album_title_history);

                RecordAction action = new RecordAction(context);
                action.open(false);
                final List<Record> list;
                list = action.listHistory();
                action.close();

                //noinspection NullableProblems
                adapter = new RecordAdapter(context, list){
                    @Override
                    public View getView (int position, View convertView, @NonNull ViewGroup parent) {
                        View v = super.getView(position, convertView, parent);
                        TextView record_item_time = v.findViewById(R.id.record_item_time);
                        record_item_time.setVisibility(View.VISIBLE);
                        return v;
                    }
                };

                listView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    ninjaWebView.loadUrl(list.get(position).getURL());
                    hideOverview();
                });

                listView.setOnItemLongClickListener((parent, view, position, id) -> {
                    show_contextMenu_list(list.get(position).getTitle(), list.get(position).getURL(), adapter, list, position,0);
                    return true;
                });
            } else if (menuItem.getItemId() == R.id.page_4) {

                PopupMenu popup = new PopupMenu(this, bottom_navigation.findViewById(R.id.page_2));
                if (overViewTab.equals(getString(R.string.album_title_home))) {
                    popup.inflate(R.menu.menu_list_start);
                } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                    popup.inflate(R.menu.menu_list_bookmark);
                } else {
                    popup.inflate(R.menu.menu_list_history);
                }
                popup.setOnMenuItemClickListener(item -> {

                    if (item.getItemId() == R.id.menu_delete) {
                        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
                        builder.setMessage(R.string.hint_database);
                        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
                            if (overViewTab.equals(getString(R.string.album_title_home))) {
                                BrowserUnit.clearHome(context);
                                bottom_navigation.setSelectedItemId(R.id.page_1);
                            } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                                BrowserUnit.clearBookmark(context);
                                bottom_navigation.setSelectedItemId(R.id.page_2);
                            } else if (overViewTab.equals(getString(R.string.album_title_history))) {
                                BrowserUnit.clearHistory(context);
                                bottom_navigation.setSelectedItemId(R.id.page_3);
                            }
                        });
                        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
                    } else if (item.getItemId() == R.id.menu_sortName) {
                        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            sp.edit().putString("sort_bookmark", "title").apply();
                            bottom_navigation.setSelectedItemId(R.id.page_2);
                        } else if (overViewTab.equals(getString(R.string.album_title_home))) {
                            sp.edit().putString("sort_startSite", "title").apply();
                            bottom_navigation.setSelectedItemId(R.id.page_1);
                        }
                    } else if (item.getItemId() == R.id.menu_sortIcon) {
                        sp.edit().putString("sort_bookmark", "time").apply();
                        bottom_navigation.setSelectedItemId(R.id.page_2);
                    } else if (item.getItemId() == R.id.menu_sortDate) {
                        sp.edit().putString("sort_startSite", "ordinal").apply();
                        bottom_navigation.setSelectedItemId(R.id.page_1);
                    } else if (item.getItemId() == R.id.menu_filter) {
                        show_dialogFilter();
                    }
                    return true;
                });
                popup.show();
            }
            return true;
        };

        bottom_navigation = dialogView.findViewById(R.id.bottom_navigation);
        bottom_navigation.setOnNavigationItemSelectedListener(navListener);

        bottom_navigation.findViewById(R.id.page_2).setOnLongClickListener(v -> {
            show_dialogFilter();
            return true;
        });

        BottomSheetBehavior<View> mBehavior = BottomSheetBehavior.from((View) dialogView.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED){
                    hideOverview();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });

        bottomSheetDialog_OverView.setOnCancelListener(dialog ->
                updateOmniBox());
    }

    private void initSearchPanel() {
        searchPanel = findViewById(R.id.searchBox);
        searchBox = findViewById(R.id.searchBox_input);
        ImageView searchUp = findViewById(R.id.searchBox_up);
        ImageView searchDown = findViewById(R.id.searchBox_down);
        ImageView searchCancel = findViewById(R.id.searchBox_cancel);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                if (currentAlbumController != null) {
                    ((NinjaWebView) currentAlbumController).findAllAsync(s.toString());
                }
            }
        });
        searchUp.setOnClickListener(v -> {
            hideKeyboard();
            ((NinjaWebView) currentAlbumController).findNext(false);
        });
        searchDown.setOnClickListener(v -> {
            hideKeyboard();
            ((NinjaWebView) currentAlbumController).findNext(true);
        });
        searchCancel.setOnClickListener(v -> {
            if (searchBox.getText().length() > 0) {
                searchBox.setText("");
            } else {
                hideKeyboard();
                searchOnSite = false;
                showOmniBox();
            }
        });
    }

    private void initBookmarkList() {
        BookmarkList db = new BookmarkList(context);
        db.open();
        Cursor cursor = db.fetchAllData(activity);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            RecordAction action = new RecordAction(context);
            action.open(true);
            action.addBookmark(new Record(
                    cursor.getString(cursor.getColumnIndexOrThrow("edit_title")),
                    cursor.getString(cursor.getColumnIndexOrThrow("pass_content")),
                    1, 0));
            cursor.moveToNext();
            action.close();
            deleteDatabase("pass_DB_v01.db");
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void show_dialogFastToggle() {


        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_toggle, null);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        //TabControl

        final ImageButton whiteList_js_tab = dialogView.findViewById(R.id.imageButton_js_tab);
        if (ninjaWebView.getSettings().getJavaScriptEnabled()) {
            whiteList_js_tab.setImageResource(R.drawable.icon_java_enabled);
            whiteList_js_tab.setOnClickListener(view -> {
                ninjaWebView.getSettings().setJavaScriptEnabled(false);
                ninjaWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
                ninjaWebView.reload();
                dialog.cancel();
            });
        } else {
            whiteList_js_tab.setImageResource(R.drawable.icon_java);
            whiteList_js_tab.setOnClickListener(view -> {
                ninjaWebView.getSettings().setJavaScriptEnabled(true);
                ninjaWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
                ninjaWebView.reload();
                dialog.cancel();
            });
        }

        ImageButton whiteList_ext_tab = dialogView.findViewById(R.id.imageButton_ext_tab);
        if (ninjaWebView.getSettings().getDomStorageEnabled()) {
            whiteList_ext_tab.setImageResource(R.drawable.icon_remote_enabled);
            whiteList_ext_tab.setOnClickListener(view -> {
                ninjaWebView.getSettings().setDomStorageEnabled(false);
                ninjaWebView.reload();
                dialog.cancel();
            });
        } else {
            whiteList_ext_tab.setImageResource(R.drawable.icon_remote);
            whiteList_ext_tab.setOnClickListener(view -> {
                ninjaWebView.getSettings().setDomStorageEnabled(true);
                ninjaWebView.reload();
                dialog.cancel();
            });
        }

        // CheckBox

        CheckBox sw_java = dialogView.findViewById(R.id.switch_js);
        final ImageButton whiteList_js = dialogView.findViewById(R.id.imageButton_js);
        CheckBox sw_adBlock = dialogView.findViewById(R.id.switch_adBlock);
        final ImageButton whiteList_ab = dialogView.findViewById(R.id.imageButton_ab);
        CheckBox sw_cookie = dialogView.findViewById(R.id.switch_cookie);
        final ImageButton whitelist_cookie = dialogView.findViewById(R.id.imageButton_cookie);
        CheckBox switch_ext = dialogView.findViewById(R.id.switch_ext);
        final ImageButton imageButton_ext = dialogView.findViewById(R.id.imageButton_ext);

        TextView dialog_title = dialogView.findViewById(R.id.dialog_title);
        dialog_title.setText(HelperUnit.domain(ninjaWebView.getUrl()));

        javaHosts = new Javascript(context);
        cookieHosts = new Cookie(context);
        adBlock = new AdBlock(context);
        remote = new Remote(context);
        ninjaWebView = (NinjaWebView) currentAlbumController;

        final String url = ninjaWebView.getUrl();

        sw_java.setChecked(sp.getBoolean(getString(R.string.sp_javascript), true));
        sw_adBlock.setChecked(sp.getBoolean(getString(R.string.sp_ad_block), true));
        sw_cookie.setChecked(sp.getBoolean(getString(R.string.sp_cookies), true));
        switch_ext.setChecked(sp.getBoolean("sp_remote", true));

        if (javaHosts.isWhite(url)) {
            whiteList_js.setImageResource(R.drawable.check_green);
        } else {
            whiteList_js.setImageResource(R.drawable.ic_action_close_red);
        }

        if (cookieHosts.isWhite(url)) {
            whitelist_cookie.setImageResource(R.drawable.check_green);
        } else {
            whitelist_cookie.setImageResource(R.drawable.ic_action_close_red);
        }

        if (adBlock.isWhite(url)) {
            whiteList_ab.setImageResource(R.drawable.check_green);
        } else {
            whiteList_ab.setImageResource(R.drawable.ic_action_close_red);
        }

        if (remote.isWhite(url)) {
            imageButton_ext.setImageResource(R.drawable.check_green);
        } else {
            whiteList_ab.setImageResource(R.drawable.ic_action_close_red);
        }

        imageButton_ext.setOnClickListener(view -> {
            if (remote.isWhite(ninjaWebView.getUrl())) {
                imageButton_ext.setImageResource(R.drawable.ic_action_close_red);
                remote.removeDomain(HelperUnit.domain(url));
            } else {
                imageButton_ext.setImageResource(R.drawable.check_green);
                remote.addDomain(HelperUnit.domain(url));
            }
        });

        whiteList_js.setOnClickListener(view -> {
            if (javaHosts.isWhite(ninjaWebView.getUrl())) {
                whiteList_js.setImageResource(R.drawable.ic_action_close_red);
                javaHosts.removeDomain(HelperUnit.domain(url));
            } else {
                whiteList_js.setImageResource(R.drawable.check_green);
                javaHosts.addDomain(HelperUnit.domain(url));
            }
        });

        whitelist_cookie.setOnClickListener(view -> {
            if (cookieHosts.isWhite(ninjaWebView.getUrl())) {
                whitelist_cookie.setImageResource(R.drawable.ic_action_close_red);
                cookieHosts.removeDomain(HelperUnit.domain(url));
            } else {
                whitelist_cookie.setImageResource(R.drawable.check_green);
                cookieHosts.addDomain(HelperUnit.domain(url));
            }
        });


        whiteList_ab.setOnClickListener(view -> {
            if (adBlock.isWhite(ninjaWebView.getUrl())) {
                whiteList_ab.setImageResource(R.drawable.ic_action_close_red);
                adBlock.removeDomain(HelperUnit.domain(url));
            } else {
                whiteList_ab.setImageResource(R.drawable.check_green);
                adBlock.addDomain(HelperUnit.domain(url));
            }
        });

        sw_java.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                sp.edit().putBoolean(getString(R.string.sp_javascript), true).apply();
            }else{
                sp.edit().putBoolean(getString(R.string.sp_javascript), false).apply();
            }

        });

        sw_adBlock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                sp.edit().putBoolean(getString(R.string.sp_ad_block), true).apply();
            }else{
                sp.edit().putBoolean(getString(R.string.sp_ad_block), false).apply();
            }
        });

        sw_cookie.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                sp.edit().putBoolean(getString(R.string.sp_cookies), true).apply();
            }else{
                sp.edit().putBoolean(getString(R.string.sp_cookies), false).apply();
            }
        });

        switch_ext.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked){
                sp.edit().putBoolean("sp_remote", true).apply();
            }else{
                sp.edit().putBoolean("sp_remote", false).apply();
            }
        });

        Chip chip_history = dialogView.findViewById(R.id.chip_history);
        chip_history.setChecked(sp.getBoolean("saveHistory", true));
        chip_history.setOnClickListener(v -> {
            if (sp.getBoolean("saveHistory", true)) {
                chip_history.setChecked(false);
                sp.edit().putBoolean("saveHistory", false).apply();
        } else {
                chip_history.setChecked(true);
                sp.edit().putBoolean("saveHistory", true).apply();
            }
        });

        Chip chip_location = dialogView.findViewById(R.id.chip_location);
        chip_location.setChecked(sp.getBoolean(getString(R.string.sp_location), false));
        chip_location.setOnClickListener(v -> {
            if (sp.getBoolean("saveHistory", true)) {
                chip_location.setChecked(false);
                sp.edit().putBoolean("saveHistory", false).apply();
            } else {
                chip_location.setChecked(true);
                sp.edit().putBoolean("saveHistory", true).apply();
            }
        });

        Chip chip_image = dialogView.findViewById(R.id.chip_image);
        chip_image.setChecked(sp.getBoolean(getString(R.string.sp_images), true));
        chip_image.setOnClickListener(v -> {
            if (sp.getBoolean(getString(R.string.sp_images), true)) {
                chip_image.setChecked(false);
                sp.edit().putBoolean(getString(R.string.sp_images), false).apply();
            } else {
                chip_image.setChecked(true);
                sp.edit().putBoolean(getString(R.string.sp_images), true).apply();
            }
        });

        Chip chip_night = dialogView.findViewById(R.id.chip_night);
        chip_night.setChecked(sp.getBoolean("sp_invert", false));
        chip_night.setOnClickListener(v -> {if (sp.getBoolean("sp_invert", false)) {
            chip_image.setChecked(false);
            sp.edit().putBoolean("sp_invert", false).apply();
        } else {
            chip_image.setChecked(true);
            sp.edit().putBoolean("sp_invert", true).apply();
        }
            HelperUnit.initRendering(ninjaWebView, context);
        });

        ImageButton ib_reload = dialogView.findViewById(R.id.ib_reload);
        ib_reload.setOnClickListener(view -> {
            if (ninjaWebView != null) {
                dialog.cancel();
                ninjaWebView.initPreferences(ninjaWebView.getUrl());
                ninjaWebView.reload();
            }
        });
    }

    private synchronized void addAlbum(String title, final String url, final boolean foreground) {
        ninjaWebView = new NinjaWebView(context);
        ninjaWebView.setOnScrollChangeListener((scrollY, oldScrollY) -> {
            if (!searchOnSite)  {
                omniBox_text.clearFocus();
                ninjaWebView.requestFocus();
            }

            if (!searchOnSite && sp.getBoolean("hideToolbar", true)) {
                int height = (int) Math.floor(ninjaWebView.getContentHeight() * ninjaWebView.getResources().getDisplayMetrics().density);
                int webViewHeight = ninjaWebView.getHeight();
                int cutoff = height - webViewHeight - 112 * Math.round(getResources().getDisplayMetrics().density);
                if (scrollY > oldScrollY && cutoff >= scrollY) {
                    toolBar.setVisibility(View.GONE);
                    fab_overflow.setVisibility(View.VISIBLE);
                } else if (scrollY < oldScrollY){
                    showOmniBox();
                }
            }
        });
        ninjaWebView.setBrowserController(this);
        ninjaWebView.setAlbumTitle(title);
        if (!url.isEmpty()) {
            ninjaWebView.initPreferences(url);
            ninjaWebView.loadUrl(url);
        }

        final View albumView = ninjaWebView.getAlbumView();
        if (currentAlbumController != null) {
            int index = BrowserContainer.indexOf(currentAlbumController) + 1;
            BrowserContainer.add(ninjaWebView, index);
        } else {
            BrowserContainer.add(ninjaWebView);
        }
        tab_container.addView(albumView, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        if (!foreground) {
            ninjaWebView.deactivate();
        } else {
            showAlbum(ninjaWebView);
        }
    }

    private void closeTabConfirmation(final Runnable okAction) {
        if(!sp.getBoolean("sp_close_tab_confirm", false)) {
            okAction.run();
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_quit);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> okAction.run());
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
    }

    @Override
    public synchronized void removeAlbum (final AlbumController controller) {
        if (BrowserContainer.size() <= 1) {
            if(!sp.getBoolean("sp_reopenLastTab", false)) {
                doubleTapsQuit();
            }else{
                ninjaWebView.loadUrl(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"));
                hideOverview();
            }
        } else {
            closeTabConfirmation(() -> {
                tab_container.removeView(controller.getAlbumView());
                int index = BrowserContainer.indexOf(controller);
                BrowserContainer.remove(controller);
                if (index >= BrowserContainer.size()) {
                    index = BrowserContainer.size() - 1;
                }
                showAlbum(BrowserContainer.get(index));
            });
        }
    }

    private void updateOmniBox() {

        if (overViewTab.equals(getString(R.string.album_title_home))) {
            bottom_navigation.setSelectedItemId(R.id.page_1);
        } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
            bottom_navigation.setSelectedItemId(R.id.page_2);
        } else if (overViewTab.equals(getString(R.string.album_title_history))) {
            bottom_navigation.setSelectedItemId(R.id.page_3);
        }

        if (ninjaWebView == currentAlbumController) {
            if (Objects.requireNonNull(ninjaWebView.getTitle()).isEmpty()) {
                omniBox_text.setText(ninjaWebView.getUrl());
            } else {
                omniBox_text.setText(ninjaWebView.getTitle());
            }
            this.cookieHosts = new Cookie(this.context);
            CookieManager manager = CookieManager.getInstance();
            if (cookieHosts.isWhite(ninjaWebView.getUrl()) || sp.getBoolean(context.getString(R.string.sp_cookies), true)) {
                manager.setAcceptCookie(true);
                manager.getCookie(ninjaWebView.getUrl());
            } else {
                manager.setAcceptCookie(false);
            }
        } else {
            ninjaWebView = (NinjaWebView) currentAlbumController;
            updateProgress(ninjaWebView.getProgress());
        }

        if (showOverflow) {
            if (Objects.requireNonNull(ninjaWebView.getTitle()).isEmpty()) {
                overflow_title.setText(ninjaWebView.getUrl());
            } else {
                overflow_title.setText(ninjaWebView.getTitle());
            }
        }
    }

    @Override
    public synchronized void updateProgress(int progress) {
        updateOmniBox();
        LinearProgressIndicator progressBar = findViewById(R.id.main_progress_bar);
        progressBar.setProgressCompat(progress, true);
        if (progress < BrowserUnit.PROGRESS_MAX) {
            progressBar.setVisibility(View.VISIBLE);
            omniBox_overview.setImageResource(R.drawable.icon_close_light);
            omniBox_overview.setOnClickListener(v -> ninjaWebView.stopLoading());
        } else {
            progressBar.setVisibility(View.GONE);
            omniBox_overview.setOnClickListener(v -> showOverview());
        }
    }

    @Override
    public void showFileChooser(ValueCallback<Uri[]> filePathCallback) {
        if(mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(null);
        }
        mFilePathCallback = filePathCallback;
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("*/*");
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
        startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
    }

    @Override
    public void onShowCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        if (view == null) {
            return;
        }
        if (customView != null && callback != null) {
            callback.onCustomViewHidden();
            return;
        }

        customView = view;
        originalOrientation = getRequestedOrientation();

        fullscreenHolder = new FrameLayout(context);
        fullscreenHolder.addView(
                customView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.addView(
                fullscreenHolder,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                ));

        customView.setKeepScreenOn(true);
        ((View) currentAlbumController).setVisibility(View.GONE);
        setCustomFullscreen(true);

        if (view instanceof FrameLayout) {
            if (((FrameLayout) view).getFocusedChild() instanceof VideoView) {
                videoView = (VideoView) ((FrameLayout) view).getFocusedChild();
                videoView.setOnErrorListener(new VideoCompletionListener());
                videoView.setOnCompletionListener(new VideoCompletionListener());
            }
        }
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    public void onHideCustomView() {
        FrameLayout decorView = (FrameLayout) getWindow().getDecorView();
        decorView.removeView(fullscreenHolder);

        customView.setKeepScreenOn(false);
        ((View) currentAlbumController).setVisibility(View.VISIBLE);
        setCustomFullscreen(false);

        fullscreenHolder = null;
        customView = null;
        if (videoView != null) {
            videoView.setOnErrorListener(null);
            videoView.setOnCompletionListener(null);
            videoView = null;
        }
        setRequestedOrientation(originalOrientation);
    }

    private void show_contextMenu_link(final String url) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);

        TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
        menuTitle.setText(url);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        GridItem item_01 = new GridItem(0, getString(R.string.main_menu_new_tabOpen),  0);
        GridItem item_02 = new GridItem(0, getString(R.string.main_menu_new_tab),  0);
        GridItem item_03 = new GridItem(0, getString(R.string.menu_share_link),  0);
        GridItem item_04 = new GridItem(0, getString(R.string.menu_open_with),  0);
        GridItem item_05 = new GridItem(0, getString(R.string.menu_save_as),  0);
        GridItem item_06 = new GridItem(0, getString(R.string.menu_save_home),  0);

        final List<GridItem> gridList = new LinkedList<>();

        gridList.add(gridList.size(), item_01);
        gridList.add(gridList.size(), item_02);
        gridList.add(gridList.size(), item_03);
        gridList.add(gridList.size(), item_04);
        gridList.add(gridList.size(), item_05);
        gridList.add(gridList.size(), item_06);

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {

            switch (position) {
                case 0:
                    addAlbum(getString(R.string.app_name), url, true);
                    dialog.cancel();
                    break;
                case 1:
                    addAlbum(getString(R.string.app_name), url, false);
                    NinjaToast.show(context, getString(R.string.toast_new_tab_successful));
                    updateOmniBox();
                    dialog.cancel();
                    break;
                case 2:
                    shareLink("", url);
                    dialog.cancel();
                    break;
                case 3:
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                    startActivity(chooser);
                    dialog.cancel();
                    break;
                case 4:
                    HelperUnit.save_as(activity, url);
                    dialog.cancel();
                    break;
                case 5:
                    save_atHome(url.replace("http://www.", "").replace("https://www.", ""), url);
                    dialog.cancel();
                    break;
            }
        });
    }

    private void shareLink (String title, String url) {
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, title);
        sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link))));
    }

    @Override
    public void onLongPress(final String url) {
        WebView.HitTestResult result = ninjaWebView.getHitTestResult();
        if (url != null) {
            show_contextMenu_link(url);
        } else if (result.getExtra() != null) {
            show_contextMenu_link(result.getExtra());
        }
    }

    private void doubleTapsQuit() {
        if (!sp.getBoolean("sp_close_browser_confirm", true)) {
            finish();
        } else {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setMessage(R.string.toast_quit);
            builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> finish());
            builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
            AlertDialog dialog = builder.create();
            dialog.show();
            Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
        }
    }

    private void hideKeyboard () {
        View view = activity.getCurrentFocus();
        if (view != null) {
            view.clearFocus();
            ninjaWebView.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showOmniBox() {
        if (!searchOnSite)  {
            fab_overflow.setVisibility(View.GONE);
            searchPanel.setVisibility(View.GONE);
            omniBox.setVisibility(View.VISIBLE);
            toolBar.setVisibility(View.VISIBLE);
        }
    }

    private void showOverflow() {

        showOverflow = true;
        final String url = ninjaWebView.getUrl();
        final String title = ninjaWebView.getTitle();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu_overflow, null);

        builder.setView(dialogView);
        AlertDialog dialog_overflow = builder.create();
        dialog_overflow.show();
        Objects.requireNonNull(dialog_overflow.getWindow()).setGravity(Gravity.BOTTOM);
        dialog_overflow.setOnCancelListener(dialog -> showOverflow = false);

        overflow_title = dialogView.findViewById(R.id.overflow_title);
        updateOmniBox();

        ImageButton overflow_reload = dialogView.findViewById(R.id.overflow_reload);
        if (Objects.requireNonNull(ninjaWebView.getUrl()).startsWith("https://")) {
            overflow_reload.setImageResource(R.drawable.icon_refresh);
        } else {
            overflow_reload.setImageResource(R.drawable.icon_alert);
        }
        overflow_reload.setOnClickListener(v -> {
            dialog_overflow.cancel();
            final String url1 = ninjaWebView.getUrl();
            if (url1 != null && ninjaWebView.isLoadFinish()) {
                if (!url1.startsWith("https://")) {
                    MaterialAlertDialogBuilder builderR = new MaterialAlertDialogBuilder(context);
                    builderR.setMessage(R.string.toast_unsecured);
                    builderR.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> ninjaWebView.loadUrl(url1.replace("http://", "https://")));
                    builderR.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
                    AlertDialog dialog = builderR.create();
                    dialog.show();
                    Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);
                } else {
                    ninjaWebView.initPreferences(ninjaWebView.getUrl());
                    ninjaWebView.reload();
                }
            } else if (url1 == null ){
                String text = getString(R.string.toast_load_error);
                NinjaToast.show(context, text);
            } else {
                ninjaWebView.stopLoading();
            }
        });

        final GridView menu_grid_tab = dialogView.findViewById(R.id.overflow_tab);
        final GridView menu_grid_share = dialogView.findViewById(R.id.overflow_share);
        final GridView menu_grid_save = dialogView.findViewById(R.id.overflow_save);
        final GridView menu_grid_other = dialogView.findViewById(R.id.overflow_other);

        int orientation = this.getResources().getConfiguration().orientation;
        int numberColumns;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            numberColumns = 1;
        } else {
            numberColumns = 2;
        }

        menu_grid_tab.setVisibility(View.VISIBLE);
        menu_grid_share.setVisibility(View.GONE);
        menu_grid_save.setVisibility(View.GONE);
        menu_grid_other.setVisibility(View.GONE);

        menu_grid_tab.setNumColumns(numberColumns);
        menu_grid_share.setNumColumns(numberColumns);
        menu_grid_save.setNumColumns(numberColumns);
        menu_grid_other.setNumColumns(numberColumns);

        // Tab

        GridItem item_01 = new GridItem(0, getString(R.string.main_menu_tabPreview), 0);
        GridItem item_02 = new GridItem(0, getString(R.string.main_menu_new_tabOpen),  0);
        GridItem item_03 = new GridItem(0, getString(R.string.menu_openFav),  0);
        GridItem item_04 = new GridItem(0, getString(R.string.menu_closeTab),  0);
        GridItem item_05 = new GridItem(0, getString(R.string.menu_quit),  0);

        final List<GridItem> gridList_tab = new LinkedList<>();

        gridList_tab.add(gridList_tab.size(), item_03);
        gridList_tab.add(gridList_tab.size(), item_01);
        gridList_tab.add(gridList_tab.size(), item_02);
        gridList_tab.add(gridList_tab.size(), item_04);
        gridList_tab.add(gridList_tab.size(), item_05);

        GridAdapter gridAdapter_tab = new GridAdapter(context, gridList_tab);
        menu_grid_tab.setAdapter(gridAdapter_tab);
        gridAdapter_tab.notifyDataSetChanged();

        menu_grid_tab.setOnItemClickListener((parent, view14, position, id) -> {
            dialog_overflow.cancel();
            if (position == 1) {
                showOverview();
            } else if (position == 2) {
                addAlbum(getString(R.string.app_name), Objects.requireNonNull(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser")), true);
            } else if (position == 0) {
                ninjaWebView.loadUrl(sp.getString("favoriteURL", "https://github.com/scoute-dich/browser"));
            } else if (position == 3) {
                removeAlbum(currentAlbumController);
            } else if (position == 4) {
                doubleTapsQuit();
            }
        });

        // Save
        GridItem item_21 = new GridItem(0, getString(R.string.menu_fav),  0);
        GridItem item_22 = new GridItem(0, getString(R.string.menu_save_home),  0);
        GridItem item_23 = new GridItem(0, getString(R.string.menu_save_bookmark),  0);
        GridItem item_24 = new GridItem(0, getString(R.string.menu_save_pdf),  0);
        GridItem item_25 = new GridItem(0, getString(R.string.menu_sc),  0);
        GridItem item_26 = new GridItem(0, getString(R.string.menu_save_as),  0);

        final List<GridItem> gridList_save = new LinkedList<>();
        gridList_save.add(gridList_save.size(), item_21);
        gridList_save.add(gridList_save.size(), item_22);
        gridList_save.add(gridList_save.size(), item_23);
        gridList_save.add(gridList_save.size(), item_24);
        gridList_save.add(gridList_save.size(), item_25);
        gridList_save.add(gridList_save.size(), item_26);

        GridAdapter gridAdapter_save = new GridAdapter(context, gridList_save);
        menu_grid_save.setAdapter(gridAdapter_save);
        gridAdapter_save.notifyDataSetChanged();

        menu_grid_save.setOnItemClickListener((parent, view13, position, id) -> {
            RecordAction action = new RecordAction(context);
            dialog_overflow.cancel();
            if (position == 0) {
                HelperUnit.setFavorite(context, url);
            } else if (position == 1) {
                save_atHome(title, url);
            } else if (position == 2) {
                action.open(true);
                if (action.checkUrl(url, RecordUnit.TABLE_BOOKMARK)) {
                    NinjaToast.show(context, getString(R.string.toast_already_exist_in_home));
                } else {
                    action.addBookmark(new Record(ninjaWebView.getTitle(), url, System.currentTimeMillis(), 0));
                    NinjaToast.show(context, getString(R.string.toast_add_to_home_successful));
                    bottom_navigation.setSelectedItemId(R.id.page_2);
                }
                action.close();
            } else if (position == 3) {
                printPDF();
            } else if (position == 4) {
                HelperUnit.createShortcut(context, ninjaWebView.getTitle(), ninjaWebView.getUrl());
            } else if (position == 5) {
                HelperUnit.save_as(activity, url);
            }
        });

        // Share
        GridItem item_11 = new GridItem(0, getString(R.string.menu_share_link),  0);
        GridItem item_12 = new GridItem(0, getString(R.string.menu_shareClipboard),  0);
        GridItem item_13 = new GridItem(0, getString(R.string.menu_open_with),  0);

        final List<GridItem> gridList_share = new LinkedList<>();
        gridList_share.add(gridList_share.size(), item_11);
        gridList_share.add(gridList_share.size(), item_12);
        gridList_share.add(gridList_share.size(), item_13);

        GridAdapter gridAdapter_share = new GridAdapter(context, gridList_share);
        menu_grid_share.setAdapter(gridAdapter_share);
        gridAdapter_share.notifyDataSetChanged();

        menu_grid_share.setOnItemClickListener((parent, view12, position, id) -> {
            dialog_overflow.cancel();
            if (position == 0) {
                if (prepareRecord()) {
                    NinjaToast.show(context, getString(R.string.toast_share_failed));
                } else {
                    shareLink(title, url);
                }
            } else if (position == 1) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("text", url);
                Objects.requireNonNull(clipboard).setPrimaryClip(clip);
                NinjaToast.show(context, R.string.toast_copy_successful);
            } else if (position == 2) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                Intent chooser = Intent.createChooser(intent, getString(R.string.menu_open_with));
                startActivity(chooser);
            }
        });

        // Other
        GridItem item_31 = new GridItem(0, getString(R.string.menu_other_searchSite),  0);
        GridItem item_32 = new GridItem(0, getString(R.string.menu_download),  0);
        GridItem item_33 = new GridItem(0, getString(R.string.setting_label),  0);

        final List<GridItem> gridList_other = new LinkedList<>();
        gridList_other.add(gridList_other.size(), item_31);
        gridList_other.add(gridList_other.size(), item_32);
        gridList_other.add(gridList_other.size(), item_33);

        GridAdapter gridAdapter_other = new GridAdapter(context, gridList_other);
        menu_grid_other.setAdapter(gridAdapter_other);
        gridAdapter_other.notifyDataSetChanged();

        menu_grid_other.setOnItemClickListener((parent, view1, position, id) -> {
            dialog_overflow.cancel();
            if (position == 0) {
                searchOnSite = true;
                fab_overflow.setVisibility(View.GONE);
                omniBox.setVisibility(View.GONE);
                searchPanel.setVisibility(View.VISIBLE);
                toolBar.setVisibility(View.VISIBLE);
            } else if (position == 1) {
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
            } else if (position == 2) {
                Intent settings = new Intent(BrowserActivity.this, Settings_Activity.class);
                startActivity(settings);
            }
        });

        BottomNavigationView.OnNavigationItemSelectedListener navListener = menuItem -> {
            if (menuItem.getItemId() == R.id.page_1) {
                menu_grid_tab.setVisibility(View.VISIBLE);
                menu_grid_share.setVisibility(View.GONE);
                menu_grid_save.setVisibility(View.GONE);
                menu_grid_other.setVisibility(View.GONE);
            } else if (menuItem.getItemId() == R.id.page_2) {
                menu_grid_tab.setVisibility(View.GONE);
                menu_grid_share.setVisibility(View.VISIBLE);
                menu_grid_save.setVisibility(View.GONE);
                menu_grid_other.setVisibility(View.GONE);
            } else if (menuItem.getItemId() == R.id.page_3) {
                menu_grid_tab.setVisibility(View.GONE);
                menu_grid_share.setVisibility(View.GONE);
                menu_grid_save.setVisibility(View.VISIBLE);
                menu_grid_other.setVisibility(View.GONE);
            } else if (menuItem.getItemId() == R.id.page_4) {
                menu_grid_tab.setVisibility(View.GONE);
                menu_grid_share.setVisibility(View.GONE);
                menu_grid_save.setVisibility(View.GONE);
                menu_grid_other.setVisibility(View.VISIBLE);
            }
            return true;
        };

        BottomNavigationView bottom_navigation = dialogView.findViewById(R.id.bottom_navigation);
        bottom_navigation.setOnNavigationItemSelectedListener(navListener);
        bottom_navigation.setSelectedItemId(R.id.page_1);
    }

    private void show_contextMenu_list (final String title, final String url,
                                        final RecordAdapter adapterRecord, final List<Record> recordList, final int location,
                                        final long icon) {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);

        TextView menuTitle = dialogView.findViewById(R.id.menuTitle);
        menuTitle.setText(url);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        GridItem item_01 = new GridItem(0, getString(R.string.main_menu_new_tabOpen),  0);
        GridItem item_02 = new GridItem(0, getString(R.string.main_menu_new_tab),  0);
        GridItem item_03 = new GridItem(0, getString(R.string.menu_delete),  0);
        GridItem item_04 = new GridItem(0, getString(R.string.menu_edit),  0);

        final List<GridItem> gridList = new LinkedList<>();

        if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
            gridList.add(gridList.size(), item_01);
            gridList.add(gridList.size(), item_02);
            gridList.add(gridList.size(), item_03);
            gridList.add(gridList.size(), item_04);
        } else {
            gridList.add(gridList.size(), item_01);
            gridList.add(gridList.size(), item_02);
            gridList.add(gridList.size(), item_03);
        }

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);
        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
        menu_grid.setOnItemClickListener((parent, view, position, id) -> {

            MaterialAlertDialogBuilder builderSubMenu;
            AlertDialog dialogSubMenu;

            switch (position) {

                case 0:
                    addAlbum(getString(R.string.app_name), url, true);
                    hideOverview();
                    dialog.cancel();
                    break;
                case 1:
                    addAlbum(getString(R.string.app_name), url, false);
                    NinjaToast.show(context, getString(R.string.toast_new_tab_successful));
                    updateOmniBox();
                    dialog.cancel();
                    break;
                case 2:
                    builderSubMenu = new MaterialAlertDialogBuilder(context);
                    builderSubMenu.setMessage(R.string.hint_database);
                    builderSubMenu.setPositiveButton(R.string.app_ok, (dialog2, whichButton) -> {
                        Record record = recordList.get(location);
                        RecordAction action = new RecordAction(context);
                        action.open(true);
                        if (overViewTab.equals(getString(R.string.album_title_home))) {
                            action.deleteURL(record.getURL(), RecordUnit.TABLE_GRID);
                        } else if (overViewTab.equals(getString(R.string.album_title_bookmarks))) {
                            action.deleteURL(record.getURL(), RecordUnit.TABLE_BOOKMARK);
                        } else if (overViewTab.equals(getString(R.string.album_title_history))) {
                            action.deleteURL(record.getURL(), RecordUnit.TABLE_HISTORY);
                        }
                        action.close();
                        recordList.remove(location);
                        updateAutoComplete();
                        adapterRecord.notifyDataSetChanged();
                        dialog.cancel();
                    });
                    builderSubMenu.setNegativeButton(R.string.app_cancel, (dialog2, whichButton) -> dialog.cancel());
                    dialogSubMenu = builderSubMenu.create();
                    dialogSubMenu.show();
                    Objects.requireNonNull(dialogSubMenu.getWindow()).setGravity(Gravity.BOTTOM);
                    break;
                case 3:
                    builderSubMenu = new MaterialAlertDialogBuilder(context);
                    View dialogViewSubMenu = View.inflate(context, R.layout.dialog_edit_title, null);

                    TextInputLayout edit_title_layout = dialogViewSubMenu.findViewById(R.id.edit_title_layout);
                    TextInputLayout edit_userName_layout = dialogViewSubMenu.findViewById(R.id.edit_userName_layout);
                    TextInputLayout edit_PW_layout = dialogViewSubMenu.findViewById(R.id.edit_PW_layout);
                    ImageView ib_icon = dialogViewSubMenu.findViewById(R.id.edit_icon);
                    ib_icon.setVisibility(View.VISIBLE);
                    edit_title_layout.setVisibility(View.VISIBLE);
                    edit_userName_layout.setVisibility(View.GONE);
                    edit_PW_layout.setVisibility(View.GONE);

                    EditText edit_title = dialogViewSubMenu.findViewById(R.id.edit_title);
                    edit_title.setText(title);

                    ib_icon.setOnClickListener(v -> {

                        MaterialAlertDialogBuilder builderFilter = new MaterialAlertDialogBuilder(context);
                        View dialogViewFilter = View.inflate(context, R.layout.dialog_menu, null);

                        builderFilter.setView(dialogViewFilter);
                        AlertDialog dialogFilter = builderFilter.create();
                        dialogFilter.show();
                        Objects.requireNonNull(dialogFilter.getWindow()).setGravity(Gravity.BOTTOM);

                        GridView menu_grid2 = dialogViewFilter.findViewById(R.id.menu_grid);

                        final List<GridItem> gridList2 = new LinkedList<>();
                        HelperUnit.addFilterItems(activity, gridList);

                        GridAdapter gridAdapter2 = new GridAdapter(context, gridList2);
                        menu_grid2.setAdapter(gridAdapter2);
                        gridAdapter2.notifyDataSetChanged();

                        menu_grid2.setOnItemClickListener((parent2, view2, position2, id2) -> {
                            newIcon = gridList.get(position2).getData();
                            HelperUnit.setFilterIcons(ib_icon, newIcon);
                            dialogFilter.cancel();
                        });
                    });

                    newIcon = icon;
                    HelperUnit.setFilterIcons(ib_icon, newIcon);

                    builderSubMenu.setView(dialogViewSubMenu);
                    builderSubMenu.setTitle(getString(R.string.menu_edit));
                    builderSubMenu.setPositiveButton(R.string.app_ok, (dialog3, whichButton) -> {
                        RecordAction action = new RecordAction(context);
                        action.open(true);
                        action.deleteURL(url, RecordUnit.TABLE_BOOKMARK);
                        action.addBookmark(new Record(edit_title.getText().toString(), url, newIcon, 0));
                        action.close();
                        updateAutoComplete();
                        bottom_navigation.setSelectedItemId(R.id.page_2);
                        dialog.cancel();
                    });
                    builderSubMenu.setNegativeButton(R.string.app_cancel, (dialog3, whichButton) -> dialog.cancel());
                    dialogSubMenu = builderSubMenu.create();
                    dialogSubMenu.show();
                    Objects.requireNonNull(dialogSubMenu.getWindow()).setGravity(Gravity.BOTTOM);
                    break;
            }
        });
    }

    private void save_atHome (final String title, final String url) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        if (action.checkUrl(url, RecordUnit.TABLE_GRID)) {
            NinjaToast.show(context, getString(R.string.toast_already_exist_in_home));
        } else {
            int counter = sp.getInt("counter", 0);
            counter = counter + 1;
            sp.edit().putInt("counter", counter).apply();
            if (action.addGridItem(new Record(title, url, 0, counter))) {
                NinjaToast.show(context, getString(R.string.toast_add_to_home_successful));
            } else {
                NinjaToast.show(context, getString(R.string.toast_add_to_home_failed));
            }
        }
        action.close();
    }

    private void show_dialogFilter() {

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        View dialogView = View.inflate(context, R.layout.dialog_menu, null);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setGravity(Gravity.BOTTOM);

        GridView menu_grid = dialogView.findViewById(R.id.menu_grid);

        final List<GridItem> gridList = new LinkedList<>();
        HelperUnit.addFilterItems(activity, gridList);

        GridAdapter gridAdapter = new GridAdapter(context, gridList);
        menu_grid.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();

        menu_grid.setOnItemClickListener((parent, view, position, id) -> {
            filter = true;
            filterBy = gridList.get(position).getData();
            dialog.cancel();
            bottom_navigation.setSelectedItemId(R.id.page_2);
        });
    }

    private void setCustomFullscreen(boolean fullscreen) {
        WindowInsetsController controller = getWindow().getInsetsController();
        if (fullscreen) {
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    private AlbumController nextAlbumController(boolean next) {
        if (BrowserContainer.size() <= 1) {
            return currentAlbumController;
        }
        List<AlbumController> list = BrowserContainer.list();
        int index = list.indexOf(currentAlbumController);
        if (next) {
            index++;
            if (index >= list.size()) {
                index = 0;
            }
        } else {
            index--;
            if (index < 0) {
                index = list.size() - 1;
            }
        }
        return list.get(index);
    }
}