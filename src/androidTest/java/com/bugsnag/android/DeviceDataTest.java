package com.bugsnag.android;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.DisplayMetrics;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.bugsnag.android.BugsnagTestUtils.getSharedPrefs;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceDataTest {

    private DeviceData deviceData;

    @Before
    public void setUp() throws Exception {
        SharedPreferences sharedPref = getSharedPrefs(InstrumentationRegistry.getContext());
        deviceData = new DeviceData(InstrumentationRegistry.getContext(), sharedPref);
    }

    @Test
    public void testSaneValues() throws JSONException, IOException {
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

        // Emulators returned null for android id before android 2.2
        assertNotNull(deviceDataJson.getString("id"));

        // historically Android ID was used, this should no longer be the case
        ContentResolver cr = InstrumentationRegistry.getContext().getContentResolver();
        @SuppressLint("HardwareIds")
        String androidId = Settings.Secure.getString(cr, Settings.Secure.ANDROID_ID);
        assertNotSame(androidId, deviceDataJson.getString("id"));

    }
}
