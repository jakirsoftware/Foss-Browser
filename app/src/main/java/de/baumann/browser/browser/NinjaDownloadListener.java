package de.baumann.browser.browser;

import static android.content.Context.DOWNLOAD_SERVICE;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;

import de.baumann.browser.R;
import de.baumann.browser.unit.BackupUnit;
import de.baumann.browser.unit.HelperUnit;

public class NinjaDownloadListener implements DownloadListener {
    private final Context context;

    public NinjaDownloadListener(Context context) {
        super();
        this.context = context;
    }

    @Override
    public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimeType, long contentLength) {
        String text = context.getString(R.string.dialog_title_download) + " - " + URLUtil.guessFileName(url, contentDisposition, mimeType);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setTitle(R.string.app_warning);
        builder.setMessage(text);
        builder.setIcon(R.drawable.icon_alert);
        builder.setPositiveButton(R.string.app_ok, (dialog, whichButton) -> {
            try {
                Activity activity = (Activity) context;
                String filename = URLUtil.guessFileName(url, contentDisposition, mimeType);
                // Maybe unexpected filename.

                if (url.startsWith("data:")) {
                    DataURIParser dataURIParser = new DataURIParser(url);
                    if (BackupUnit.checkPermissionStorage(context)) {
                        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(dataURIParser.getImagedata()); }
                    else BackupUnit.requestPermission(activity); }
                else {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimeType);
                    //------------------------COOKIE!!------------------------
                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    //------------------------COOKIE!!------------------------
                    request.setDescription(context.getString(R.string.dialog_title_download));
                    request.setTitle(filename);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
                    DownloadManager dm = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
                    assert dm != null;
                    if (BackupUnit.checkPermissionStorage(context)) dm.enqueue(request);
                    else BackupUnit.requestPermission(activity); }}
            catch (Exception e) {
                System.out.println("Error Downloading File: " + e);
                Toast.makeText(context, context.getString(R.string.app_error) + e.toString().substring(e.toString().indexOf(":")), Toast.LENGTH_LONG).show();
                e.printStackTrace();}
        });
        builder.setNeutralButton(R.string.menu_share_link, (dialog, whichButton) -> {
            try {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(Intent.EXTRA_TEXT, url);
                context.startActivity(Intent.createChooser(sharingIntent, (context.getString(R.string.menu_share_link)))); }
            catch (Exception e) {
                System.out.println("Error Downloading File: " + e);
                Toast.makeText(context, context.getString(R.string.app_error) + e.toString().substring(e.toString().indexOf(":")), Toast.LENGTH_LONG).show();
                e.printStackTrace();}
        });
        builder.setNegativeButton(R.string.app_cancel, (dialog, whichButton) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
        HelperUnit.setupDialog(context, dialog);
    }
}
