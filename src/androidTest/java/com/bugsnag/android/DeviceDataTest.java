package com.bugsnag.android;

import com.bugsnag.android.DeviceData;

public class DeviceDataTest extends BugsnagTestCase {
    public void testNotNull() {
        DeviceData deviceData = new DeviceData(getContext());

        assertNotNull(deviceData.getScreenDensity());
        assertNotNull(deviceData.getScreenResolution());
        assertNotNull(deviceData.getTotalMemory());
        assertNotNull(deviceData.isRooted());
        assertNotNull(deviceData.getLocale());
        assertNotNull(deviceData.getAndroidId());
    }
}
