package com.bugsnag.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.DisplayMetrics;

public class DeviceDataTest extends BugsnagTestCase {
    public void testSaneValues() throws JSONException {
        Configuration config = new Configuration("some-api-key");
        DeviceData deviceData = new DeviceData(getContext());
        JSONObject deviceDataJson = streamableToJson(deviceData);

        assertTrue(deviceDataJson.getDouble("screenDensity") > 0);
        assertTrue(deviceDataJson.getDouble("dpi") >= DisplayMetrics.DENSITY_LOW);
        assertTrue(deviceDataJson.getString("screenResolution").matches("^\\d+x\\d+$"));
        assertTrue(deviceDataJson.getLong("totalMemory") > 0);
        assertNotNull(deviceDataJson.getBoolean("jailbroken"));
        assertNotNull(deviceDataJson.getString("locale"));

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
            // Emulators returned null for android id before android 2.2
            assertNotNull(deviceDataJson.getString("id"));
        }
    }
}
