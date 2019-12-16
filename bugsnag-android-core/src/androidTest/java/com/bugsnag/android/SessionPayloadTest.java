package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
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

public class SessionPayloadTest {

    private JSONObject rootNode;
    private Session session;
    private AppDataCollector appDataCollector;

    private SessionStore sessionStore;
    private File storageDir;
    private SessionPayload payload;
    private DeviceDataCollector deviceDataCollector;
    private Client client;

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
        client = generateClient();
        payload = generatePayloadFromSession(context, session);
        rootNode = streamableToJson(payload);
    }

    private SessionPayload generatePayloadFromSession(Context context,
                                                      Session session) {
        appDataCollector = client.getAppDataCollector();
        deviceDataCollector = client.deviceDataCollector;
        return new SessionPayload(session, null, appDataCollector.generateApp(),
                deviceDataCollector.generateDevice());
    }

    /**
     * Deletes any files in the session store created during the test
     *
     * @throws Exception if the operation fails
     */
    @After
    public void tearDown() {
        FileUtils.clearFilesInDir(storageDir);
        client.close();
    }

    /**
     * Serialises sessions from a file instead
     */
    @Test
    public void testMultipleSessionFiles() throws Exception {
        sessionStore.write(session);
        sessionStore.write(generateSession());
        List<File> storedFiles = sessionStore.findStoredFiles();

        SessionPayload payload = new SessionPayload(null,
            storedFiles, appDataCollector.generateApp(), deviceDataCollector.generateDevice());
        rootNode = streamableToJson(payload);

        assertNotNull(rootNode);
        JSONArray sessions = rootNode.getJSONArray("sessions");
        assertNotNull(sessions);
        assertEquals(2, sessions.length());
    }

    @Test
    public void testAutoCapturedOverride() throws Exception {
        session = new Session("id", new Date(), null, false);
        Context context = ApplicationProvider.getApplicationContext();
        payload = generatePayloadFromSession(context, session);
        assertFalse(session.isAutoCaptured());
        session.setAutoCaptured(true);
        assertTrue(session.isAutoCaptured());

        JSONObject rootNode = streamableToJson(payload);
        JSONObject sessionNode = rootNode.getJSONArray("sessions").getJSONObject(0);
        assertFalse(sessionNode.has("user"));
    }
}
