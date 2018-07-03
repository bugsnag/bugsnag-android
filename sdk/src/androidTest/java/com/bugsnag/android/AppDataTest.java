package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateSessionTracker;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.annotation.NonNull;
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

    @Before
    public void setUp() throws Exception {
        config = new Configuration("some-api-key");
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
    }

    @Test
    public void testManifestData() throws JSONException, IOException {
        AppData appData = generateAppData();
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals("com.bugsnag.android.test", appDataJson.get("id"));
        assertEquals("com.bugsnag.android.test", appDataJson.get("packageName"));
        assertEquals("Bugsnag Android Tests", appDataJson.get("name"));
        assertEquals(1, appDataJson.get("versionCode"));
        assertEquals("1.0", appDataJson.get("versionName"));
        assertEquals("1.0", appDataJson.get("version"));
        assertEquals("development", appDataJson.get("releaseStage"));

        assertTrue(appDataJson.getLong("memoryUsage") > 0);
        assertNotNull(appDataJson.getBoolean("lowMemory"));
        assertTrue(appDataJson.getLong("duration") >= 0);
    }

    @Test
    public void testAppVersionOverride() throws JSONException, IOException {
        String appVersion = "1.2.3";
        config.setAppVersion(appVersion);

        AppData appData = generateAppData();
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals(appVersion, appDataJson.get("version"));
    }

    @Test
    public void testReleaseStageOverride() throws JSONException, IOException {
        String releaseStage = "test-stage";
        config.setReleaseStage(releaseStage);

        AppData appData = generateAppData();
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals(releaseStage, appDataJson.get("releaseStage"));
    }

    @NonNull
    private AppData generateAppData() {
        return new AppData(InstrumentationRegistry.getContext(), config, generateSessionTracker());
    }

}
