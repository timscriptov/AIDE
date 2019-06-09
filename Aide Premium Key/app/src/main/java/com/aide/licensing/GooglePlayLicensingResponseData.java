package com.aide.licensing;

import android.text.TextUtils;
import com.aide.licensing.util.LicensingLog;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

public class GooglePlayLicensingResponseData
{
    public String extra;
    public long graceTimestamp;
    public long nonce;
    public String packageName;
    public int responseCode;
    public long timestamp;
    public String userId;
    public long validityTimestamp;
    public String versionCode;

    public static GooglePlayLicensingResponseData parse(String responseData)
	{
        String mainData;
        String extraData;
        int index = responseData.indexOf(58);
        if (-1 == index)
		{
            mainData = responseData;
            extraData = "";
        }
		else
		{
            mainData = responseData.substring(0, index);
            if (index >= responseData.length())
			{
                extraData = "";
            }
			else
			{
                extraData = responseData.substring(index + 1);
            }
        }
        String[] fields = TextUtils.split(mainData, Pattern.quote("|"));
        if (fields.length < 6)
		{
            throw new IllegalArgumentException("Wrong number of fields.");
        }
        GooglePlayLicensingResponseData data = new GooglePlayLicensingResponseData();
        data.extra = extraData;
        data.responseCode = Integer.parseInt(fields[0]);
        data.nonce = Long.parseLong(fields[1]);
        data.packageName = fields[2];
        data.versionCode = fields[3];
        data.userId = fields[4];
        data.timestamp = Long.parseLong(fields[5]);
        LicensingLog.d("timestamp: " + new Date(data.timestamp).toString());
        if (data.responseCode == 0 || data.responseCode == 2)
		{
            Map<String, String> results = new HashMap<String, String>();
            try
			{
                for (NameValuePair item : URLEncodedUtils.parse(new URI("?" + data.extra), "UTF-8"))
				{
                    results.put(item.getName(), item.getValue());
                }
                data.validityTimestamp = Long.parseLong(results.get("VT"));
                data.graceTimestamp = Long.parseLong(results.get("GT"));
                LicensingLog.d("validity timestamp: " + new Date(data.validityTimestamp).toString());
                LicensingLog.d("grace timestamp: " + new Date(data.graceTimestamp).toString());
            }
			catch (URISyntaxException e)
			{
                throw new IllegalArgumentException(e);
            }
        }
        return data;
    }

    public String toString()
	{
        return TextUtils.join("|", new Object[]{Integer.valueOf(responseCode), Long.valueOf(nonce), packageName, versionCode, userId, Long.valueOf(timestamp)});
    }
}
