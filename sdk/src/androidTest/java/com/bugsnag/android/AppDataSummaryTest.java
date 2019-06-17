package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
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

import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppDataSummaryTest {

    private Map<String, Object> appData;

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = generateClient();
        appData = new AppData(client).getAppDataSummary();
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testAccessors() {
        assertEquals(1, appData.get("versionCode"));
        assertEquals("1.0", appData.get("version"));
        assertEquals("development", appData.get("releaseStage"));
        assertEquals("android", appData.get("type"));
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
