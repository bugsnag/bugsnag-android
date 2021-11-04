package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import com.bugsnag.android.DeviceBuildInfo;
import com.bugsnag.android.DeviceWithState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DeviceSerializerTest {

    private DeviceWithState device;

    /**
     * Generates a DeviceWithState for verifying the serializer
     */
    @Before
    public void setup() {
        Map<String, Object> runtimeVersions = new HashMap<>();
        runtimeVersions.put("androidApiLevel", 27);
        runtimeVersions.put("osBuild", "bulldog");
        device = new DeviceWithState(
                new DeviceBuildInfo(
                        "google",
                        "pixel4",
                        "8.0",
                        27,
                        "bulldog",
                        "bulldog-3.6-android",
                        "test-tag",
                        "google pixel",
                        new String[]{"x86"}
                ),
                true,
                "fa02",
                "yue",
                50923409234L,
                runtimeVersions,
                20923423434L,
                23409662345L,
                "portrait",
                new Date(0)
        );
    }

    @Test
    public void serialize() {
        Map<String, Object> map = new HashMap<>();
        new DeviceSerializer().serialize(map, device);

        assertEquals(Arrays.asList("x86"), map.get("cpuAbi"));
        assertEquals(true, map.get("jailbroken"));
        assertEquals("fa02", map.get("id"));
        assertEquals("yue", map.get("locale"));
        assertEquals("google", map.get("manufacturer"));
        assertEquals("pixel4", map.get("model"));
        assertEquals("android", map.get("osName"));
        assertEquals("8.0", map.get("osVersion"));
        assertEquals(50923409234L, map.get("totalMemory"));
        assertEquals(20923423434L, map.get("freeDisk"));
        assertEquals(23409662345L, map.get("freeMemory"));
        assertEquals("portrait", map.get("orientation"));
        assertEquals("1970-01-01T00:00:00.000Z", map.get("time"));

        Map<String, Object> runtimeVersions = new HashMap<>();
        runtimeVersions.put("androidApiLevel", "27");
        runtimeVersions.put("osBuild", "bulldog");
        assertEquals(runtimeVersions, map.get("runtimeVersions"));

    }
}
