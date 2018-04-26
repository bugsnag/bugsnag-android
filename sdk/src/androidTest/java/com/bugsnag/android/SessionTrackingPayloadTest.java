package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateSession;
import static com.bugsnag.android.BugsnagTestUtils.generateSessionTracker;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

public class SessionTrackingPayloadTest {

    private JSONObject rootNode;
    private Session session;
    private AppData appData;

    private SessionStore sessionStore;
    private File storageDir;

    /**
     * Configures a session tracking payload and session store, ensuring that 0 files are present
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        Configuration config = new Configuration("api-key");
        sessionStore = new SessionStore(config, context);

        Assert.assertNotNull(sessionStore.storeDirectory);
        storageDir = new File(sessionStore.storeDirectory);
        FileUtils.clearFilesInDir(storageDir);

        session = generateSession();
        appData = new AppData(context, new Configuration("a"), generateSessionTracker());
        SessionTrackingPayload payload = new SessionTrackingPayload(session, appData);
        rootNode = streamableToJson(payload);
    }

    /**
     * Deletes any files in the session store created during the test
     *
     * @throws Exception if the operation fails
     */
    @After
    public void tearDown() throws Exception {
        FileUtils.clearFilesInDir(storageDir);
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

        SessionTrackingPayload payload = new SessionTrackingPayload(storedFiles, appData);
        rootNode = streamableToJson(payload);

        assertNotNull(rootNode);
        JSONArray sessions = rootNode.getJSONArray("sessions");
        assertNotNull(sessions);
        assertEquals(2, sessions.length());
    }

}
