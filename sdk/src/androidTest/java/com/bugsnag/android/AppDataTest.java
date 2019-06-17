package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppDataTest {

    private Configuration config;
    private Map<String, Object> appData;
    private Client client;

    /**
     * Configures a new AppData for testing accessors + serialisation
     *
     * @throws Exception if setup failed
     */
    @Before
    public void setUp() throws Exception {
        config = new Configuration("some-api-key");
        client = generateClient(config);
        appData = new AppData(client).getAppData();
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testAccessors() {
        assertEquals("com.bugsnag.android.test", appData.get("packageName"));
        assertNull(appData.get("buildUUID"));
        assertTrue(((Long) appData.get("duration")) > 0);
        assertEquals(0L, appData.get("durationInForeground"));
        assertFalse((Boolean) appData.get("inForeground"));
    }

    @Test
    public void testJsonSerialisation() throws JSONException {
        appData.put("buildUUID", "fa54de");
        JSONObject appDataJson = mapToJson(appData);

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
    public void testAppVersionOverride() throws JSONException {
        String appVersion = "1.2.3";
        config.setAppVersion(appVersion);

        JSONObject appDataJson = mapToJson(client.appData.getAppData());
        assertEquals(appVersion, appDataJson.get("version"));
    }

    @Test
    public void testReleaseStageOverride() throws JSONException {
        String releaseStage = "test-stage";
        config.setReleaseStage(releaseStage);

        JSONObject appDataJson = mapToJson(client.appData.getAppData());
        assertEquals(releaseStage, appDataJson.get("releaseStage"));
    }

}
