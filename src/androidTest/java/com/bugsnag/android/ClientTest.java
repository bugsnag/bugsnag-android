package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import static com.bugsnag.android.Client.MF_APP_VERSION;
import static com.bugsnag.android.Client.MF_BUILD_UUID;
import static com.bugsnag.android.Client.MF_ENABLE_EXCEPTION_HANDLER;
import static com.bugsnag.android.Client.MF_ENDPOINT;
import static com.bugsnag.android.Client.MF_PERSIST_USER_BETWEEN_SESSIONS;
import static com.bugsnag.android.Client.MF_RELEASE_STAGE;
import static com.bugsnag.android.Client.MF_SEND_THREADS;

public class ClientTest extends BugsnagTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Make sure no user is stored
        SharedPreferences sharedPref = getSharedPrefs();
        sharedPref.edit()
            .remove("user.id")
            .remove("user.email")
            .remove("user.name")
            .commit();
    }


    public void testNullContext() {
        try {
            Client client = new Client(null, "api-key");
            fail("Should throw for null Contexts");
        } catch(NullPointerException e) { }
    }

    public void testNotify() {
        // Notify should not crash
        Client client = new Client(getContext(), "api-key");
        client.notify(new RuntimeException("Testing"));
    }

    public void testConfig() {
        Configuration config = new Configuration("api-key");
        config.setEndpoint("new-endpoint");

        Client client = new Client(getContext(), config);

        // Notify should not crash
        client.notify(new RuntimeException("Testing"));
    }

    public void testRestoreUserFromPrefs() {

        // Set a user in prefs
        SharedPreferences sharedPref = getSharedPrefs();
        sharedPref.edit()
            .putString("user.id", "123456")
            .putString("user.email", "mr.test@email.com")
            .putString("user.name", "Mr Test")
            .commit();

        Configuration config = new Configuration("api-key");
        config.setPersistUserBetweenSessions(true);
        Client client = new Client(getContext(), config);
        final User user = new User();

        client.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                // Pull out the user information
                user.setId(error.getUser().getId());
                user.setEmail(error.getUser().getEmail());
                user.setName(error.getUser().getName());
                return true;
            }
        });

        client.notify(new RuntimeException("Testing"));

        // Check the user details have been set
        assertEquals("123456", user.getId());
        assertEquals("mr.test@email.com", user.getEmail());
        assertEquals("Mr Test", user.getName());
    }

    public void testStoreUserInPrefs() {
        Configuration config = new Configuration("api-key");
        config.setPersistUserBetweenSessions(true);
        Client client = new Client(getContext(), config);
        client.setUser("123456", "mr.test@email.com", "Mr Test");

        // Check that the user was store in prefs
        SharedPreferences sharedPref = getSharedPrefs();
        assertEquals("123456", sharedPref.getString("user.id", null));
        assertEquals("mr.test@email.com", sharedPref.getString("user.email", null));
        assertEquals("Mr Test", sharedPref.getString("user.name", null));
    }

    public void testStoreUserInPrefsDisabled() {
        Configuration config = new Configuration("api-key");
        config.setPersistUserBetweenSessions(false);
        Client client = new Client(getContext(), config);
        client.setUser("123456", "mr.test@email.com", "Mr Test");

        // Check that the user was not stored in prefs
        SharedPreferences sharedPref = getSharedPrefs();
        assertFalse(sharedPref.contains("user.id"));
        assertFalse(sharedPref.contains("user.email"));
        assertFalse(sharedPref.contains("user.name"));
    }

    public void testClearUser() {

        // Set a user in prefs
        SharedPreferences sharedPref = getSharedPrefs();
        sharedPref.edit()
            .putString("user.id", "123456")
            .putString("user.email", "mr.test@email.com")
            .putString("user.name", "Mr Test")
            .commit();

        // Clear the user using the command
        Client client = new Client(getContext(), "api-key");
        client.clearUser();

        // Check that there is no user information in the prefs anymore
        sharedPref = getSharedPrefs();
        assertFalse(sharedPref.contains("user.id"));
        assertFalse(sharedPref.contains("user.email"));
        assertFalse(sharedPref.contains("user.name"));
    }

    public void testEmptyManifestConfig() {
        Configuration config = new Configuration("api-key");
        Bundle data = new Bundle();
        Configuration newConfig = Client.populateConfigFromManifest(new Configuration("api-key"), data);

        assertEquals(config.getBuildUUID(), newConfig.getBuildUUID());
        assertEquals(config.getAppVersion(), newConfig.getAppVersion());
        assertEquals(config.getReleaseStage(), newConfig.getReleaseStage());
        assertEquals(config.getEndpoint(), newConfig.getEndpoint());
        assertEquals(config.getSendThreads(), newConfig.getSendThreads());
        assertEquals(config.getEnableExceptionHandler(), newConfig.getEnableExceptionHandler());
        assertEquals(config.getPersistUserBetweenSessions(), newConfig.getPersistUserBetweenSessions());
    }

    public void testFullManifestConfig() {
        String buildUuid = "123";
        String appVersion = "v1.0";
        String releaseStage = "debug";
        String endpoint = "http://example.com";

        Bundle data = new Bundle();
        data.putString(MF_BUILD_UUID, buildUuid);
        data.putString(MF_APP_VERSION, appVersion);
        data.putString(MF_RELEASE_STAGE, releaseStage);
        data.putString(MF_ENDPOINT, endpoint);
        data.putBoolean(MF_SEND_THREADS, false);
        data.putBoolean(MF_ENABLE_EXCEPTION_HANDLER, false);
        data.putBoolean(MF_PERSIST_USER_BETWEEN_SESSIONS, true);

        Configuration newConfig = Client.populateConfigFromManifest(new Configuration("api-key"), data);
        assertEquals(buildUuid, newConfig.getBuildUUID());
        assertEquals(appVersion, newConfig.getAppVersion());
        assertEquals(releaseStage, newConfig.getReleaseStage());
        assertEquals(endpoint, newConfig.getEndpoint());
        assertEquals(false, newConfig.getSendThreads());
        assertEquals(false, newConfig.getEnableExceptionHandler());
        assertEquals(true, newConfig.getPersistUserBetweenSessions());
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        // Make sure no user is stored
        SharedPreferences sharedPref = getSharedPrefs();
        sharedPref.edit()
            .remove("user.id")
            .remove("user.email")
            .remove("user.name")
            .commit();
    }
}
