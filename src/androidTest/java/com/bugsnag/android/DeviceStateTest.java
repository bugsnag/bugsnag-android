package com.bugsnag.android;

import com.bugsnag.android.DeviceState;

public class DeviceStateTest extends BugsnagTestCase {
    public void testNotNull() {
        DeviceState deviceData = new DeviceState(getContext());

        assertNotNull(deviceData.getFreeMemory());
        assertNotNull(deviceData.getOrientation());
        assertNotNull(deviceData.getBatteryLevel());

        // These values are default when running on the emulator
        assertTrue(deviceData.isCharging());
        assertEquals(deviceData.getLocationStatus(), "allowed");
        assertEquals(deviceData.getNetworkAccess(), "cellular");
    }
}
