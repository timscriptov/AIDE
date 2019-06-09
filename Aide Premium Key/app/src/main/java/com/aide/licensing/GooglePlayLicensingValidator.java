package com.aide.licensing;

import android.content.Context;
import com.aide.licensing.util.Base64;
import com.aide.licensing.util.Base64DecoderException;
import com.aide.licensing.util.LicensingLog;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class GooglePlayLicensingValidator
{
    private static final String KEY_FACTORY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA1withRSA";

    public enum LicenseVerificationResult
	{
        LICENSED,
        IN_GRACE_PERIOD,
        OVER_GRACE_PERIOD,
        NOT_LICENSED,
        TEMPORARY_ERROR
		}

    private static PublicKey generatePublicKey(String encodedPublicKey)
	{
        try
		{
            return KeyFactory.getInstance(KEY_FACTORY_ALGORITHM).generatePublic(new X509EncodedKeySpec(Base64.decode(encodedPublicKey)));
        }
		catch (NoSuchAlgorithmException e)
		{
            throw new RuntimeException(e);
        }
		catch (Base64DecoderException e2)
		{
            LicensingLog.e("Could not decode from Base64.");
            throw new IllegalArgumentException(e2);
        }
		catch (InvalidKeySpecException e3)
		{
            LicensingLog.e("Invalid key specification.");
            throw new IllegalArgumentException(e3);
        }
    }

    private static boolean verifySignature(String signedData, String signature, String encodedPublicKey)
	{
        PublicKey publicKey = generatePublicKey(encodedPublicKey);
        try
		{
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(signedData.getBytes());
            if (sig.verify(Base64.decode(signature)))
			{
                return true;
            }
            LicensingLog.e("Signature verification failed.");
            return false;
        }
		catch (NoSuchAlgorithmException e)
		{
            throw new RuntimeException(e);
        }
		catch (InvalidKeyException e2)
		{
            LicensingLog.e("Invalid public key.");
            return false;
        }
		catch (SignatureException e3)
		{
            throw new RuntimeException(e3);
        }
		catch (Base64DecoderException e4)
		{
            LicensingLog.e("Could not Base64-decode signature.");
            return false;
        }
    }

    public static LicenseVerificationResult verifyLicense(Context context, long expectedNonce, int receivedResponseCode, int expectedVersionCode, String expectedPackageName, String signedData, String signature, String publicKey)
	{
        LicensingLog.d((receivedResponseCode == -1 ? "Licensing data from file" : "Licensing service response") + " - code: " + Integer.toHexString(receivedResponseCode) + " data: " + signedData + " sig: " + signature);
        GooglePlayLicensingResponseData data = null;
        if (signedData != null)
		{
            data = parseAndVerify(signedData, signature, publicKey);
        }
        if (receivedResponseCode == -1 && data == null)
		{
            return LicenseVerificationResult.NOT_LICENSED;
        }
        if (data != null && !verifyData(context, expectedNonce, receivedResponseCode, expectedVersionCode, expectedPackageName, false, data))
		{
            return LicenseVerificationResult.NOT_LICENSED;
        }
        int responseCode;
        if (receivedResponseCode == -1)
		{
            responseCode = data.responseCode;
        }
		else
		{
            responseCode = receivedResponseCode;
        }
        switch (responseCode)
		{
            case IGooglePlayLicensingServiceResponseCodes.LICENSED /*0*/:
            case IGooglePlayLicensingServiceResponseCodes.LICENSED_OLD_KEY /*2*/:
                if (data == null)
				{
                    LicensingLog.e("Licensed but no data");
                    return LicenseVerificationResult.NOT_LICENSED;
                }
                boolean licensed = System.currentTimeMillis() < data.validityTimestamp;
                boolean inGracePeriod = System.currentTimeMillis() < data.graceTimestamp;
                if (!inGracePeriod && data.graceTimestamp - data.timestamp > 432000000)
				{
                    inGracePeriod = true;
                }
                if (licensed)
				{
                    return LicenseVerificationResult.LICENSED;
                }
                if (inGracePeriod)
				{
                    return LicenseVerificationResult.IN_GRACE_PERIOD;
                }
                return LicenseVerificationResult.OVER_GRACE_PERIOD;
            case 1:
                LicensingLog.d("Not licensed.");
                break;
            case IGooglePlayLicensingServiceResponseCodes.ERROR_NOT_MARKET_MANAGED /*3*/:
            case IGooglePlayLicensingServiceResponseCodes.ERROR_INVALID_PACKAGE_NAME /*258*/:
            case IGooglePlayLicensingServiceResponseCodes.ERROR_NON_MATCHING_UID /*259*/:
                LicensingLog.w("Licensing error: " + responseCode);
                break;
            case IGooglePlayLicensingServiceResponseCodes.ERROR_SERVER_FAILURE /*4*/:
                LicensingLog.w("An error has occurred on the licensing server.");
                return LicenseVerificationResult.TEMPORARY_ERROR;
            case IGooglePlayLicensingServiceResponseCodes.ERROR_OVER_QUOTA /*5*/:
                LicensingLog.w("Licensing server is refusing to talk to this device, over quota.");
                return LicenseVerificationResult.TEMPORARY_ERROR;
            case IGooglePlayLicensingServiceResponseCodes.ERROR_CONTACTING_SERVER /*257*/:
                LicensingLog.w("Error contacting licensing server.");
                return LicenseVerificationResult.TEMPORARY_ERROR;
            default:
                LicensingLog.e("Unknown response code for license check: " + responseCode);
                break;
        }
        return LicenseVerificationResult.NOT_LICENSED;
    }

    public static boolean verifyData(Context context, long expectedNonce, int expectedResponseCode, int expectedVersionCode, String expectedPackageName, boolean verifyDeviceId, GooglePlayLicensingResponseData data)
	{
        if (expectedResponseCode != -1 && data.responseCode != expectedResponseCode)
		{
            LicensingLog.e("Response codes don't match.");
            return false;
        }
		else if (expectedNonce != -1 && data.nonce != expectedNonce)
		{
            LicensingLog.e("Nonce doesn't match.");
            return false;
        }
		else if (verifyDeviceId && !Security.verifyNonceForDevice(context, data.nonce))
		{
            LicensingLog.e("Nonce does not match device.");
            return false;
        }
		else if (data.packageName.equals(expectedPackageName))
		{
            try
			{
                int versionCode = Integer.parseInt(data.versionCode);
                if (expectedVersionCode == -1 || versionCode == expectedVersionCode)
				{
                    return true;
                }
                LicensingLog.e("Unexpected version code");
                return false;
            }
			catch (NumberFormatException e)
			{
                LicensingLog.e("Non-int version code");
                return false;
            }
        }
		else
		{
            LicensingLog.e("Package name doesn't match.");
            return false;
        }
    }

    public static boolean responseShouldBeSigned(int responseCode)
	{
        return responseCode == 0 || responseCode == 1 || responseCode == 2;
    }

    public static GooglePlayLicensingResponseData parseAndVerify(String signedData, String signature, String encodedPublicKey)
	{
        try
		{
            GooglePlayLicensingResponseData data = GooglePlayLicensingResponseData.parse(signedData);
            if (!responseShouldBeSigned(data.responseCode) || verifySignature(signedData, signature, encodedPublicKey))
			{
                return data;
            }
            return null;
        }
		catch (IllegalArgumentException e)
		{
            LicensingLog.e("Could not parse response.", e);
            return null;
        }
    }
}
