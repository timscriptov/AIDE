package com.aide.licensing;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.aide.licensing.util.LicensingLog;

public class LicenseUpdateAlarmReceiver extends BroadcastReceiver
{
    private static final int BOOT_INITIAL_DELAY = 120000;
    
    public void onReceive(Context context, Intent intent)
	{
        LicensingLog.d("Timer broadcast received.");
        context.startService(new Intent(context, LicenseUpdateService.class));
    }

    public static void startLicenseCheckAlarm(Context context)
	{
        startLicenseCheckAlarm(context, 3600000, true);
    }

    public static void startLicenseCheckAlarmAfterBoot(Context context)
	{
        startLicenseCheckAlarm(context, BOOT_INITIAL_DELAY, false);
    }

    private static void startLicenseCheckAlarm(Context context, int initialDelay, boolean allowInexactTiming)
	{
        GooglePlayLicensingInterface appBilling = new GooglePlayLicensingInterface();
        appBilling.init(context, true);
        if (appBilling.doesLicenseFileExist())
		{
            LicensingLog.d("Restarting timer.");
            AlarmManager am = (AlarmManager) context.getSystemService("alarm");
            PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(context, LicenseUpdateAlarmReceiver.class), 0);
            if (allowInexactTiming)
			{
                am.setInexactRepeating(0, System.currentTimeMillis() + ((long) initialDelay), 3600000, pi);
            }
			else
			{
                am.setRepeating(0, System.currentTimeMillis() + ((long) initialDelay), 3600000, pi);
            }
        }
    }
}
