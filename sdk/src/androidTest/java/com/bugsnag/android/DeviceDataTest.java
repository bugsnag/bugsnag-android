package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.getSharedPrefs;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;
import android.support.test.InstrumentationRegistry;
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

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceDataTest {

    private DeviceData deviceData;

    @Before
    public void setUp() throws Exception {
        DeviceDataCollector deviceDataCollector = new DeviceDataCollector(generateClient());
        deviceData = deviceDataCollector.generateDeviceData();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
    }

    @Test
    public void testId() {
        assertNotNull(deviceData.getId());
        String expected = "abc123";
        deviceData.setId(expected);
        assertEquals(expected, deviceData.getId());
    }

    @Test
    public void testOrientation() {
        assertNotNull(deviceData.getOrientation());
        String expected = "landscape";
        deviceData.setOrientation(expected);
        assertEquals(expected, deviceData.getOrientation());
    }

    @Test
    public void testFreeMemory() {
        assertTrue(deviceData.getFreeMemory() > 0);
        long expected = 15000000L;
        deviceData.setFreeMemory(expected);
        assertEquals(expected, deviceData.getFreeMemory());
    }

    @Test
    public void testTotalMemory() {
        assertTrue(deviceData.getTotalMemory() > 0);
        long expected = 15000000L;
        deviceData.setTotalMemory(expected);
        assertEquals(expected, deviceData.getTotalMemory());
    }

    @Test
    public void testFreeDisk() {
        Long expected = 15000000L;
        deviceData.setFreeDisk(expected);
        assertEquals(expected, deviceData.getFreeDisk());
    }

    @Test
    public void testJsonSerialisation() throws IOException, JSONException {
        JSONObject deviceDataJson = streamableToJson(deviceData);

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
