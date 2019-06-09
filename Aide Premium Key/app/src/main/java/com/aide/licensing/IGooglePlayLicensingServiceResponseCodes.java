package com.aide.licensing;

public interface IGooglePlayLicensingServiceResponseCodes
{
    public static final int ERROR_CONTACTING_SERVER = 257;
    public static final int ERROR_INVALID_PACKAGE_NAME = 258;
    public static final int ERROR_NON_MATCHING_UID = 259;
    public static final int ERROR_NOT_MARKET_MANAGED = 3;
    public static final int ERROR_OVER_QUOTA = 5;
    public static final int ERROR_SERVER_FAILURE = 4;
    public static final int LICENSED = 0;
    public static final int LICENSED_OLD_KEY = 2;
    public static final int NOT_LICENSED = 1;
}
