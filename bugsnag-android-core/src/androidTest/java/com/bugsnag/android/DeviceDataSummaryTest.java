package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
import static org.junit.Assert.assertEquals;
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

import java.util.Map;

public class DeviceDataSummaryTest {

    private Map<String, Object> deviceData;

    /**
     * Generates a device data object
     */
    @Before
    public void setUp() {
        Connectivity connectivity = BugsnagTestUtils.generateConnectivity();
        Context context = ApplicationProvider.getApplicationContext();
        Resources resources = context.getResources();
        DeviceData deviceData = new DeviceData(connectivity, context, resources,
                "123", DeviceBuildInfo.Companion.defaultInfo(), NoopLogger.INSTANCE);
        this.deviceData = deviceData.getDeviceDataSummary();
    }

    @Test
    public void testAccessors() {
        assertNotNull(deviceData.get("manufacturer"));
        assertNotNull(deviceData.get("model"));
        assertNotNull(deviceData.get("osName"));
        assertNotNull(deviceData.get("osVersion"));
    }

    @Test
    public void testJsonSerialisation() throws JSONException {
        JSONObject deviceDataJson = mapToJson(deviceData);

        assertEquals("android", deviceDataJson.getString("osName"));
        assertNotNull(deviceDataJson.getString("osVersion"));
        assertNotNull(deviceDataJson.getString("manufacturer"));
        assertNotNull(deviceDataJson.getString("model"));
        assertNotNull(deviceDataJson.get("cpuAbi"));
        assertTrue(deviceDataJson.has("jailbroken"));

        JSONObject versions = deviceDataJson.getJSONObject("runtimeVersions");
        assertTrue((Integer) versions.get("androidApiLevel") >= 14);
    }

}
