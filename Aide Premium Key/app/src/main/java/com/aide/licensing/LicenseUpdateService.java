package com.aide.licensing;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import com.aide.licensing.util.LicensingLog;

public class LicenseUpdateService extends Service
{
    private GooglePlayLicensingInterface appBilling;

    public IBinder onBind(Intent arg0)
	{
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId)
	{
        LicensingLog.d("License Update service: onStartCommand");
        appBilling = new GooglePlayLicensingInterface();
        appBilling.init(this, true);
        appBilling.requestLicenseFileUpdate();
        return 1;
    }

    public void onDestroy()
	{
        LicensingLog.d("License Update service: onDestroy");
        appBilling.shutdown();
        appBilling = null;
    }
}
