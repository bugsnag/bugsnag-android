package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateApp;
import static com.bugsnag.android.BugsnagTestUtils.generateDevice;
import static com.bugsnag.android.BugsnagTestUtils.generateSession;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

import java.io.File;
import java.util.Date;
import java.util.List;

public class SessionV2PayloadTest {

    private Session session;

    private SessionStore sessionStore;
    private File storageDir;

    /**
     * Configures a session tracking payload and session store, ensuring that 0 files are present
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        sessionStore = new SessionStore(context, NoopLogger.INSTANCE, null);

        Assert.assertNotNull(sessionStore.storeDirectory);
        storageDir = new File(sessionStore.storeDirectory);
        FileUtils.clearFilesInDir(storageDir);
        session = generateSession();
        session.setApp(generateApp());
        session.setDevice(generateDevice());
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
        sessionStore.write(session);
        List<File> storedFiles = sessionStore.findStoredFiles();
        Session payload = new Session(storedFiles.get(0), new Notifier(), NoopLogger.INSTANCE);
        JSONObject rootNode = streamableToJson(payload);
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

        JSONObject rootNode = streamableToJson(session);
        JSONObject sessionNode = rootNode.getJSONArray("sessions").getJSONObject(0);
        assertFalse(sessionNode.has("user"));
    }
}
