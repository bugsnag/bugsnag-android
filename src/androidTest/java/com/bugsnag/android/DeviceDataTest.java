package com.bugsnag.android;

import org.json.JSONObject;

public class DeviceDataTest extends BugsnagTestCase {
    public void testSaneValues() {
        DeviceData deviceData = new DeviceData(getContext());

        assertTrue(deviceData.getScreenDensity() > 0);
        assertNotNull(deviceData.getScreenResolution());
        assertNotNull(deviceData.getTotalMemory());
        assertNotNull(deviceData.isRooted());
        assertNotNull(deviceData.getLocale());

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.FROYO) {
            // Emulators returned null for android id before android 2.2
            assertNotNull(deviceData.getAndroidId());
        }
    }
}
