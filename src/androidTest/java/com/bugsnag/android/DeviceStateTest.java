package com.bugsnag.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class DeviceStateTest {

    private DeviceState deviceState;

    @Before
    public void setUp() throws Exception {
        deviceState = new DeviceState(InstrumentationRegistry.getContext());
    }

    @Test
    public void testSaneValues() throws JSONException, IOException {
        JSONObject deviceStateJson = streamableToJson(deviceState);

        assertTrue(deviceStateJson.getLong("freeMemory") > 0);
        assertNotNull(deviceStateJson.get("orientation"));
        assertTrue(deviceStateJson.getDouble("batteryLevel") > 0);
        assertTrue(deviceStateJson.getBoolean("charging"));
        assertEquals("allowed", deviceStateJson.getString("locationStatus"));
        assertNotNull(deviceStateJson.get("networkAccess"));
        assertNotNull(deviceStateJson.get("time"));
    }
}
