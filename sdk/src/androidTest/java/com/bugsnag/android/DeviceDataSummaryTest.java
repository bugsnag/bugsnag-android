package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import java.io.IOException;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceDataSummaryTest {

    private Map<String, Object> deviceData;

    @Before
    public void setUp() throws Exception {
        DeviceData deviceData = new DeviceData(generateClient());
        this.deviceData = deviceData.getDeviceDataSummary();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
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
        assertTrue(deviceDataJson.has("jailbroken"));
    }

}
