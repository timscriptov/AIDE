package com.aide.premium;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class AnalyticsInstallReferrerReceiver extends BroadcastReceiver {
    private static final String DATA_DIR_NAME = "installreferrerdata";
    private static final String LOG_TAG = "aideanalytics";

    public void onReceive(Context context, Intent intent) {
        FileWriter fw;
        String referrer = intent.getExtras().getString("referrer");
        File installReferrerDataDir = context.getDir(DATA_DIR_NAME, 0);
        if (referrer != null) {
            File tmpDataFile = new File(installReferrerDataDir, "tmp-" + UUID.randomUUID().toString());
            File dataFile = new File(installReferrerDataDir, UUID.randomUUID().toString());
            try {
                fw = new FileWriter(tmpDataFile);
                fw.write(referrer);
                fw.close();
                tmpDataFile.renameTo(dataFile);
            } catch (IOException e1) {
                Log.e(LOG_TAG, "Writing referrer file failed", e1);
            } catch (Throwable th) {
                //fw.close();
            }
        }
    }
}
