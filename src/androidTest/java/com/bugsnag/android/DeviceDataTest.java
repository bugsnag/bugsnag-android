package com.bugsnag.android;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.DisplayMetrics;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class DeviceDataTest extends BugsnagTestCase {

    public void testSaneValues() throws JSONException, IOException {
        Configuration config = new Configuration("some-api-key");
        SharedPreferences sharedPref = getSharedPrefs();
        DeviceData deviceData = new DeviceData(getContext(), sharedPref);
        JSONObject deviceDataJson = streamableToJson(deviceData);

        assertEquals("android", deviceDataJson.getString("osName"));
        assertTrue(deviceDataJson.getString("manufacturer").length() > 1);
        assertTrue(deviceDataJson.getString("brand").length() > 1);
        assertTrue(deviceDataJson.getString("model").length() > 1);

        assertTrue(deviceDataJson.getDouble("screenDensity") > 0);
        assertTrue(deviceDataJson.getDouble("dpi") >= DisplayMetrics.DENSITY_LOW);
        assertTrue(deviceDataJson.getString("screenResolution").matches("^\\d+x\\d+$"));
        assertTrue(deviceDataJson.getLong("totalMemory") > 0);
        assertNotNull(deviceDataJson.getBoolean("jailbroken"));
        assertNotNull(deviceDataJson.getString("locale"));
        assertNotNull(deviceDataJson.getString("cpuAbi"));

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
            // Emulators returned null for android id before android 2.2
            assertNotNull(deviceDataJson.getString("id"));

            // historically Android ID was used, this should no longer be the case
            ContentResolver cr = getContext().getContentResolver();
            @SuppressLint("HardwareIds")
            String androidId = Settings.Secure.getString(cr, Settings.Secure.ANDROID_ID);
            assertNotSame(androidId, deviceDataJson.getString("id"));
        }

    }
}
