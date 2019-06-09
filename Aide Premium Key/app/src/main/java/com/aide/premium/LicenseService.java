package com.aide.premium;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import com.aide.licensing.GooglePlayLicensingInterface;
import com.aide.licensing.IAideLicenseResultListener;
import com.aide.licensing.IAideLicensingService.Stub;
import com.aide.licensing.util.LicensingLog;

public class LicenseService extends Service
{
    private GooglePlayLicensingInterface appBilling;
    private Handler handler;

    public IBinder onBind(Intent intent)
	{
        LicensingLog.d("License service: onBind");
        handler = new Handler();
        appBilling = new GooglePlayLicensingInterface();
        appBilling.init(this, false);
        return new Stub() {
            public void checkLicense(final IAideLicenseResultListener l) throws RemoteException
			{
                if (Binder.getCallingUid() != Process.myUid())
				{
                    LicensingLog.e("UID mismatch - my uid: " + Process.myUid() + " calling uid: " + Binder.getCallingUid());
                }
				else
				{
                    handler.post(new Runnable() {
							public void run()
							{
								checkLicenseImpl(l);
							}
						});
                }
            }
        };
    }

    private void checkLicenseImpl(IAideLicenseResultListener l)
	{
        appBilling.initiateLicenseCheck(l);
    }

    public boolean onUnbind(Intent intent)
	{
        stopSelf();
        return super.onUnbind(intent);
    }

    public void onDestroy()
	{
        LicensingLog.d("License service: onDestroy");
        appBilling.shutdown();
        appBilling = null;
    }
}
