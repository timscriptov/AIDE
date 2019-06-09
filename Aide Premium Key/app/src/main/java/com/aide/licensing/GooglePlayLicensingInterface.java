package com.aide.licensing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import com.aide.licensing.GooglePlayLicensingValidator.LicenseVerificationResult;
import com.aide.licensing.util.LicensingLog;
import com.aide.licensing.util.StreamUtilities;
import com.aide.premium.key.BuildConfig;
import com.android.vending.licensing.ILicenseResultListener.Stub;
import com.android.vending.licensing.ILicensingService;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.FileWriter;

public class GooglePlayLicensingInterface
{
    private static final String LICENSE_FILENAME = "license.txt";
    private static final long MILLIS_PER_HOUR = 3600000;
    private static final long MILLIS_PER_MINUTE = 60000;
    private Context context;
    private long dontRetryLicenseCheckBeforeTime;
    private Handler handler;
    private long lastServerLicenseCheckRequestTime;
    private ILicensingService licensingService;
    private boolean licensingServiceBindRequested;
    private ServiceConnection licensingServiceConnection;
    private IAideLicenseResultListener listener;
    private boolean onlyUpdateFile;

    static class GooglePlayLicensingInterface
	{
        static final int[] LicenseVerification = new int[LicenseVerificationResult.values().length];

        static {
            try
			{
                LicenseVerification[LicenseVerificationResult.LICENSED.ordinal()] = 1;
            }
			catch (NoSuchFieldError e)
			{
            }
            try
			{
                LicenseVerification[LicenseVerificationResult.IN_GRACE_PERIOD.ordinal()] = 2;
            }
			catch (NoSuchFieldError e2)
			{
            }
            try
			{
                LicenseVerification[LicenseVerificationResult.OVER_GRACE_PERIOD.ordinal()] = 3;
            }
			catch (NoSuchFieldError e3)
			{
            }
            try
			{
                LicenseVerification[LicenseVerificationResult.NOT_LICENSED.ordinal()] = 4;
            }
			catch (NoSuchFieldError e4)
			{
            }
            try
			{
                LicenseVerification[LicenseVerificationResult.TEMPORARY_ERROR.ordinal()] = 5;
            }
			catch (NoSuchFieldError e5)
			{
            }
        }
    }

    private class ResultListener extends Stub
	{
        private long nonce;

        public ResultListener(long nonce)
		{
            this.nonce = nonce;
        }

        public void verifyLicense(final int responseCode, final String signedData, final String signature) throws RemoteException
		{
            handler.post(new Runnable() {
					public void run()
					{
						lastServerLicenseCheckRequestTime = 0;
						verifyLicenseFromService(nonce, responseCode, signedData, signature);
					}
				});
        }
    }

    public void init(Context context, boolean onlyUpdateFile)
	{
        this.context = context;
        this.handler = new Handler();
        this.onlyUpdateFile = onlyUpdateFile;
    }

    public void shutdown()
	{
        if (licensingService != null)
		{
            LicensingLog.d("License service shutdown");
            context.unbindService(licensingServiceConnection);
            licensingServiceConnection = null;
            listener = null;
            licensingServiceBindRequested = false;
        }
    }

    public boolean doesLicenseFileExist()
	{
        return getLicenseFile().isFile();
    }

    public void requestLicenseFileUpdate()
	{
        LicenseVerificationResult res = verifyLicenseFromFile();
        boolean stopService = true;
        if ((res == LicenseVerificationResult.IN_GRACE_PERIOD || res == LicenseVerificationResult.OVER_GRACE_PERIOD) && initiateLicenseCheck(null))
		{
            stopService = false;
        }
        if (stopService)
		{
            stopLicenseUpdateService();
        }
    }

    private void stopLicenseUpdateService()
	{
        LicensingLog.d("Stopping LicenseUpdateService");
        context.stopService(new Intent(context, LicenseUpdateService.class));
    }

    public boolean initiateLicenseCheck(IAideLicenseResultListener l)
	{
        this.listener = l;
        if (System.currentTimeMillis() < dontRetryLicenseCheckBeforeTime)
		{
            return false;
        }
        LicenseVerificationResult res = verifyLicenseFromFile();
        switch (GooglePlayLicensingInterface.LicenseVerification[res.ordinal()])
		{
            case 1:
                return false;
            case IGooglePlayLicensingServiceResponseCodes.LICENSED_OLD_KEY /*2*/:
            case IGooglePlayLicensingServiceResponseCodes.ERROR_NOT_MARKET_MANAGED /*3*/:
            case IGooglePlayLicensingServiceResponseCodes.ERROR_SERVER_FAILURE /*4*/:
                if (licensingService != null)
				{
                    requestLicenseCheck();
                }
				else if (licensingServiceBindRequested)
				{
                    LicensingLog.d("Licensing service bind already pending");
                }
				else
				{
                    this.licensingServiceConnection = new ServiceConnection() {
                        public void onServiceDisconnected(ComponentName name)
						{
                            LicensingLog.w("Licensing service unexpectedly disconnected.");
                            licensingServiceBindRequested = false;
                            licensingService = null;
                        }

                        public void onServiceConnected(ComponentName name, IBinder binder)
						{
                            LicensingLog.d("Licensing service connected.");
                            licensingService = ILicensingService.Stub.asInterface(binder);
                            licensingServiceBindRequested = false;
                            requestLicenseCheck();
                        }
                    };
                    Intent serviceIntent = new Intent("com.android.vending.licensing.ILicensingService");
                    serviceIntent.setPackage("com.android.vending");
                    boolean licensingServiceBindRequestedSuccessfully = context.bindService(serviceIntent, licensingServiceConnection, 1);
                    if (licensingServiceBindRequestedSuccessfully)
					{
                        licensingServiceBindRequested = licensingServiceBindRequestedSuccessfully;
                    }
					else
					{
                        LicensingLog.d("Licensing service could not be bound");
                        return false;
                    }
                }
                return true;
            default:
                throw new RuntimeException("Unknown LicenseVerificationResult " + res);
        }
    }

    private void requestLicenseCheck()
	{
        if (System.currentTimeMillis() < lastServerLicenseCheckRequestTime + MILLIS_PER_MINUTE)
		{
            LicensingLog.d("License check responsed pending - throttled");
            return;
        }
        try
		{
            LicensingLog.d("Requesting license check");
            long nonce = Security.generateNonce(context);
            licensingService.checkLicense(nonce, getPackageName(), new ResultListener(nonce));
            lastServerLicenseCheckRequestTime = System.currentTimeMillis();
        }
		catch (RemoteException e)
		{
            LicensingLog.e(e);
        }
    }

    private String getPackageName()
	{
        return context.getApplicationContext().getPackageName();
    }

    private String getEncodedPublicKey()
	{
        if (getPackageName().equals(BuildConfig.APPLICATION_ID))
		{
            return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkjXTkIvABlLJgyvluBm4h4Ytw87BbBrSRXohMVMvk0Eq2pYJKT1jYfC1W65/YY5GcFYwMiaemVlpH40h/h+rkm+GYYq04awtN8zv35+HymbrW6ztGgNv7gF7ksAOOb8swqQwlx6uzfZWzbny7r9kkKURlXWmpkcKpWUjfoQ1MIRFxuXoJ2owTjvdkezI2hjCxC+NJ57QCG8tBoWJo5jscDRylHuaXBGVX7fJx7NsWzlk9xTXUFE7F8J5OaPRpgCNwo+xC0pXSoA/yCv1dOy4v2tPr9L9rfB95nrTwRs1ob44LOY3UqYgVMC1wpy+nxU6UGch8g8/DfHXfaefBcsbfwIDAQAB";
        }
        if (getPackageName().equals("com.aide.java.premium.key"))
		{
            return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn+LHwdRGxOPUMAOl1xlpO/jK/2cqCBxkbIlP0fjvubrkNhj+TdoFzXAPEVJQDmBhRdgfOW7T1JyNDKjQYHqvsqxecl5DSxy+e2c3do2+OmtXDgsCZnwsPoQKZlXqYmr2m7oohe+ogGQ6dvK5ToyLEHtJu6zBs4HNrHEDJAZz4+eG8UydfPJut0VaBxFhLTwdInzILO27JcabVjNWSbMj/3ClErZjcaM9wBhPaftBFpyQWBiIVL5dULHspqlXsiVljpIvz1UTwkHwE+hqJPhPGLBIe6xqNPyv16IOssFskia97yekprSeoMaP0xzc/c7KT2sqsVuCUJsm41yDbqvIDwIDAQAB";
        }
        if (getPackageName().equals("com.aide.designer"))
		{
            return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAscoEgrFdBkxPcWpFbDQhJaTdZ92LV4sGN7NdUmkp1OIL9m9vO8cYFRU2ZCpHEQ+gilfA2T37855hqegNJfv3Uv/Rh9gZYy+cEGyqEnZyjZ0eYXEMcRmuvLNCrkFpg8xlzRpAvwMB7Tseez5GSYuwefuSle5OBLYrKYxZ14qIiHBwYrS338+v0jIav07YAlwxqkLIEWikNN0lI7ZuJlhcgIiwCJCDT9WtHaU8GgOGYh4cTLnuNAo5FOsSNnEmSmdc4jRyWCPSSETMs+fbEy3BsDKpaO4I2b/VO0/GVmyI9jkYJgkmx/33gYKSTRw9BZR3yy7Vtnq64/afnSoMYqzkDQIDAQAB";
        }
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAh9PowqySSG6WBaQx3i5y+3vjHUlva+HzWV7ZlYCkMdY5rHxp48M566VZjQVPV9xF0a47f0n0kdECfNGcLvLtQdB6rhk1xW6ouQG+uCiLmjLomLVZbaHPS7kDDP0ETU7SpVksPfPFlHCNLgZ/68HQzWMv7WmQ5Y1+9x3QQ4EfC3gsQdftsNyjSr7GZ7oR3dMDPyMkjNa5tHi4ZzSEHJQyutD7ezURXAsmyEvFUphp871vKCHNrNA+NHt2KMptZdRuCpip6yr6yyD1uFGWZ7XDEtu9Gtt93dWlC6PyecA21LHbGGbv6M9T5WkyqYRyorDKxdfOXIYuuW2I5PdmiE//+wIDAQAB";
    }

    private LicenseVerificationResult verifyLicense(long expectedNonce, int receivedResponseCode, String signedData, String signature, boolean fromFile)
	{
        int versionCode = -1;
        try
		{
            versionCode = fromFile ? -1 : context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionCode;
        }
		catch (NameNotFoundException e)
		{
            LicensingLog.e(e);
        }
		catch (Throwable th)
		{
            if (!fromFile && onlyUpdateFile)
			{
                stopLicenseUpdateService();
            }
        }
        LicenseVerificationResult res = GooglePlayLicensingValidator.verifyLicense(context, expectedNonce, receivedResponseCode, versionCode, getPackageName(), signedData, signature, getEncodedPublicKey());
        switch (GooglePlayLicensingInterface.LicenseVerification[res.ordinal()])
		{
            case 1:
            case IGooglePlayLicensingServiceResponseCodes.LICENSED_OLD_KEY /*2*/:
            case IGooglePlayLicensingServiceResponseCodes.ERROR_NOT_MARKET_MANAGED /*3*/:
                LicenseUpdateAlarmReceiver.startLicenseCheckAlarm(context);
                dontRetryLicenseCheckBeforeTime = GooglePlayLicensingValidator.parseAndVerify(signedData, signature, getEncodedPublicKey()).validityTimestamp;
                if (!fromFile && res == LicenseVerificationResult.LICENSED)
				{
                    writeLicenseInfoToDisk(signedData, signature);
                }
                boolean allowAccess = res == LicenseVerificationResult.LICENSED || res == LicenseVerificationResult.IN_GRACE_PERIOD;
                if (!(onlyUpdateFile || listener == null))
				{
                    if (allowAccess)
					{
                        try
						{
                            listener.licenseVerified(signedData, signature);
                        }
						catch (RemoteException e2)
						{
                            LicensingLog.e(e2);
                        }
                    }
					else
					{
                        try
						{
							listener.licenseNotVerified();
						}
						catch (RemoteException e)
						{
							
						}
                    }
                }
                LicenseUpdateAlarmReceiver.startLicenseCheckAlarm(context);
                break;
            case IGooglePlayLicensingServiceResponseCodes.ERROR_SERVER_FAILURE /*4*/:
                if (!(this.onlyUpdateFile || listener == null))
				{
                    try
					{
                        this.listener.licenseNotVerified();
                    }
					catch (RemoteException e22)
					{
                        LicensingLog.e(e22);
                    }
                }
                this.dontRetryLicenseCheckBeforeTime = System.currentTimeMillis() + MILLIS_PER_HOUR;
                break;
            case IGooglePlayLicensingServiceResponseCodes.ERROR_OVER_QUOTA /*5*/:
                break;
            default:
                if (!(onlyUpdateFile || listener == null))
				{
                    try
					{
                        listener.licenseNotVerified();
                    }
					catch (RemoteException e222)
					{
                        LicensingLog.e(e222);
                    }
                }
                dontRetryLicenseCheckBeforeTime = System.currentTimeMillis() + MILLIS_PER_HOUR;
                throw new RuntimeException("Unknown LicenseVerificationResult " + res);
        }
        if (!fromFile && onlyUpdateFile)
		{
            stopLicenseUpdateService();
        }
        return res;
    }

    private void writeLicenseInfoToDisk(String signedData, String signature) {
        FileWriter fw = null;
        try {
            FileWriter fw2 = new FileWriter(getLicenseFile());
            try {
                fw2.write(signedData);
                fw2.write(0);
                fw2.write(signature);
                if (fw2 != null) {
                    try {
                        fw2.close();
                    } catch (IOException e2) {
                        LicensingLog.e(e2);
                        fw = fw2;
                        return;
                    }
                }
                fw = fw2;
            } catch (IOException e3) {
                fw = fw2;
                try {
                    LicensingLog.e(e3);
                    if (fw != null) {
                        try {
                            fw.close();
                        } catch (IOException e22) {
                            LicensingLog.e(e22);
                        }
                    }
                } catch (Throwable th2) {
                    if (fw != null) {
                        try {
                            fw.close();
                        } catch (IOException e222) {
                            LicensingLog.e(e222);
                        }
                    }
                }
            } catch (Throwable th3) {
                fw = fw2;
                if (fw != null) {
                    fw.close();
                }
            }
        } catch (IOException e4) {
            LicensingLog.e(e4);
            if (fw != null) {
                try
				{
					fw.close();
				}
				catch (IOException ignored)
				{
					
				}
            }
        }
    }

    private String[] readLicenseInfoFromDisk()
	{
        String[] strArr = null;
        File file = getLicenseFile();
        if (!file.isFile())
		{
            return strArr;
        }
        try
		{
            Reader fr = new FileReader(file);
            Writer sw = new StringWriter();
            StreamUtilities.transfer(fr, sw);
            return sw.toString().split("\u0000");
        }
		catch (IOException e)
		{
            LicensingLog.e(e);
            return strArr;
        }
    }

    private File getLicenseFile()
	{
        return new File(context.getFilesDir(), LICENSE_FILENAME);
    }

    private void verifyLicenseFromService(long expectedNonce, int expectedResponseCode, String signedData, String signature)
	{
        verifyLicense(expectedNonce, expectedResponseCode, signedData, signature, false);
    }

    private LicenseVerificationResult verifyLicenseFromFile()
	{
        String[] fileContents = readLicenseInfoFromDisk();
        if (fileContents == null || fileContents.length != 2)
		{
            return LicenseVerificationResult.NOT_LICENSED;
        }
        return verifyLicense(-1, -1, fileContents[0], fileContents[1], true);
    }
}
