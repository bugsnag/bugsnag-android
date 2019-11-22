package com.bugsnag.android;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class DeviceDataTest {

    private Map<String, Object> deviceData;

    /**
     * Generates a device data object
     */
    @Before
    public void setUp() {
        Connectivity connectivity = BugsnagTestUtils.generateConnectivity();
        Context context = ApplicationProvider.getApplicationContext();
        Resources resources = context.getResources();
        DeviceData deviceData = new DeviceData(connectivity, context, resources,
                "123", DeviceBuildInfo.Companion.defaultInfo());
        this.deviceData = deviceData.getDeviceData();
    }

    @Test
    public void testAccessors() {
        assertNotNull(deviceData.get("id"));
        assertNotNull(deviceData.get("orientation"));
        assertTrue((Long) deviceData.get("freeMemory") > 0);
        assertTrue((Long) deviceData.get("totalMemory") > 0);
    }

}
