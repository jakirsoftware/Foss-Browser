package de.baumann.browser.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.baumann.browser.database.RecordAction;
import de.baumann.browser.unit.RecordUnit;

public class Profile_standard {
    private static final String FILE = "remoteHosts.txt";
    private static final Set<String> hostsStandard = new HashSet<>();
    private static final List<String> listStandard = new ArrayList<>();
    @SuppressLint("ConstantLocale")
    private static final Locale locale = Locale.getDefault();

    private static void loadHosts(final Context context) {
        Thread thread = new Thread(() -> {
            AssetManager manager = context.getAssets();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(manager.open(FILE)));
                String line;
                while ((line = reader.readLine()) != null) {
                    hostsStandard.add(line.toLowerCase(locale));
                }
            } catch (IOException i) {
                Log.w("browser", "Error loading hosts");
            }
        });
        thread.start();
    }

    private synchronized static void loadDomains(Context context) {
        RecordAction action = new RecordAction(context);
        action.open(false);
        listStandard.clear();
        listStandard.addAll(action.listDomains(RecordUnit.TABLE_REMOTE));
        action.close();
    }

    private final Context context;

    public Profile_standard(Context context) {
        this.context = context;

        if (hostsStandard.isEmpty()) {
            loadHosts(context);
        }
        loadDomains(context);
    }

    public boolean isWhite(String url) {
        for (String domain : listStandard) {
            if (url != null && url.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void addDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.addDomain(domain, RecordUnit.TABLE_REMOTE);
        action.close();
        listStandard.add(domain);
    }

    public synchronized void removeDomain(String domain) {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.deleteDomain(domain, RecordUnit.TABLE_REMOTE);
        action.close();
        listStandard.remove(domain);
    }

    public synchronized void clearDomains() {
        RecordAction action = new RecordAction(context);
        action.open(true);
        action.clearTable(RecordUnit.TABLE_REMOTE);
        action.close();
        listStandard.clear();
    }
}
