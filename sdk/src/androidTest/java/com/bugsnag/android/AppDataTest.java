package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateSessionTracker;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppDataTest {

    private Configuration config;
    private AppData appData;
    private AppDataCollector appDataCollector;

    /**
     * Configures a new AppData for testing accessors + serialisation
     *
     * @throws Exception if setup failed
     */
    @Before
    public void setUp() throws Exception {
        config = new Configuration("some-api-key");
        Client client = new Client(InstrumentationRegistry.getContext(), config);
        appDataCollector = new AppDataCollector(client);
        appData = appDataCollector.generateAppData();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
    }

    @Test
    public void testPackageName() {
        assertEquals("com.bugsnag.android.test", appData.getPackageName());
        String expected = "com.example.foo";
        appData.setPackageName(expected);
        assertEquals(expected, appData.getPackageName());
    }

    @Test
    public void testBuildUuid() {
        assertNull(appData.getBuildUuid());
        String expected = "fad4902f";
        appData.setBuildUuid(expected);
        assertEquals(expected, appData.getBuildUuid());
    }

    @Test
    public void testDuration() {
        assertTrue(appData.getDuration() > 0);
        long expected = 1500;
        appData.setDuration(expected);
        assertEquals(expected, appData.getDuration());
    }

    @Test
    public void testDurationInForeground() {
        assertEquals(0, appData.getDurationInForeground());
        long expected = 1500;
        appData.setDurationInForeground(expected);
        assertEquals(expected, appData.getDurationInForeground());
    }

    @Test
    public void testInForeground() {
        assertFalse(appData.isInForeground());
        appData.setInForeground(true);
        assertTrue(appData.isInForeground());
    }

    @Test
    public void testJsonSerialisation() throws JSONException, IOException {
        appData.setBuildUuid("fa54de");
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals(1, appDataJson.getInt("versionCode"));
        assertEquals("1.0", appDataJson.get("version"));
        assertEquals("development", appDataJson.get("releaseStage"));
        assertEquals("android", appDataJson.get("type"));
        assertEquals("com.bugsnag.android.test", appDataJson.get("id"));
        assertNotNull(appDataJson.get("buildUUID"));
        assertNotNull(appDataJson.get("duration"));
        assertNotNull(appDataJson.get("durationInForeground"));
        assertFalse(appDataJson.getBoolean("inForeground"));
    }

    @Test
    public void testAppVersionOverride() throws JSONException, IOException {
        String appVersion = "1.2.3";
        config.setAppVersion(appVersion);
        appData = appDataCollector.generateAppData();

        JSONObject appDataJson = streamableToJson(appData);
        assertEquals(appVersion, appDataJson.get("version"));
    }

    @Test
    public void testReleaseStageOverride() throws JSONException, IOException {
        String releaseStage = "test-stage";
        config.setReleaseStage(releaseStage);
        appData = appDataCollector.generateAppData();

        JSONObject appDataJson = streamableToJson(appData);
        assertEquals(releaseStage, appDataJson.get("releaseStage"));
    }

}
