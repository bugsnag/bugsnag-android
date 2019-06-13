package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceDataSummaryTest {

    private Map<String, Object> deviceData;

    /**
     * Generates a device data object
     */
    @Before
    public void setUp() throws Exception {
        ConnectivityCompat connectivityCompat = BugsnagTestUtils.generateConnectivityCompat();
        DeviceData deviceData = new DeviceData(generateClient(), connectivityCompat);
        this.deviceData = deviceData.getDeviceDataSummary();
    }

    @Test
    public void testManufacturer() {
        assertNotNull(deviceData.get("manufacturer"));
    }

    @Test
    public void testModel() {
        assertNotNull(deviceData.get("model"));
    }

    @Test
    public void testOsName() {
        assertNotNull(deviceData.get("osName"));
    }

    @Test
    public void testOsVersion() {
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
