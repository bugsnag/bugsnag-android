package com.bugsnag.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class AppDataTest extends BugsnagTestCase {

    @Test
    public void testManifestData() throws JSONException, IOException {
        Configuration config = new Configuration("some-api-key");
        AppData appData = new AppData(InstrumentationRegistry.getContext(), config);
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals("com.bugsnag.android.test", appDataJson.get("id"));
        assertEquals("com.bugsnag.android.test", appDataJson.get("packageName"));
        assertEquals("Bugsnag Android Tests", appDataJson.get("name"));
        assertEquals(Integer.valueOf(1), appDataJson.get("versionCode"));
        assertEquals("1.0", appDataJson.get("versionName"));
        assertEquals("1.0", appDataJson.get("version"));
        assertEquals("development", appDataJson.get("releaseStage"));
    }

    @Test
    public void testAppVersionOverride() throws JSONException, IOException {
        Configuration config = new Configuration("some-api-key");
        config.setAppVersion("1.2.3");

        AppData appData = new AppData(InstrumentationRegistry.getContext(), config);
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals("1.2.3", appDataJson.get("version"));
    }

    @Test
    public void testReleaseStageOverride() throws JSONException, IOException {
        Configuration config = new Configuration("some-api-key");
        config.setReleaseStage("test-stage");

        AppData appData = new AppData(InstrumentationRegistry.getContext(), config);
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals("test-stage", appDataJson.get("releaseStage"));
    }
}
