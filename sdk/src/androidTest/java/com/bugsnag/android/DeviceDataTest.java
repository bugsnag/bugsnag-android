package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceDataTest {

    private Map<String, Object> deviceData;

    @Before
    public void setUp() throws Exception {
        DeviceData deviceData = new DeviceData(generateClient());
        this.deviceData = deviceData.getDeviceData();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
    }

    @Test
    public void testId() {
        assertNotNull(deviceData.get("id"));
    }

    @Test
    public void testOrientation() {
        assertNotNull(deviceData.get("orientation"));
    }

    @Test
    public void testFreeMemory() {
        assertTrue((Long) deviceData.get("freeMemory") > 0);
    }

    @Test
    public void testTotalMemory() {
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
