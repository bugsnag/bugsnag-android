package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static com.bugsnag.android.BugsnagTestUtils.getSharedPrefs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

@SuppressWarnings("unchecked")
@SmallTest
public class ClientTest {

    private static final String USER_ID = "123456";
    private static final String USER_EMAIL = "mr.test@email.com";
    private static final String USER_NAME = "Mr Test";

    private Context context;
    private Configuration config;
    private Client client;

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        clearSharedPrefs();
        config = new Configuration("api-key");
    }

    /**
     * Clears sharedPreferences to remove any values persisted
     */
    @After
    public void tearDown() {
        clearSharedPrefs();
        if (client != null) {
            client.close();
            client = null;
        }
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

    private void setUserPrefs() {
        // Set a user in prefs
        SharedPreferences sharedPref = getSharedPrefs(context);
        sharedPref.edit()
            .putString("user.id", USER_ID)
            .putString("user.email", USER_EMAIL)
            .putString("user.name", USER_NAME)
            .commit();
    }

    @Test(expected = NullPointerException.class)
    public void testNullContext() {
        client = new Client(null, "api-key");
    }

    @Test
    public void testNotify() {
        // Notify should not crash
        client = BugsnagTestUtils.generateClient();
        client.notify(new RuntimeException("Testing"));
    }

    @Test
    public void testRestoreUserFromPrefs() {
        setUserPrefs();

        config.setPersistUserBetweenSessions(true);
        config.setDelivery(BugsnagTestUtils.generateDelivery());
        client = new Client(context, config);

        final User[] user = new User[1];

        client.addOnError(new OnError() {
            @Override
            public boolean run(@NonNull Event event) {
                // Pull out the user information
                user[0] = event.getUser();
                return true;
            }
        });

        client.notify(new RuntimeException("Testing"));

        // Check the user details have been set
        assertEquals(USER_ID, user[0].getId());
        assertEquals(USER_EMAIL, user[0].getEmail());
        assertEquals(USER_NAME, user[0].getName());
    }

    @Test
    public void testStoreUserInPrefs() {
        config.setPersistUserBetweenSessions(true);
        client = new Client(context, config);
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
        client = new Client(context, config);
        client.setUser(USER_ID, USER_EMAIL, USER_NAME);

        // Check that the user was not stored in prefs
        SharedPreferences sharedPref = getSharedPrefs(context);
        assertNotNull(sharedPref.getString("install.iud", null));
        assertFalse(sharedPref.contains("user.id"));
        assertFalse(sharedPref.contains("user.email"));
        assertFalse(sharedPref.contains("user.name"));
    }

    @Test
    public void testClearUser() {
        // Set a user in prefs
        setUserPrefs();

        // Clear the user using the command
        client = new Client(context, "api-key");
        client.setUser(null, null, null);

        // Check that there is no user information in the prefs anymore
        SharedPreferences sharedPref = getSharedPrefs(context);
        assertNotNull(sharedPref.getString("install.iud", null));
        assertFalse(sharedPref.contains("user.id"));
        assertFalse(sharedPref.contains("user.email"));
        assertFalse(sharedPref.contains("user.name"));
    }

    @SuppressWarnings("deprecation") // test backwards compatibility of client.setMaxBreadcrumbs
    @Test
    public void testMaxBreadcrumbs() {
        Configuration config = generateConfiguration();
        config.setAutoCaptureBreadcrumbs(false);
        config.setMaxBreadcrumbs(2);
        client = generateClient(config);
        assertEquals(1, client.breadcrumbs.store.size());

        client.leaveBreadcrumb("test");
        client.leaveBreadcrumb("another");
        assertEquals(2, client.breadcrumbs.store.size());

        Breadcrumb poll = client.breadcrumbs.store.poll();
        assertEquals(BreadcrumbType.MANUAL, poll.getType());
        assertEquals("manual", poll.getMessage());
        assertEquals("test", poll.getMetadata().get("message"));
    }

    @Test
    public void testClientAddToTab() {
        client = generateClient();
        client.addMetadata("drink", "cola", "cherry");
        assertNotNull(client.getMetadata("drink", null));
    }

    @Test
    public void testClientClearTab() {
        client = generateClient();
        client.addMetadata("drink", "cola", "cherry");

        client.clearMetadata("drink", null);
        assertNull(client.getMetadata("drink", null));
    }

    @Test
    public void testClientUser() {
        client = generateClient();
        assertNotNull(client.getUser());
        assertNotNull(client.getUser().getId());
    }

    @Test
    public void testAppDataCollection() {
        client = generateClient();
        AppData appData = client.appData;
        assertEquals(client.appData, appData);
    }

    @Test
    public void testAppDataMetadata() {
        client = generateClient();
        Map<String, Object> app = client.appData.getAppDataMetadata();
        assertEquals(6, app.size());
        assertEquals("Bugsnag Android Tests", app.get("name"));
        assertEquals("com.bugsnag.android.core.test", app.get("packageName"));
        assertEquals("1.0", app.get("versionName"));
        assertNotNull(app.get("memoryUsage"));
        assertTrue(app.containsKey("activeScreen"));
        assertNotNull(app.get("lowMemory"));
    }

    @Test
    public void testDeviceDataCollection() {
        client = generateClient();
        DeviceData deviceData = client.deviceData;
        assertEquals(client.deviceData, deviceData);
    }

    @Test
    public void testPopulateDeviceMetadata() {
        client = generateClient();
        Map<String, Object> metadata = client.deviceData.getDeviceMetadata();

        assertEquals(9, metadata.size());
        assertNotNull(metadata.get("batteryLevel"));
        assertNotNull(metadata.get("charging"));
        assertNotNull(metadata.get("locationStatus"));
        assertNotNull(metadata.get("networkAccess"));
        assertNotNull(metadata.get("brand"));
        assertNotNull(metadata.get("screenDensity"));
        assertNotNull(metadata.get("dpi"));
        assertNotNull(metadata.get("emulator"));
        assertNotNull(metadata.get("screenResolution"));
    }
}
