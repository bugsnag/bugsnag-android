package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
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

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceDataSummaryTest {

    private DeviceDataSummary deviceData;

    @Before
    public void setUp() throws Exception {
        DeviceDataCollector deviceDataCollector = new DeviceDataCollector(generateClient());
        deviceData = deviceDataCollector.generateDeviceDataSummary();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
    }

    @Test
    public void testManufacturer() {
        assertNotNull(deviceData.getManufacturer());
        String expected = "Apple";
        deviceData.setManufacturer(expected); // give it 10 years
        assertEquals(expected, deviceData.getManufacturer());
    }

    @Test
    public void testModel() {
        assertNotNull(deviceData.getModel());
        String expected = "Samsung S3";
        deviceData.setModel(expected);
        assertEquals(expected, deviceData.getModel());
    }

    @Test
    public void testOsName() {
        assertNotNull(deviceData.getOsName());
        String expected = "ChromeOS";
        deviceData.setOsName(expected);
        assertEquals(expected, deviceData.getOsName());
    }

    @Test
    public void testOsVersion() {
        assertNotNull(deviceData.getOsVersion());
        String expected = "Cyanogen 5";
        deviceData.setOsVersion(expected);
        assertEquals(expected, deviceData.getOsVersion());
    }

    @Test
    public void testJsonSerialisation() throws IOException, JSONException {
        JSONObject deviceDataJson = streamableToJson(deviceData);

        assertEquals("android", deviceDataJson.getString("osName"));
        assertNotNull(deviceDataJson.getString("osVersion"));
        assertNotNull(deviceDataJson.getString("manufacturer"));
        assertNotNull(deviceDataJson.getString("model"));
        assertTrue(deviceDataJson.has("jailbroken"));
    }

}
