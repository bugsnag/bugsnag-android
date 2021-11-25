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

public class SessionTrackingPayloadTest {

    private JSONObject rootNode;
    private Session session;
    private AppData appData;

    private SessionStore sessionStore;
    private File storageDir;
    private SessionTrackingPayload payload;
    private DeviceData deviceData;
    private Client client;

    /**
     * Configures a session tracking payload and session store, ensuring that 0 files are present
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        Configuration config = new Configuration("api-key");
        File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
        config.setPersistenceDirectory(cacheDir);
        sessionStore = new SessionStore(config, null);

        Assert.assertNotNull(sessionStore.storeDirectory);
        storageDir = new File(sessionStore.storeDirectory);
        FileUtils.clearFilesInDir(storageDir);
        session = generateSession();
        client = generateClient();
        payload = generatePayloadFromSession(context, session);
        rootNode = streamableToJson(payload);
    }

    private SessionTrackingPayload generatePayloadFromSession(Context context,
                                                  Session session) throws Exception {
        appData = client.getAppData();
        deviceData = client.deviceData;
        return new SessionTrackingPayload(session, null, appData, deviceData);
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

    @Test
    public void testPayloadSerialisation() throws Exception {
        assertNotNull(rootNode);
        JSONArray sessions = rootNode.getJSONArray("sessions");

        JSONObject sessionNode = sessions.getJSONObject(0);
        assertNotNull(sessionNode);
        assertEquals("test", sessionNode.getString("id"));
        String startedAt = sessionNode.getString("startedAt");
        assertEquals(DateUtils.toIso8601(session.getStartedAt()), startedAt);
        assertNotNull(sessionNode.getJSONObject("user"));

        assertNotNull(rootNode.getJSONObject("notifier"));
        assertNotNull(rootNode.getJSONObject("device"));
        assertNotNull(rootNode.getJSONObject("app"));
    }

    /**
     * Serialises sessions from a file instead
     */
    @Test
    public void testMultipleSessionFiles() throws Exception {
        sessionStore.write(session);
        sessionStore.write(generateSession());
        List<File> storedFiles = sessionStore.findStoredFiles();

        SessionTrackingPayload payload = new SessionTrackingPayload(null,
            storedFiles, appData, deviceData);
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
