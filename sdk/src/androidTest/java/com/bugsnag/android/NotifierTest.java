package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
public class NotifierTest {

    private Notifier notifier;

    @Before
    public void setUp() throws Exception {
        notifier = new Notifier();
    }

    @Test
    public void testName() {
        assertEquals("Android Bugsnag Notifier", notifier.getName());
        String expected = "CrossPlatformFramework";
        notifier.setName(expected);
        assertEquals(expected, notifier.getName());
    }

    @Test
    public void testVersion() {
        assertNotNull(notifier.getVersion());
        String expected = "1.2.3";
        notifier.setVersion(expected);
        assertEquals(expected, notifier.getVersion());
    }

    @Test
    public void testUrl() {
        assertEquals("https://bugsnag.com", notifier.getURL());
        String expected = "http://example.com";
        notifier.setURL(expected);
        assertEquals(expected, notifier.getURL());
    }

    @Test
    public void testJsonSerialisation() throws JSONException, IOException {
        JSONObject notifierJson = streamableToJson(notifier);
        assertEquals(3, notifierJson.length());
        assertEquals("Android Bugsnag Notifier", notifierJson.getString("name"));
        assertNotNull(notifierJson.getString("version"));
        assertEquals("https://bugsnag.com", notifierJson.getString("url"));
    }
}
