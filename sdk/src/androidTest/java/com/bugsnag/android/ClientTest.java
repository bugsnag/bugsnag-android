package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.getSharedPrefs;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ClientTest {

    private static final String USER_ID = "123456";
    private static final String USER_EMAIL = "mr.test@email.com";
    private static final String USER_NAME = "Mr Test";

    private Context context;
    private Configuration config;

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getContext();
        clearSharedPrefs();
        config = new Configuration("api-key");
    }

    /**
     * Clears sharedPreferences to remove any values persisted
     * @throws Exception if IO to sharedPrefs failed
     */
    @After
    public void tearDown() throws Exception {
        clearSharedPrefs();
        Async.cancelTasks();
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

    @SuppressWarnings("deprecation")
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
        config.setDelivery(BugsnagTestUtils.generateDelivery());
        Client client = new Client(context, config);

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
        assertEquals(USER_ID, user.getId());
        assertEquals(USER_EMAIL, user.getEmail());
        assertEquals(USER_NAME, user.getName());
    }

    @Test
    public void testStoreUserInPrefs() {
        config.setPersistUserBetweenSessions(true);
        Client client = new Client(context, config);
        client.setUser(USER_ID, USER_EMAIL, USER_NAME);

        // Check that the user was store in prefs
        SharedPreferences sharedPref = getSharedPrefs(context);
        assertEquals(USER_ID, sharedPref.getString("user.id", null));
        assertEquals(USER_EMAIL, sharedPref.getString("user.email", null));
        assertEquals(USER_NAME, sharedPref.getString("user.name", null));
    }

    @Test
    public void testStoreUserInPrefsDisabled() {
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
    public void testClearUser() {
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
    public void testEmptyManifestConfig() {
        Bundle data = new Bundle();
        Configuration protoConfig = new Configuration("api-key");
        Configuration newConfig = Client.populateConfigFromManifest(protoConfig, data);

        assertEquals(config.getApiKey(), newConfig.getApiKey());
        assertEquals(config.getBuildUUID(), newConfig.getBuildUUID());
        assertEquals(config.getAppVersion(), newConfig.getAppVersion());
        assertEquals(config.getReleaseStage(), newConfig.getReleaseStage());
        assertEquals(config.getEndpoint(), newConfig.getEndpoint());
        assertEquals(config.getSessionEndpoint(), newConfig.getSessionEndpoint());
        assertEquals(config.getSendThreads(), newConfig.getSendThreads());
        assertEquals(config.getEnableExceptionHandler(), newConfig.getEnableExceptionHandler());
        assertEquals(config.getPersistUserBetweenSessions(),
            newConfig.getPersistUserBetweenSessions());
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

        Configuration protoConfig = new Configuration("api-key");
        Configuration newConfig = Client.populateConfigFromManifest(protoConfig, data);
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

    @Test
    public void testMaxBreadcrumbs() {
        Client client = generateClient();
        assertEquals(0, client.breadcrumbs.store.size());

        client.setMaxBreadcrumbs(1);

        client.leaveBreadcrumb("test");
        client.leaveBreadcrumb("another");
        assertEquals(1, client.breadcrumbs.store.size());

        Breadcrumb poll = client.breadcrumbs.store.poll();
        assertEquals(BreadcrumbType.MANUAL, poll.getType());
        assertEquals("manual", poll.getName());
        assertEquals("another", poll.getMetadata().get("message"));
    }

    @Test
    public void testClearBreadcrumbs() {
        Client client = generateClient();
        assertEquals(0, client.breadcrumbs.store.size());

        client.leaveBreadcrumb("test");
        assertEquals(1, client.breadcrumbs.store.size());

        client.clearBreadcrumbs();
        assertEquals(0, client.breadcrumbs.store.size());
    }

    @Test
    public void testClientAddToTab() {
        Client client = generateClient();
        client.addToTab("drink", "cola", "cherry");
        assertNotNull(client.getMetaData().getTab("drink"));
    }

    @Test
    public void testClientClearTab() {
        Client client = generateClient();
        client.addToTab("drink", "cola", "cherry");

        client.clearTab("drink");
        assertTrue(client.getMetaData().getTab("drink").isEmpty());
    }

    @SuppressWarnings("deprecation")
    @Test(expected = IllegalArgumentException.class)
    public void testApiClientNullValidation() {
        generateClient().setSessionTrackingApiClient(null);
    }

    @Test
    public void testClientUser() {
        Client client = generateClient();
        assertNotNull(client.getUser());
        assertNotNull(client.getUser().getId());
    }

    @Test
    public void testBreadcrumbGetter() {
        Client client = generateClient();
        Collection<Breadcrumb> breadcrumbs = client.getBreadcrumbs();

        int breadcrumbCount = breadcrumbs.size();
        client.leaveBreadcrumb("Foo");
        assertEquals(breadcrumbCount, breadcrumbs.size()); // should not pick up new breadcrumbs
    }

    @Test
    public void testBreadcrumbStoreNotModified() {
        Client client = generateClient();
        Collection<Breadcrumb> breadcrumbs = client.getBreadcrumbs();
        int breadcrumbCount = client.breadcrumbs.store.size();

        breadcrumbs.clear(); // only the copy should be cleared
        assertTrue(breadcrumbs.isEmpty());
        assertEquals(breadcrumbCount, client.breadcrumbs.store.size());
    }

    @Test
    public void testAppDataCollection() {
        Client client = generateClient();
        AppData appData = client.getAppData();
        assertEquals(client.getAppData(), appData);
    }

    @Test
    public void testAppDataMetaData() {
        Client client = generateClient();
        Map<String, Object> app = client.getAppData().getAppDataMetaData();
        assertEquals(6, app.size());
        assertEquals("Bugsnag Android Tests", app.get("name"));
        assertEquals("com.bugsnag.android.test", app.get("packageName"));
        assertEquals("1.0", app.get("versionName"));
        assertNotNull(app.get("memoryUsage"));
        assertTrue(app.containsKey("activeScreen"));
        assertNotNull(app.get("lowMemory"));
    }

    @Test
    public void testDeviceDataCollection() {
        Client client = generateClient();
        DeviceData deviceData = client.getDeviceData();
        assertEquals(client.getDeviceData(), deviceData);
    }

    @Test
    public void testPopulateDeviceMetadata() {
        Client client = generateClient();
        Map<String, Object> metaData = client.getDeviceData().getDeviceMetaData();

        assertEquals(14, metaData.size());
        assertNotNull(metaData.get("batteryLevel"));
        assertNotNull(metaData.get("charging"));
        assertNotNull(metaData.get("locationStatus"));
        assertNotNull(metaData.get("networkAccess"));
        assertNotNull(metaData.get("time"));
        assertNotNull(metaData.get("brand"));
        assertNotNull(metaData.get("apiLevel"));
        assertNotNull(metaData.get("osBuild"));
        assertNotNull(metaData.get("locale"));
        assertNotNull(metaData.get("screenDensity"));
        assertNotNull(metaData.get("dpi"));
        assertNotNull(metaData.get("emulator"));
        assertNotNull(metaData.get("screenResolution"));
        assertNotNull(metaData.get("cpuAbi"));
    }
}
