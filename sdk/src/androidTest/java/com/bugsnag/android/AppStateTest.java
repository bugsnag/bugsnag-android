package com.bugsnag.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AppStateTest {

    private AppState appState;

    @Before
    public void setUp() throws Exception {
        appState = new AppState(InstrumentationRegistry.getContext());
    }

    @Test
    public void testSaneValues() throws JSONException, IOException {
        JSONObject appStateJson = streamableToJson(appState);

        assertTrue(appStateJson.getLong("memoryUsage") > 0);
        assertNotNull(appStateJson.getBoolean("lowMemory"));
        assertTrue(appStateJson.getLong("duration") >= 0);
    }
}
