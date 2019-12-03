package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.convert;
import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.test.core.app.ApplicationProvider;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class AppDataSummaryTest {

    private Map<String, Object> appData;

    @Mock
    Client client;

    @Mock
    SessionTracker sessionTracker;

    /**
     * Constructs an app data object
     */
    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        Configuration config = generateConfiguration();
        config.setVersionCode(1);
        AppData obj = new AppData(context, packageManager, convert(config),
                sessionTracker, NoopLogger.INSTANCE);
        this.appData = obj.getAppDataSummary();
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
