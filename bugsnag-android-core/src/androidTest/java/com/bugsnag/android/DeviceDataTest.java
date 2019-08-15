package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

public class DeviceDataTest {

    private Map<String, Object> deviceData;

    /**
     * Generates a device data object
     */
    @Before
    public void setUp() throws Exception {
        Connectivity connectivity = BugsnagTestUtils.generateConnectivity();
        Context context = ApplicationProvider.getApplicationContext();
        Resources resources = context.getResources();
        SharedPreferences prefs = context.getSharedPreferences("", Context.MODE_PRIVATE);
        DeviceData deviceData = new DeviceData(connectivity, context, resources, "123");
        this.deviceData = deviceData.getDeviceData();
    }

    @Test
    public void testAccessors() {
        assertNotNull(deviceData.get("id"));
        assertNotNull(deviceData.get("orientation"));
        assertTrue((Long) deviceData.get("freeMemory") > 0);
        assertTrue((Long) deviceData.get("totalMemory") > 0);
    }

    @Test
    public void testJsonSerialisation() throws IOException, JSONException {
        JSONObject deviceDataJson = mapToJson(deviceData);

        // serialises inherited fields correctly
        for (String key : Arrays.asList("osName",
            "osVersion", "manufacturer", "model", "jailbroken")) {
            assertTrue(deviceDataJson.has(key));
        }

        assertNotNull(deviceDataJson.getString("id"));
        assertTrue(deviceDataJson.getLong("freeMemory") > 0);
        assertTrue(deviceDataJson.getLong("totalMemory") > 0);
        assertTrue(deviceDataJson.has("freeDisk"));
        assertNotNull(deviceDataJson.getString("orientation"));
    }

}
