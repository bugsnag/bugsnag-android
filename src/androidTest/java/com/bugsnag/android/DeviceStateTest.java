package com.bugsnag.android;

public class DeviceStateTest extends BugsnagTestCase {
    public void testSaneValues() {
        DeviceState deviceState = new DeviceState(getContext());

        assertTrue(deviceState.getFreeMemory() > 0);
        assertNotNull(deviceState.getOrientation());
        assertTrue(deviceState.getBatteryLevel() > 0);
        assertTrue(deviceState.isCharging());
        assertEquals("allowed", deviceState.getLocationStatus());
        assertNotNull(deviceState.getNetworkAccess());
    }
}
