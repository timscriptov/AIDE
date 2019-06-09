package com.aide.licensing;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public class Security
{
    private static final SecureRandom RANDOM = new SecureRandom();

    public static long generateNonce(Context context)
	{
        return (RANDOM.nextLong() << 32) | getDeviceId(context);
    }

    public static boolean verifyNonceForDevice(Context context, long nonce)
	{
        return (4294967295L & nonce) == getDeviceId(context);
    }

    public static long getDeviceId(Context context)
	{
        byte[] deviceIdBytes = getDeviceIdKey(context);
        return 268435455 & ((((((long) deviceIdBytes[0]) & 255) | ((((long) deviceIdBytes[1]) & 255) << 8)) | ((((long) deviceIdBytes[2]) & 255) << 16)) | ((((long) deviceIdBytes[3]) & 255) << 24));
    }

    private static byte[] getDeviceIdKey(Context context)
	{
        try
		{
            return MessageDigest.getInstance("SHA-256").digest(getDeviceIdString(context).getBytes("UTF-8"));
        }
		catch (Exception e)
		{
            throw new RuntimeException(e);
        }
    }

    private static String getDeviceIdString(Context context)
	{
        String telephonyDeviceId = "(default)";
        String telephonySimSerialNo = "(default)";
        try
		{
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
            try
			{
                telephonyDeviceId = telephonyManager.getDeviceId();
            }
			catch (Exception e)
			{
            }
            try
			{
                telephonySimSerialNo = telephonyManager.getSimSerialNumber();
            }
			catch (Exception e2)
			{
            }
        }
		catch (Exception e3)
		{
        }
        String android_id = "(default)";
        try
		{
            android_id = Secure.getString(context.getContentResolver(), "android_id");
        }
		catch (Exception e4)
		{
        }
        StringBuilder sb = new StringBuilder();
        sb.append("android id: ");
        sb.append(android_id);
        sb.append(";");
        sb.append("tel device id: ");
        sb.append(telephonyDeviceId);
        sb.append(";");
        sb.append("tel sim serial no: ");
        sb.append(telephonySimSerialNo);
        sb.append(";");
        sb.append("accounts: ");
        try
		{
            Account[] accts = AccountManager.get(context).getAccountsByType("com.google");
            Arrays.sort(accts, new Comparator<Account>() {
					public int compare(Account lhs, Account rhs)
					{
						return Security.clean(lhs.name).compareTo(Security.clean(rhs.name));
					}
				});
            for (Account acct : accts)
			{
                try
				{
                    sb.append(clean(acct.name));
                    sb.append(",");
                }
				catch (Exception e5)
				{
                }
            }
        }
		catch (Exception e6)
		{
        }
        return sb.toString();
    }

    private static String clean(String s)
	{
        if (s == null)
		{
            return "";
        }
        return s.trim().toLowerCase(Locale.US);
    }
}
