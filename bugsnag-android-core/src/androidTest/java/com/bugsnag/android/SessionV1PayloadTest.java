package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateApp;
import static com.bugsnag.android.BugsnagTestUtils.generateDevice;
import static com.bugsnag.android.BugsnagTestUtils.generateSession;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class SessionV1PayloadTest {

    private SessionStore sessionStore;
    private File storageDir;
    private File file;

    /**
     * Configures a session tracking payload and session store, ensuring that 0 files are present
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        ImmutableConfig config = BugsnagTestUtils.generateImmutableConfig();
        sessionStore = new SessionStore(context, config, NoopLogger.INSTANCE, null);

        Assert.assertNotNull(sessionStore.storeDirectory);
        storageDir = new File(sessionStore.storeDirectory);
        FileUtils.clearFilesInDir(storageDir);

        file = new File(storageDir, "150450000000053a27e4e-967c-4e5c-91be-2e86f2eb7cdc.json");
        writeLegacyFile(generateSession());
    }

    void writeLegacyFile(Session session) throws IOException {
        FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
        Writer out = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
        JsonStream stream = new JsonStream(out);
        session.serializeSessionInfo(stream);
        out.flush();
    }

    /**
     * Deletes any files in the session store created during the test
     *
     * @throws Exception if the operation fails
     */
    @After
    public void tearDown() {
        FileUtils.clearFilesInDir(storageDir);
    }

    /**
     * Serialises sessions from a file instead
     */
    @Test
    public void testSessionFromFile() throws Exception {
        Session payload = new Session(file, new Notifier(), NoopLogger.INSTANCE);
        payload.setApp(generateApp());
        payload.setDevice(generateDevice());

        JSONObject rootNode = streamableToJson(payload);
        assertNotNull(rootNode);
        assertNotNull(rootNode.getJSONObject("app"));
        assertNotNull(rootNode.getJSONObject("device"));
        assertNotNull(rootNode.getJSONObject("notifier"));

        JSONArray sessions = rootNode.getJSONArray("sessions");
        assertNotNull(sessions);
        assertTrue(file.length() > 0);
        assertEquals(1, sessions.length());

        JSONObject session = sessions.getJSONObject(0);
        assertEquals("test", session.getString("id"));
    }
}
