package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.test.filters.SmallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

@SmallTest
public class NotifierSerializationTest {

    private Notifier notifier;

    @Before
    public void setUp() throws Exception {
        notifier = new Notifier();
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
