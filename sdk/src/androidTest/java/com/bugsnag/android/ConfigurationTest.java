package com.bugsnag.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ConfigurationTest {

    private Configuration config;

    @Before
    public void setUp() throws Exception {
        config = new Configuration("api-key");
    }

    @Test
    public void testEndpoints() {
        // Default endpoints
        assertEquals("https://notify.bugsnag.com", config.getEndpoint());

        // Setting an endpoint
        String endpoint = "http://localhost:8000";
        config.setEndpoint(endpoint);
        assertEquals(endpoint, config.getEndpoint());
    }

    @Test
    public void testSessionEndpoints() {
        // Default endpoints
        assertEquals("https://sessions.bugsnag.com", config.getSessionEndpoint());

        // Setting an endpoint
        String endpoint = "http://localhost:8000";
        config.setSessionEndpoint(endpoint);
        assertEquals(endpoint, config.getSessionEndpoint());
    }

    @Test
    public void testShouldNotify() {
        // Should notify if notifyReleaseStages is null
        assertTrue(config.shouldNotifyForReleaseStage("development"));

        // Shouldn't notify if notifyReleaseStages is set and releaseStage is null
        config.setNotifyReleaseStages(new String[]{"example"});
        assertFalse(config.shouldNotifyForReleaseStage(null));

        // Shouldn't notify if releaseStage not in notifyReleaseStages
        String releaseStage = "production";
        config.setNotifyReleaseStages(new String[]{releaseStage});
        assertFalse(config.shouldNotifyForReleaseStage("not-production"));

        // Should notify if releaseStage in notifyReleaseStages
        config.setNotifyReleaseStages(new String[]{releaseStage});
        assertTrue(config.shouldNotifyForReleaseStage(releaseStage));
    }

    @Test
    public void testShouldIgnore() {
        // Should not ignore by default
        String className = "java.io.IOException";
        assertFalse(config.shouldIgnoreClass(className));

        // Should ignore when added to ignoreClasses
        config.setIgnoreClasses(new String[]{className});
        assertTrue(config.shouldIgnoreClass(className));
    }

    @Test
    public void testInProject() {
        // Shouldn't be inProject if projectPackages hasn't been set
        assertFalse(config.inProject("com.bugsnag.android.Example"));

        // Should be inProject if class in projectPackages
        config.setProjectPackages(new String[]{"com.bugsnag.android"});
        assertTrue(config.inProject("com.bugsnag.android.Example"));

        // Shouldn't be inProject if class not in projectPackages
        config.setProjectPackages(new String[]{"com.bugsnag.android"});
        assertFalse(config.inProject("java.io.IOException"));

        // Should be inProject if class is in projectPackages with null element
        config.setProjectPackages(new String[]{null, "java.io.IOException"});
        assertTrue(config.inProject("java.io.IOException"));
    }

    @Test
    public void testLaunchThreshold() throws Exception {
        assertEquals(5000L, config.getLaunchCrashThresholdMs());

        config.setLaunchCrashThresholdMs(-5);
        assertEquals(0, config.getLaunchCrashThresholdMs());

        int expected = 1500;
        config.setLaunchCrashThresholdMs(expected);
        assertEquals(expected, config.getLaunchCrashThresholdMs());
    }

    @Test
    public void testDefaults() throws Exception {
        assertFalse(config.shouldAutoCaptureSessions());
    }

    @Test
    public void testErrorApiHeaders() throws Exception {
        Map<String, String> headers = config.getErrorApiHeaders();
        assertEquals(config.getApiKey(), headers.get("Bugsnag-Api-Key"));
        assertNotNull(headers.get("Bugsnag-Sent-At"));
        assertNotNull(headers.get("Bugsnag-Payload-Version"));
    }

    @Test
    public void testSessionApiHeaders() throws Exception {
        Map<String, String> headers = config.getSessionApiHeaders();
        assertEquals(config.getApiKey(), headers.get("Bugsnag-Api-Key"));
        assertNotNull(headers.get("Bugsnag-Sent-At"));
        assertNotNull(headers.get("Bugsnag-Payload-Version"));
    }

    @Test
    public void testOverrideContext() throws Exception {
        config.setContext("LevelOne");
        assertEquals("LevelOne", config.getContext());
    }

    @Test
    public void testOverrideFilters() throws Exception {
        config.setFilters(new String[]{"Foo"});
        assertArrayEquals(new String[]{"Foo"}, config.getFilters());
    }

    @Test
    public void testOverrideIgnoreClasses() throws Exception {
        config.setIgnoreClasses(new String[]{"Bar"});
        assertArrayEquals(new String[]{"Bar"}, config.getIgnoreClasses());
    }

    @Test
    public void testOverrideNotifyReleaseStages() throws Exception {
        config.setNotifyReleaseStages(new String[]{"Test"});
        assertArrayEquals(new String[]{"Test"}, config.getNotifyReleaseStages());
    }

    @Test
    public void testOverrideNotifierType() throws Exception {
        config.setNotifierType("React Native");
        assertEquals("React Native", config.getNotifierType());
    }

    @Test
    public void testOverrideCodeBundleId() throws Exception {
        config.setCodeBundleId("abc123");
        assertEquals("abc123", config.getCodeBundleId());
    }
}
