package com.bugsnag.android;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class AppDataTest extends BugsnagTestCase {
    public void testManifestData() throws JSONException, IOException {
        Configuration config = new Configuration("some-api-key");
        AppData appData = new AppData(getContext(), config);
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals("com.bugsnag.android.test", appDataJson.get("id"));
        assertEquals("com.bugsnag.android.test", appDataJson.get("packageName"));
        assertEquals("Bugsnag Android Tests", appDataJson.get("name"));
        assertEquals(Integer.valueOf(1), appDataJson.get("versionCode"));
        assertEquals("1.0", appDataJson.get("versionName"));
        assertEquals("1.0", appDataJson.get("version"));
        assertEquals("development", appDataJson.get("releaseStage"));
    }

    public void testAppVersionOverride() throws JSONException, IOException {
        Configuration config = new Configuration("some-api-key");
        config.setAppVersion("1.2.3");

        AppData appData = new AppData(getContext(), config);
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals("1.2.3", appDataJson.get("version"));
    }

    public void testReleaseStageOverride() throws JSONException, IOException {
        Configuration config = new Configuration("some-api-key");
        config.setReleaseStage("test-stage");

        AppData appData = new AppData(getContext(), config);
        JSONObject appDataJson = streamableToJson(appData);

        assertEquals("test-stage", appDataJson.get("releaseStage"));
    }
}
