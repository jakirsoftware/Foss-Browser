package de.baumann.browser.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.preference.PreferenceManager;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import android.util.AttributeSet;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;

import de.baumann.browser.browser.*;
import de.baumann.browser.R;
import de.baumann.browser.database.FaviconHelper;
import de.baumann.browser.database.Record;
import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.BrowserUnit;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class NinjaWebView extends WebView implements AlbumController {

    private OnScrollChangeListener onScrollChangeListener;
    public NinjaWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public NinjaWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onScrollChanged(int l, int t, int old_l, int old_t) {
        super.onScrollChanged(l, t, old_l, old_t);
        if (onScrollChangeListener != null) {
            onScrollChangeListener.onScrollChange(t, old_t);
        }
    }

    public void setOnScrollChangeListener(OnScrollChangeListener onScrollChangeListener) {
        this.onScrollChangeListener = onScrollChangeListener;
    }

    public interface OnScrollChangeListener {
        /**
         * Called when the scroll position of a view changes.
         *
         * @param scrollY    Current vertical scroll origin.
         * @param oldScrollY Previous vertical scroll origin.
         */
        void onScrollChange(int scrollY, int oldScrollY);
    }

    private Context context;
    private boolean desktopMode;
    private boolean nightMode;
    public boolean fingerPrintProtection;
    public boolean history;
    public boolean adBlock;
    public boolean saveData;
    public boolean camera;
    private boolean stopped;
    private AlbumItem album;
    private AlbumController predecessor=null;
    private NinjaWebViewClient webViewClient;
    private NinjaWebChromeClient webChromeClient;
    private NinjaDownloadListener downloadListener;

    private String profile;

    public boolean isBackPressed;
    public void setIsBackPressed(Boolean isBackPressed) {
        this.isBackPressed = isBackPressed;
    }

    private List_trusted listTrusted;
    private List_standard listStandard;
    private List_protected listProtected;
    private Bitmap favicon;
    private SharedPreferences sp;

    private boolean foreground;
    public boolean isForeground() {
        return foreground;
    }
    private BrowserController browserController = null;
    public BrowserController getBrowserController() {
        return browserController;
    }

    public void setBrowserController(BrowserController browserController) {
        this.browserController = browserController;
        this.album.setBrowserController(browserController);
    }

    public NinjaWebView(Context context) {
        super(context);
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        String profile = sp.getString("profile","standard");
        this.context = context;
        this.foreground = false;
        this.desktopMode=false;
        this.nightMode=false;
        this.isBackPressed = false;
        this.fingerPrintProtection=sp.getBoolean(profile + "_fingerPrintProtection",true);
        this.history=sp.getBoolean(profile + "_history",true);
        this.adBlock=sp.getBoolean(profile + "_adBlock",false);
        this.saveData=sp.getBoolean(profile + "_saveData",false);
        this.camera=sp.getBoolean(profile + "_camera",false);

        this.stopped=false;
        this.listTrusted = new List_trusted(this.context);
        this.listStandard = new List_standard(this.context);
        this.listProtected = new List_protected(this.context);
        this.album = new AlbumItem(this.context, this, this.browserController);
        this.webViewClient = new NinjaWebViewClient(this);
        this.webChromeClient = new NinjaWebChromeClient(this);
        this.downloadListener = new NinjaDownloadListener(this.context);
        initWebView();
        initAlbum();
    }

    private synchronized void initWebView() {
        setWebViewClient(webViewClient);
        setWebChromeClient(webChromeClient);
        setDownloadListener(downloadListener);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @TargetApi(Build.VERSION_CODES.O)
    public synchronized void initPreferences(String url) {

        sp = PreferenceManager.getDefaultSharedPreferences(context);
        profile = sp.getString("profile", "profileStandard");
        String profileOriginal = profile;
        WebSettings webSettings = getSettings();

        String userAgent = getUserAgent(desktopMode);
        webSettings.setUserAgentString(userAgent);
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            webSettings.setSafeBrowsingEnabled(true);
        }
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setTextZoom(Integer.parseInt(Objects.requireNonNull(sp.getString("sp_fontSize", "100"))));

        if (sp.getBoolean("sp_autofill", true)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_YES);
            } else {
                webSettings.setSaveFormData(true);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
            } else {
                webSettings.setSaveFormData(false);
            }
        }

        if (listTrusted.isWhite(url)) {
            profile = "profileTrusted";
        } else if (listStandard.isWhite(url)) {
            profile = "profileStandard";
        } else if (listProtected.isWhite(url)) {
            profile = "profileProtected";
        }

        webSettings.setMediaPlaybackRequiresUserGesture(sp.getBoolean(profile + "_saveData",true));
        webSettings.setBlockNetworkImage(!sp.getBoolean(profile + "_images", true));
        webSettings.setGeolocationEnabled(sp.getBoolean(profile + "_location", false));
        webSettings.setJavaScriptEnabled(sp.getBoolean(profile + "_javascript", true));
        webSettings.setJavaScriptCanOpenWindowsAutomatically(sp.getBoolean(profile + "_javascriptPopUp", false));
        webSettings.setDomStorageEnabled(sp.getBoolean(profile + "_dom", false));
        fingerPrintProtection = sp.getBoolean(profile + "_fingerPrintProtection", true);
        history = sp.getBoolean(profile + "_saveHistory", true);
        adBlock = sp.getBoolean(profile + "_adBlock", true);
        saveData = sp.getBoolean(profile + "_saveData", true);
        camera = sp.getBoolean(profile + "_camera", true);
        initCookieManager(url);
        profile = profileOriginal;
    }

    public synchronized void initCookieManager(String url) {
        sp = PreferenceManager.getDefaultSharedPreferences(context);
        profile = sp.getString("profile", "profileStandard");
        String profileOriginal = profile;
        if (listTrusted.isWhite(url)) {
            profile = "profileTrusted";
        } else if (listStandard.isWhite(url)) {
            profile = "profileStandard";
        } else if (listProtected.isWhite(url)) {
            profile = "profileProtected";
        }
        CookieManager manager = CookieManager.getInstance();
        if (sp.getBoolean(profile + "_cookies", false)) {
            manager.setAcceptCookie(true);
            manager.getCookie(url);
        } else {
            manager.setAcceptCookie(false);
        }
        profile = profileOriginal;
    }

    public void setProfileIcon (ImageButton omniBox_tab) {
        String url = this.getUrl();
        assert url != null;
        switch (profile) {
            case "profileTrusted":
                if (url.startsWith("http:")) omniBox_tab.setImageResource(R.drawable.icon_profile_trusted_red);
                else omniBox_tab.setImageResource(R.drawable.icon_profile_trusted_light);
                break;
            case "profileStandard":
                if (url.startsWith("http:")) omniBox_tab.setImageResource(R.drawable.icon_profile_standard_red);
                else omniBox_tab.setImageResource(R.drawable.icon_profile_standard_light);
                break;
            case "profileProtected":
                if (url.startsWith("http:")) omniBox_tab.setImageResource(R.drawable.icon_profile_protected_red);
                else omniBox_tab.setImageResource(R.drawable.icon_profile_protected_light);
                break;
            default:
                if (url.startsWith("http:")) omniBox_tab.setImageResource(R.drawable.icon_profile_changed_red);
                else omniBox_tab.setImageResource(R.drawable.icon_profile_changed_light);
                break;
        }

        if (listTrusted.isWhite(url)) {
            if (url.startsWith("http:")) omniBox_tab.setImageResource(R.drawable.icon_profile_trusted_red);
            else omniBox_tab.setImageResource(R.drawable.icon_profile_trusted_light);
        } else if (listStandard.isWhite(url)) {
            if (url.startsWith("http:")) omniBox_tab.setImageResource(R.drawable.icon_profile_standard_red);
            else omniBox_tab.setImageResource(R.drawable.icon_profile_standard_light);
        } else if (listProtected.isWhite(url)) {
            if (url.startsWith("http:")) omniBox_tab.setImageResource(R.drawable.icon_profile_protected_red);
            else omniBox_tab.setImageResource(R.drawable.icon_profile_protected_light);
        }
    }

    public void setProfileDefaultValues() {
        sp.edit()
                .putBoolean("profileTrusted_saveData", true)
                .putBoolean("profileTrusted_images", true)
                .putBoolean("profileTrusted_adBlock", true)
                .putBoolean("profileTrusted_location", false)
                .putBoolean("profileTrusted_fingerPrintProtection", false)
                .putBoolean("profileTrusted_cookies", true)
                .putBoolean("profileTrusted_javascript", true)
                .putBoolean("profileTrusted_javascriptPopUp", true)
                .putBoolean("profileTrusted_saveHistory", true)
                .putBoolean("profileTrusted_camera", false)
                .putBoolean("profileTrusted_microphone", false)
                .putBoolean("profileTrusted_dom", true)

                .putBoolean("profileStandard_saveData", true)
                .putBoolean("profileStandard_images", true)
                .putBoolean("profileStandard_adBlock", true)
                .putBoolean("profileStandard_location", false)
                .putBoolean("profileStandard_fingerPrintProtection", true)
                .putBoolean("profileStandard_cookies", false)
                .putBoolean("profileStandard_javascript", true)
                .putBoolean("profileStandard_javascriptPopUp", false)
                .putBoolean("profileStandard_saveHistory", true)
                .putBoolean("profileStandard_camera", false)
                .putBoolean("profileStandard_microphone", false)
                .putBoolean("profileStandard_dom", false)

                .putBoolean("profileProtected_saveData", true)
                .putBoolean("profileProtected_images", true)
                .putBoolean("profileProtected_adBlock", true)
                .putBoolean("profileProtected_location", false)
                .putBoolean("profileProtected_fingerPrintProtection", true)
                .putBoolean("profileProtected_cookies", false)
                .putBoolean("profileProtected_javascript", false)
                .putBoolean("profileProtected_javascriptPopUp", false)
                .putBoolean("profileProtected_saveHistory", true)
                .putBoolean("profileProtected_camera", false)
                .putBoolean("profileProtected_microphone", false)
                .putBoolean("profileProtected_dom", false).apply();
    }

    public void setProfileChanged () {
        sp.edit().putBoolean("profileChanged_saveData", sp.getBoolean(profile + "_saveData",true))
                .putBoolean("profileChanged_images", sp.getBoolean(profile + "_images",true))
                .putBoolean("profileChanged_adBlock", sp.getBoolean(profile + "_adBlock",true))
                .putBoolean("profileChanged_location", sp.getBoolean(profile + "_location",false))
                .putBoolean("profileChanged_fingerPrintProtection", sp.getBoolean(profile + "_fingerPrintProtection",true))
                .putBoolean("profileChanged_cookies", sp.getBoolean(profile + "_cookies",false))
                .putBoolean("profileChanged_javascript", sp.getBoolean(profile + "_javascript",true))
                .putBoolean("profileChanged_javascriptPopUp", sp.getBoolean(profile + "_javascriptPopUp",false))
                .putBoolean("profileChanged_saveHistory", sp.getBoolean(profile + "_saveHistory",true))
                .putBoolean("profileChanged_camera", sp.getBoolean(profile + "_camera",false))
                .putBoolean("profileChanged_microphone", sp.getBoolean(profile + "_microphone",false))
                .putBoolean("profileChanged_dom", sp.getBoolean(profile + "_dom",false))
                .putString("profile", "profileChanged").apply();
    }

    public void putProfileBoolean (String string, TextView dialog_titleProfile,
                                   Chip chip_profile_trusted, Chip chip_profile_standard, Chip chip_profile_protected, Chip chip_profile_changed) {
        switch (string) {
            case "_images":
                sp.edit().putBoolean("profileChanged_images", !sp.getBoolean("profileChanged_images", true)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_images),Toast.LENGTH_SHORT).show();
                break;
            case "_javascript":
                sp.edit().putBoolean("profileChanged_javascript", !sp.getBoolean("profileChanged_javascript", true)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_javascript),Toast.LENGTH_SHORT).show();
                break;
            case "_javascriptPopUp":
                sp.edit().putBoolean("profileChanged_javascriptPopUp", !sp.getBoolean("profileChanged_javascriptPopUp", false)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_javascript_popUp),Toast.LENGTH_SHORT).show();
                break;
            case "_cookies":
                sp.edit().putBoolean("profileChanged_cookies", !sp.getBoolean("profileChanged_cookies", false)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_cookie),Toast.LENGTH_SHORT).show();
                break;
            case "_fingerPrintProtection":
                sp.edit().putBoolean("profileChanged_fingerPrintProtection", !sp.getBoolean("profileChanged_fingerPrintProtection", true)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_fingerPrint),Toast.LENGTH_SHORT).show();
                break;
            case "_adBlock":
                sp.edit().putBoolean("profileChanged_adBlock", !sp.getBoolean("profileChanged_adBlock", true)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_adblock),Toast.LENGTH_SHORT).show();
                break;
            case "_saveData":
                sp.edit().putBoolean("profileChanged_saveData", !sp.getBoolean("profileChanged_saveData", true)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_save_data),Toast.LENGTH_SHORT).show();
                break;
            case "_saveHistory":
                sp.edit().putBoolean("profileChanged_saveHistory", !sp.getBoolean("profileChanged_saveHistory", true)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_history),Toast.LENGTH_SHORT).show();
                break;
            case "_location":
                sp.edit().putBoolean("profileChanged_location", !sp.getBoolean("profileChanged_location", false)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_location),Toast.LENGTH_SHORT).show();
                break;
            case "_camera":
                sp.edit().putBoolean("profileChanged_camera", !sp.getBoolean("profileChanged_camera", false)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_camera),Toast.LENGTH_SHORT).show();
                break;
            case "_microphone":
                sp.edit().putBoolean("profileChanged_microphone", !sp.getBoolean("profileChanged_microphone", false)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_microphone),Toast.LENGTH_SHORT).show();
                break;
            case "_dom":
                sp.edit().putBoolean("profileChanged_dom", !sp.getBoolean("profileChanged_dom", false)).apply();
                Toast.makeText(this.context,this.context.getString(R.string.setting_title_dom),Toast.LENGTH_SHORT).show();
                break;
        }
        this.initPreferences("");

        String textTitle;
        switch (Objects.requireNonNull(profile)) {
            case "profileTrusted":
                chip_profile_trusted.setChecked(true);
                chip_profile_standard.setChecked(false);
                chip_profile_protected.setChecked(false);
                chip_profile_changed.setChecked(false);
                textTitle = this.context.getString(R.string.setting_title_profiles_active) + ": " + this.context.getString(R.string.setting_title_profiles_trusted);
                break;
            case "profileStandard":
                chip_profile_trusted.setChecked(false);
                chip_profile_standard.setChecked(true);
                chip_profile_protected.setChecked(false);
                chip_profile_changed.setChecked(false);
                textTitle = this.context.getString(R.string.setting_title_profiles_active) + ": " + this.context.getString(R.string.setting_title_profiles_standard);
                break;
            case "profileProtected":
                chip_profile_trusted.setChecked(false);
                chip_profile_standard.setChecked(false);
                chip_profile_protected.setChecked(true);
                chip_profile_changed.setChecked(false);
                textTitle = this.context.getString(R.string.setting_title_profiles_active) + ": " + this.context.getString(R.string.setting_title_profiles_protected);
                break;
            default:
                chip_profile_trusted.setChecked(false);
                chip_profile_standard.setChecked(false);
                chip_profile_protected.setChecked(false);
                chip_profile_changed.setChecked(true);
                textTitle = this.context.getString(R.string.setting_title_profiles_active) + ": " + this.context.getString(R.string.setting_title_profiles_changed);
                break;
        }
        dialog_titleProfile.setText(textTitle);
    }

    public boolean getBoolean (String string) {
        switch (string) {
            case "_images":
                return sp.getBoolean(profile + "_images", true);
            case "_javascript":
                return sp.getBoolean(profile + "_javascript", true);
            case "_javascriptPopUp":
                return sp.getBoolean(profile + "_javascriptPopUp", false);
            case "_cookies":
                return sp.getBoolean(profile + "_cookies", false);
            case "_fingerPrintProtection":
                return sp.getBoolean(profile + "_fingerPrintProtection", true);
            case "_adBlock":
                return sp.getBoolean(profile + "_adBlock", true);
            case "_saveData":
                return sp.getBoolean(profile + "_saveData", true);
            case "_saveHistory":
                return sp.getBoolean(profile + "_saveHistory", true);
            case "_location":
                return sp.getBoolean(profile + "_location", false);
            case "_camera":
                return sp.getBoolean(profile + "_camera", false);
            case "_microphone":
                return sp.getBoolean(profile + "_microphone", false);
            case "_dom":
                return sp.getBoolean(profile + "_dom", false);
            default:
                return false;
        }
    }

    private synchronized void initAlbum() {
        album.setAlbumTitle(context.getString(R.string.app_name));
        album.setBrowserController(browserController);
    }

    public synchronized HashMap<String, String> getRequestHeaders() {
        HashMap<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("DNT", "1");
        //  Server-side detection for GlobalPrivacyControl
        requestHeaders.put("Sec-GPC","1");
        requestHeaders.put("X-Requested-With","com.duckduckgo.mobile.android");

        profile = sp.getString("profile", "profileStandard");
        if (sp.getBoolean(profile + "_saveData", false)) {
            requestHeaders.put("Save-Data", "on");
        }
        return requestHeaders;
    }

    @Override
    public synchronized void stopLoading(){
        stopped=true;
        super.stopLoading();
    }

    public synchronized void reloadWithoutInit(){  //needed for camera usage without deactivating "save_data"
        stopped=false;
        super.reload();
    }

    @Override
    public synchronized void reload(){
        stopped=false;
        this.initPreferences(this.getUrl());
        super.reload();
    }

    @Override
    public synchronized void loadUrl(String url) {
        initPreferences(BrowserUnit.queryWrapper(context, url.trim()));
        InputMethodManager imm = (InputMethodManager) this.context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        favicon=null;
        stopped=false;
        super.loadUrl(BrowserUnit.queryWrapper(context, url.trim()), getRequestHeaders());
    }

    @Override
    public View getAlbumView() {
        return album.getAlbumView();
    }

    public void setAlbumTitle(String title, String url) {
        album.setAlbumTitle(title);
        CardView cardView = getAlbumView().findViewById(R.id.cardView);
        cardView.setVisibility(VISIBLE);
        FaviconHelper.setFavicon(context, getAlbumView(), url, R.id.faviconView, R.drawable.icon_image_broken_light);
    }

    @Override
    public synchronized void activate() {
        requestFocus();
        foreground = true;
        album.activate();
    }

    @Override
    public synchronized void deactivate() {
        clearFocus();
        foreground = false;
        album.deactivate();
    }

    public synchronized void updateTitle(int progress) {
        if (foreground && !stopped) {
            browserController.updateProgress(progress);
        } else if (foreground) {
            browserController.updateProgress(BrowserUnit.LOADING_STOPPED);
        }
        if (isLoadFinish() && !stopped) {
            browserController.updateAutoComplete();
        }
    }

    public synchronized void updateTitle(String title) {
        album.setAlbumTitle(title);
    }

    public synchronized void updateFavicon (String url) {
        CardView cardView = getAlbumView().findViewById(R.id.cardView);
        cardView.setVisibility(VISIBLE);
        FaviconHelper.setFavicon(context, getAlbumView(), url, R.id.faviconView, R.drawable.icon_image_broken_light);
    }

    @Override
    public synchronized void destroy() {
        stopLoading();
        onPause();
        clearHistory();
        setVisibility(GONE);
        removeAllViews();
        super.destroy();
    }

    public boolean isLoadFinish() {
        return getProgress() >= BrowserUnit.PROGRESS_MAX;
    }

    public boolean isDesktopMode() {
        return desktopMode;
    }
    public boolean isNightMode() {
        return nightMode;
    }

    public boolean isFingerPrintProtection() {
        return fingerPrintProtection;
    }
    public boolean isHistory() {
        return history;
    }
    public boolean isAdBlock() {
        return adBlock;
    }
    public boolean isSaveData() {
        return saveData;
    }
    public boolean isCamera() {
        return camera;
    }

    public String getUserAgent(boolean desktopMode){
        String mobilePrefix = "Mozilla/5.0 (Linux; Android "+ Build.VERSION.RELEASE + ")";
        String desktopPrefix = "Mozilla/5.0 (X11; Linux "+ System.getProperty("os.arch") +")";

        String newUserAgent=WebSettings.getDefaultUserAgent(context);
        String prefix = newUserAgent.substring(0, newUserAgent.indexOf(")") + 1);

        if (desktopMode) {
            try {
                newUserAgent=newUserAgent.replace(prefix,desktopPrefix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                newUserAgent=newUserAgent.replace(prefix,mobilePrefix);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //Override UserAgent if own UserAgent is defined
        if (!sp.contains("userAgentSwitch")){  //if new switch_text_preference has never been used initialize the switch
            if (Objects.requireNonNull(sp.getString("sp_userAgent", "")).equals("")) {
                sp.edit().putBoolean("userAgentSwitch", false).apply();
            }else{
                sp.edit().putBoolean("userAgentSwitch", true).apply();
            }
        }

        String ownUserAgent = sp.getString("sp_userAgent", "");
        assert ownUserAgent != null;
        if (!ownUserAgent.equals("") && (sp.getBoolean("userAgentSwitch",false))) newUserAgent=ownUserAgent;
        return newUserAgent;
    }

    public void toggleDesktopMode(boolean reload) {
        desktopMode=!desktopMode;
        String newUserAgent=getUserAgent(desktopMode);
        getSettings().setUserAgentString(newUserAgent);
        getSettings().setUseWideViewPort(desktopMode);
        getSettings().setSupportZoom(desktopMode);
        getSettings().setLoadWithOverviewMode(desktopMode);
        if (reload) reload();
    }

    public void toggleNightMode() {
        nightMode=!nightMode;
        if (nightMode) {
            if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            } else {
                Paint paint = new Paint();
                ColorMatrix matrix = new ColorMatrix();
                matrix.set(NEGATIVE_COLOR);
                ColorMatrix gcm = new ColorMatrix();
                gcm.setSaturation(0);
                ColorMatrix concat = new ColorMatrix();
                concat.setConcat(matrix, gcm);
                ColorMatrixColorFilter filter = new ColorMatrixColorFilter(concat);
                paint.setColorFilter(filter);
                // maybe sometime LAYER_TYPE_NONE would better?
                this.setLayerType(View.LAYER_TYPE_HARDWARE, paint);
            }
        } else {
            if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(this.getSettings(), WebSettingsCompat.FORCE_DARK_OFF);
            } else {
                this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
        }
    }

    private static final float[] NEGATIVE_COLOR = {
            -1.0f, 0, 0, 0, 255, // Red
            0, -1.0f, 0, 0, 255, // Green
            0, 0, -1.0f, 0, 255, // Blue
            0, 0, 0, 1.0f, 0     // Alpha
    };
    
    public void resetFavicon(){this.favicon=null;}

    public void setFavicon(Bitmap favicon) {
        this.favicon = favicon;

        //Save faviconView for existing bookmarks or start site entries
        FaviconHelper faviconHelper = new FaviconHelper(context);
        RecordAction action = new RecordAction(context);
        action.open(false);
        List<Record> list;
        list = action.listBookmark(context, false, 0);
        action.close();
        for (Record listItem: list){
            if(listItem.getURL().equals(getUrl())){
                if (faviconHelper.getFavicon(listItem.getURL())==null) faviconHelper.addFavicon(getUrl(),getFavicon());
            }
        }

        action.open(false);
        list = action.listStartSite((Activity) context);
        action.close();
        for (Record listItem: list){
            if(listItem.getURL().equals(getUrl())){
                if (faviconHelper.getFavicon(listItem.getURL())==null) faviconHelper.addFavicon(getUrl(),getFavicon());
            }
        }

        action.open(false);
        list = action.listHistory();
        action.close();
        for (Record listitem: list){
            if(listitem.getURL().equals(getUrl())){
                if (faviconHelper.getFavicon(listitem.getURL())==null) faviconHelper.addFavicon(getUrl(),getFavicon());
            }
        }

    }

    @Nullable
    @Override
    public Bitmap getFavicon() {
        return favicon;
    }

    public void setStopped(boolean stopped){this.stopped=stopped;}

    public String getProfile() {
        return profile;
    }

    public AlbumController getPredecessor(){ return predecessor;}

    public void setPredecessor(AlbumController predecessor) {
        this.predecessor = predecessor;
    }
}
