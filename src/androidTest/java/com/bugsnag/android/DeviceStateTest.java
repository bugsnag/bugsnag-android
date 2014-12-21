package com.bugsnag.android;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceStateTest extends BugsnagTestCase {
    public void testSaneValues() throws JSONException {
        Configuration config = new Configuration("some-api-key");
        DeviceState deviceState = new DeviceState(getContext());
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
