package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
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
public class AppDataSummaryTest {

    private AppDataSummary appData;

    /**
     * Configures a new AppDataSummary for testing accessors + serialisation
     *
     * @throws Exception if setup failed
     */
    @Before
    public void setUp() throws Exception {
        Configuration config = new Configuration("some-api-key");
        Context context = InstrumentationRegistry.getContext();
        appData = new AppDataSummary(context, config);
    }

    @Test
    public void testVersionCode() {
        assertEquals(Integer.valueOf(1), appData.getVersionCode());
        int expected = 15;
        appData.setVersionCode(expected);
        assertEquals(Integer.valueOf(expected), appData.getVersionCode());
    }

    @Test
    public void testVersionName() {
        assertEquals("1.0", appData.getVersionName());
        String expected = "1.2.3";
        appData.setVersionName(expected);
        assertEquals(expected, appData.getVersionName());
    }

    @Test
    public void testReleaseStage() {
        assertEquals("development", appData.getReleaseStage());
        String expected = "beta";
        appData.setReleaseStage(expected);
        assertEquals(expected, appData.getReleaseStage());
    }

    @Test
    public void testNotifierType() {
        assertEquals("android", appData.getNotifierType());
        String expected = "custom";
        appData.setNotifierType(expected);
        assertEquals(expected, appData.getNotifierType());
    }

    @Test
    public void testCodeBundleId() {
        assertNull(appData.getCodeBundleId());
        String expected = "123";
        appData.setCodeBundleId(expected);
        assertEquals(expected, appData.getCodeBundleId());
    }

    @Test
    public void testJsonSerialisation() throws IOException, JSONException {
        JSONObject appDataJson = streamableToJson(appData);
        assertEquals(1, appDataJson.getInt("versionCode"));
        assertEquals("1.0", appDataJson.get("version"));
        assertEquals("development", appDataJson.get("releaseStage"));
        assertEquals("android", appDataJson.get("type"));
    }

}
