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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceDataSummaryTest {

    private Map<String, Object> deviceData;
    private Client client;

    /**
     * Generates a device data object
     */
    @Before
    public void setUp() throws Exception {
        Connectivity connectivity = BugsnagTestUtils.generateConnectivity();
        client = generateClient();
        DeviceData deviceData = new DeviceData(client, connectivity);
        this.deviceData = deviceData.getDeviceDataSummary();
    }

    @After
    public void tearDown() {
        client.close();
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
