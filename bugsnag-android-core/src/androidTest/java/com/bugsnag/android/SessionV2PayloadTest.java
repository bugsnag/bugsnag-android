package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateApp;
import static com.bugsnag.android.BugsnagTestUtils.generateDevice;
import static com.bugsnag.android.BugsnagTestUtils.generateSession;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Date;
import java.util.List;

public class SessionV2PayloadTest {

    private Session session;

    /**
     * Configures a session tracking payload and session store, ensuring that 0 files are present
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        session = generateSession();
        session.setApp(generateApp());
        session.setDevice(generateDevice());
    }

    /**
     * Serialises sessions from a file instead
     */
    @Test
    public void testSessionFromFile() throws Exception {
        // write file to disk
        File file = File.createTempFile("150450000000053a27e4e-967c-4e5c-91be-2e86f2eb7cdc_v2",
                "json");
        FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
        Writer out = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
        JsonStream stream = new JsonStream(out);
        session.toStream(stream);
        out.flush();

        Session payload = new Session(file, new Notifier(), NoopLogger.INSTANCE);
        JSONObject obj = BugsnagTestUtils.streamableToJson(payload);
        JSONObject rootNode = obj.getJSONArray("sessions").getJSONObject(0);
        assertNotNull(rootNode);

        assertNotNull(rootNode.getJSONObject("app"));
        assertNotNull(rootNode.getJSONObject("device"));
        assertNotNull(rootNode.getJSONObject("notifier"));

        JSONArray sessions = rootNode.getJSONArray("sessions");
        assertNotNull(sessions);
        assertEquals(1, sessions.length());

        JSONObject session = sessions.getJSONObject(0);
        assertEquals("test", session.getString("id"));
    }

    @Test
    public void testAutoCapturedOverride() throws Exception {
        session = new Session("id", new Date(), null, false, new Notifier(), NoopLogger.INSTANCE);
        assertFalse(session.isAutoCaptured());
        session.setAutoCaptured(true);
        assertTrue(session.isAutoCaptured());

        JSONObject rootNode = BugsnagTestUtils.streamableToJson(session);
        JSONObject sessionNode = rootNode.getJSONArray("sessions").getJSONObject(0);
        assertFalse(sessionNode.has("user"));
    }
}
