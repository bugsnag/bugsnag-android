package com.bugsnag.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.bugsnag.android.BugsnagTestUtils.getSharedPrefs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ClientTest {

    private static final String USER_ID = "123456";
    private static final String USER_EMAIL = "mr.test@email.com";
    private static final String USER_NAME = "Mr Test";

    private Context context;
    private Configuration config;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getContext();
        clearSharedPrefs();
        config = new Configuration("api-key");
    }

    @After
    public void tearDown() throws Exception {
        clearSharedPrefs();
    }

    private void clearSharedPrefs() {
        // Make sure no user is stored
        SharedPreferences sharedPref = getSharedPrefs(context);
        sharedPref.edit()
            .remove("user.id")
            .remove("user.email")
            .remove("user.name")
            .commit();
    }

    private SharedPreferences setUserPrefs() {
        // Set a user in prefs
        SharedPreferences sharedPref = getSharedPrefs(context);
        sharedPref.edit()
            .putString("user.id", USER_ID)
            .putString("user.email", USER_EMAIL)
            .putString("user.name", USER_NAME)
            .commit();
        return sharedPref;
    }

    @Test(expected = NullPointerException.class)
    public void testNullContext() {
        new Client(null, "api-key");
    }

    @Test
    public void testNotify() {
        // Notify should not crash
        Client client = BugsnagTestUtils.generateClient();
        client.notify(new RuntimeException("Testing"));
    }

    @Test
    public void testConfig() {
        config.setEndpoint("new-endpoint");

        Client client = new Client(context, config);
        client.setErrorReportApiClient(BugsnagTestUtils.generateErrorReportApiClient());
        client.setSessionTrackingApiClient(BugsnagTestUtils.generateSessionTrackingApiClient());

        // Notify should not crash
        client.notify(new RuntimeException("Testing"));
    }

    @Test
    public void testRestoreUserFromPrefs() {
        setUserPrefs();

        config.setPersistUserBetweenSessions(true);
        Client client = new Client(context, config);
        client.setErrorReportApiClient(BugsnagTestUtils.generateErrorReportApiClient());
        client.setSessionTrackingApiClient(BugsnagTestUtils.generateSessionTrackingApiClient());

        final User[] user = new User[1];

        client.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                // Pull out the user information
                user[0] = error.getUser();
                return true;
            }
        });

        client.notify(new RuntimeException("Testing"));

        assertThat(user[0], is(not(nullValue())));

        // Check the user details have been set
        assertEquals(USER_ID, user[0].getId());
        assertEquals(USER_EMAIL, user[0].getEmail());
        assertEquals(USER_NAME, user[0].getName());
    }

    @Deprecated
    @Test
    public void testStoreUserInPrefsDeprecated() {
        config.setPersistUserBetweenSessions(true);
        Client client = new Client(context, config);
        //noinspection deprecation
        client.setUser(USER_ID, USER_EMAIL, USER_NAME);

        // Check that the user was store in prefs
        SharedPreferences sharedPref = getSharedPrefs(context);
        assertEquals(USER_ID, sharedPref.getString("user.id", null));
        assertEquals(USER_EMAIL, sharedPref.getString("user.email", null));
        assertEquals(USER_NAME, sharedPref.getString("user.name", null));
    }

    @Test
    public void testStoreUserInPrefs() {
        config.setPersistUserBetweenSessions(true);
        Client client = new Client(context, config);
        client.setUser(new User.Builder().id(USER_ID).email(USER_EMAIL).name(USER_NAME).build());

        // Check that the user was store in prefs
        SharedPreferences sharedPref = getSharedPrefs(context);
        assertEquals(USER_ID, sharedPref.getString("user.id", null));
        assertEquals(USER_EMAIL, sharedPref.getString("user.email", null));
        assertEquals(USER_NAME, sharedPref.getString("user.name", null));
    }

    @Deprecated
    @Test
    public void testStoreUserInPrefsDisabledDeprecated() {
        config.setPersistUserBetweenSessions(false);
        Client client = new Client(context, config);
        client.setUser(USER_ID, USER_EMAIL, USER_NAME);

        // Check that the user was not stored in prefs
        SharedPreferences sharedPref = getSharedPrefs(context);
        assertFalse(sharedPref.contains("user.id"));
        assertFalse(sharedPref.contains("user.email"));
        assertFalse(sharedPref.contains("user.name"));
    }

    @Test
    public void testStoreUserInPrefsDisabled() {
        config.setPersistUserBetweenSessions(false);
        Client client = new Client(context, config);
        client.setUser(new User.Builder().id(USER_ID).email(USER_EMAIL).name(USER_NAME).build());

        // Check that the user was not stored in prefs
        SharedPreferences sharedPref = getSharedPrefs(context);
        assertFalse(sharedPref.contains("user.id"));
        assertFalse(sharedPref.contains("user.email"));
        assertFalse(sharedPref.contains("user.name"));
    }

    @Deprecated
    @Test
    public void testClearUserDeprecated() {
        // Set a user in prefs
        setUserPrefs();

        // Clear the user using the command
        Client client = new Client(context, "api-key");
        client.clearUser();

        // Check that there is no user information in the prefs anymore
        SharedPreferences sharedPref = getSharedPrefs(context);
        assertFalse(sharedPref.contains("user.id"));
        assertFalse(sharedPref.contains("user.email"));
        assertFalse(sharedPref.contains("user.name"));
    }

    @Test
    public void testClearUser() {
        // Set a user in prefs
        setUserPrefs();

        // Clear the user using the command
        Client client = new Client(context, "api-key");
        client.setUser(null);

        // Check that there is no user information in the prefs anymore
        SharedPreferences sharedPref = getSharedPrefs(context);
        assertFalse(sharedPref.contains("user.id"));
        assertFalse(sharedPref.contains("user.email"));
        assertFalse(sharedPref.contains("user.name"));
    }

    @Test
    public void testSetGetUser() {
        User user = new User.Builder().build();
        Client client = new Client(context, "api-key");
        client.setUser(user);
        assertThat(client.getUser(), is(user));
    }

    @Test
    public void testEmptyManifestConfig() {
        Bundle data = new Bundle();
        Configuration newConfig = Client.populateConfigFromManifest(new Configuration("api-key"), data);

        assertEquals(config.getApiKey(), newConfig.getApiKey());
        assertEquals(config.getBuildUUID(), newConfig.getBuildUUID());
        assertEquals(config.getAppVersion(), newConfig.getAppVersion());
        assertEquals(config.getReleaseStage(), newConfig.getReleaseStage());
        assertEquals(config.getEndpoint(), newConfig.getEndpoint());
        assertEquals(config.getSessionEndpoint(), newConfig.getSessionEndpoint());
        assertEquals(config.getSendThreads(), newConfig.getSendThreads());
        assertEquals(config.getEnableExceptionHandler(), newConfig.getEnableExceptionHandler());
        assertEquals(config.getPersistUserBetweenSessions(), newConfig.getPersistUserBetweenSessions());
    }

    @Test
    public void testFullManifestConfig() {
        String buildUuid = "123";
        String appVersion = "v1.0";
        String releaseStage = "debug";
        String endpoint = "http://example.com";
        String sessionEndpoint = "http://session-example.com";

        Bundle data = new Bundle();
        data.putString("com.bugsnag.android.BUILD_UUID", buildUuid);
        data.putString("com.bugsnag.android.APP_VERSION", appVersion);
        data.putString("com.bugsnag.android.RELEASE_STAGE", releaseStage);
        data.putString("com.bugsnag.android.SESSIONS_ENDPOINT", sessionEndpoint);
        data.putString("com.bugsnag.android.ENDPOINT", endpoint);
        data.putBoolean("com.bugsnag.android.SEND_THREADS", false);
        data.putBoolean("com.bugsnag.android.ENABLE_EXCEPTION_HANDLER", false);
        data.putBoolean("com.bugsnag.android.PERSIST_USER_BETWEEN_SESSIONS", true);
        data.putBoolean("com.bugsnag.android.AUTO_CAPTURE_SESSIONS", true);

        Configuration newConfig = Client.populateConfigFromManifest(new Configuration("api-key"), data);
        assertEquals(buildUuid, newConfig.getBuildUUID());
        assertEquals(appVersion, newConfig.getAppVersion());
        assertEquals(releaseStage, newConfig.getReleaseStage());
        assertEquals(endpoint, newConfig.getEndpoint());
        assertEquals(sessionEndpoint, newConfig.getSessionEndpoint());
        assertEquals(false, newConfig.getSendThreads());
        assertEquals(false, newConfig.getEnableExceptionHandler());
        assertEquals(true, newConfig.getPersistUserBetweenSessions());
        assertEquals(true, newConfig.shouldAutoCaptureSessions());
    }

}
