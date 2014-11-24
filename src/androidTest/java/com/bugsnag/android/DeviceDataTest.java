package com.bugsnag.android;

import org.json.JSONObject;

import com.bugsnag.android.DeviceData;

public class DeviceDataTest extends BugsnagTestCase {
    public void testSaneValues() {
        DeviceData deviceData = new DeviceData(getContext());

        assertTrue(deviceData.getScreenDensity() > 0);
        assertNotNull(deviceData.getScreenResolution());
        assertNotNull(deviceData.getTotalMemory());
        assertNotNull(deviceData.isRooted());
        assertNotNull(deviceData.getLocale());
        assertNotNull(deviceData.getAndroidId());
    }
}
