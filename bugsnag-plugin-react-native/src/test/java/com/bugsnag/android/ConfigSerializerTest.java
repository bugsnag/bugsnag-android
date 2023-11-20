package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.bugsnag.android.internal.ImmutableConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigSerializerTest {

    private ImmutableConfig config;

    /**
     * Constructs an immutable config object
     */
    @Before
    public void setUp() throws Exception {
        config = TestData.generateConfig();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serialize() {
        ConfigSerializer serializer = new ConfigSerializer();
        Map<String, Object> map = new HashMap<>();
        serializer.serialize(map, config);

        assertEquals("123456abcdeabcde", map.get("apiKey"));
        assertTrue((Boolean) map.get("autoDetectErrors"));
        assertTrue((Boolean) map.get("autoTrackSessions"));
        assertEquals("ALWAYS", map.get("sendThreads"));

        assertEquals(Collections.singleton("production"), map.get("enabledReleaseStages"));
        assertEquals(Collections.singleton("com.example"), map.get("projectPackages"));
        assertEquals("production", map.get("releaseStage"));
        assertEquals("builduuid-123", map.get("buildUuid"));
        assertEquals("1.4.3", map.get("appVersion"));
        assertEquals(55, map.get("versionCode"));
        assertEquals("android", map.get("type"));
        assertTrue((Boolean) map.get("persistUser"));
        assertEquals(55, map.get("launchCrashThresholdMs"));
        assertEquals(22, map.get("maxBreadcrumbs"));
        assertEquals(Collections.singleton("manual"), map.get("enabledBreadcrumbTypes"));

        Map<String, Object> errorTypes = (Map<String, Object>) map.get("enabledErrorTypes");
        assertTrue((Boolean) errorTypes.get("anrs"));
        assertTrue((Boolean) errorTypes.get("ndkCrashes"));
        assertTrue((Boolean) errorTypes.get("unhandledExceptions"));
        assertTrue((Boolean) errorTypes.get("unhandledRejections"));

        Map<String, Object> endpoints = (Map<String, Object>) map.get("endpoints");
        assertEquals("https://notify.bugsnag.com", endpoints.get("notify"));
        assertEquals("https://sessions.bugsnag.com", endpoints.get("sessions"));
    }
}
