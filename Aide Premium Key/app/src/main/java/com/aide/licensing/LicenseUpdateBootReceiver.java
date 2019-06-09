package com.aide.licensing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.aide.licensing.util.LicensingLog;

public class LicenseUpdateBootReceiver extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent)
	{
        LicensingLog.d("Boot broadcast received.");
        LicenseUpdateAlarmReceiver.startLicenseCheckAlarmAfterBoot(context);
    }
}
