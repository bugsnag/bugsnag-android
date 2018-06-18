package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateSessionTracker;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppDataTest {

    private Configuration config;
    private AppData appData;
    private Context context;
    private SessionTracker sessionTracker;

    @Before
    public void setUp() throws Exception {
        config = new Configuration("some-api-key");
        context = InstrumentationRegistry.getContext();
        sessionTracker = generateSessionTracker();
        appData = new AppData(context, config, sessionTracker);
    }

    @Test
    public void testPackageName() {
        assertEquals("com.bugsnag.android.test", appData.getPackageName());
        String expected = "com.example.foo";
        appData.setPackageName(expected);
        assertEquals(expected, appData.getPackageName());
    }

    @Test
    public void testBuildUUID() {
        assertNull(appData.getBuildUUID());
        String expected = "fad4902f";
        appData.setBuildUUID(expected);
        assertEquals(expected, appData.getBuildUUID());
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
        appData.setBuildUUID("fa54de");
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
        appData = new AppData(context, config, sessionTracker);

        JSONObject appDataJson = streamableToJson(appData);
        assertEquals(appVersion, appDataJson.get("version"));
    }

    @Test
    public void testReleaseStageOverride() throws JSONException, IOException {
        String releaseStage = "test-stage";
        config.setReleaseStage(releaseStage);
        appData = new AppData(context, config, sessionTracker);

        JSONObject appDataJson = streamableToJson(appData);
        assertEquals(releaseStage, appDataJson.get("releaseStage"));
    }

}
