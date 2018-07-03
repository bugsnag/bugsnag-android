package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppDataSummaryTest {

    private Map<String, Object> appData;

    /**
     * Configures a new AppDataSummary for testing accessors + serialisation
     *
     * @throws Exception if setup failed
     */
    @Before
    public void setUp() throws Exception {
        AppData appData = new AppData(generateClient());
        this.appData = appData.getAppDataSummary();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
    }

    @Test
    public void testVersionCode() {
        assertEquals(1, appData.get("versionCode"));
    }

    @Test
    public void testVersionName() {
        assertEquals("1.0", appData.get("version"));
    }

    @Test
    public void testReleaseStage() {
        assertEquals("development", appData.get("releaseStage"));
    }

    @Test
    public void testNotifierType() {
        assertEquals("android", appData.get("type"));
    }

    @Test
    public void testCodeBundleId() {
        assertNull(appData.get("codeBundleId"));
    }

    @Test
    public void testJsonSerialisation() throws JSONException {
        JSONObject appDataJson = mapToJson(appData);
        assertEquals(1, appDataJson.getInt("versionCode"));
        assertEquals("1.0", appDataJson.get("version"));
        assertEquals("development", appDataJson.get("releaseStage"));
        assertEquals("android", appDataJson.get("type"));
    }

}
