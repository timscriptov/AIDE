package com.aide.ndk;

import com.google.android.vending.expansion.downloader.impl.DownloaderService;

public class AideNdkDownloaderService extends DownloaderService
{
	public static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlOaP9HhuSnkTl2I27NNgBAcRp9KrPYSXD0G9maKXuPrHRiya9FLp9jDNuOUJ8RbWTbMiJ9eDc2PWAOv/pbb0uHXxH0+wKD92f6jNZCm2+OlizlJj4xvCwHg5w4CYPp92J1/eb9VIGc1hY8ElMKvJ7yWpuOR39fB2dHUPzqHytmSoPFQ+6/aCpdBgjlhFUMg99lm+bLyMEr0HupdcO4qVL/KBM4z2q9YZbOS2CNZGrDNXTaZ7VLO0hZ3CSva6ECL1/gbF9+MW52IS6A24Wy3P3tsKxqXlLkA8Hhrar2+Pip90qaQonLyxCzJVeCoX7Im6S7qkt77E9PuGdCVBSizpRwIDAQAB";
	public static final byte[] SALT = new byte[]{ 1, 42, -12, -1, 54, 98, -100, -12, 43, 2, -8, -4, 13, 9, -106, -107, -33, 45, -1, 84};
    
	public String getPublicKey()
	{
        return BASE64_PUBLIC_KEY;
    }

    public byte[] getSALT()
	{
        return SALT;
    }

    public String getAlarmReceiverClassName()
	{
        return AideNdkAlarmReceiver.class.getName();
    }
}
