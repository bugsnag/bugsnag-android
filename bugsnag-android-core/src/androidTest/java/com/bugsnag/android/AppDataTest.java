package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.mapToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

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
public class AppDataTest {

    private Map<String, Object> appData;

    @Mock
    Client client;

    @Mock
    SessionTracker sessionTracker;

    /**
     * Configures a new AppData for testing accessors + serialisation
     *
     * @throws Exception if setup failed
     */
    @Before
    public void setUp() throws Exception {
        when(sessionTracker.isInForeground()).thenReturn(true);
        when(sessionTracker.getDurationInForegroundMs(anyLong())).thenReturn(500L);

        Context context = ApplicationProvider.getApplicationContext();
        PackageManager packageManager = context.getPackageManager();
        Configuration config = new Configuration("api-key");
        config.setVersionCode(1);
        AppData obj = new AppData(context, packageManager, config, sessionTracker);
        this.appData = obj.getAppData();
    }

    @Test
    public void testAccessors() {
        assertEquals("com.bugsnag.android.core.test", appData.get("packageName"));
        assertNull(appData.get("buildUUID"));
        assertTrue(((Long) appData.get("duration")) >= 0);
        assertEquals(500L, appData.get("durationInForeground"));
        assertTrue((Boolean) appData.get("inForeground"));
    }

    @Test
    public void testJsonSerialisation() throws JSONException {
        appData.put("buildUUID", "fa54de");
        JSONObject appDataJson = mapToJson(appData);

        assertEquals(1, appDataJson.getInt("versionCode"));
        assertEquals("1.0", appDataJson.get("version"));
        assertEquals("development", appDataJson.get("releaseStage"));
        assertEquals("android", appDataJson.get("type"));
        assertEquals("com.bugsnag.android.core.test", appDataJson.get("id"));
        assertNotNull(appDataJson.get("buildUUID"));
        assertTrue(((Long) appData.get("duration")) >= 0);
        assertEquals(500L, appData.get("durationInForeground"));
        assertTrue(appDataJson.getBoolean("inForeground"));
    }
}
