package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static com.bugsnag.android.BugsnagTestUtils.getSharedPrefs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private User user;

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        clearSharedPrefs();
        config = generateConfiguration();
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

    @Test(expected = IllegalArgumentException.class)
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

        config.setPersistUser(true);
        config.setDelivery(BugsnagTestUtils.generateDelivery());
        client = new Client(context, config);

        client.addOnError(new OnErrorCallback() {
            @Override
            public boolean onError(@NonNull Event event) {
                // Pull out the user information
                ClientTest.this.user = event.getUser();
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
        config.setPersistUser(true);
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
        config.setPersistUser(false);
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
    public void testMaxBreadcrumbs() {
        Configuration config = generateConfiguration();
        List<BreadcrumbType> breadcrumbTypes
                = Arrays.asList(BreadcrumbType.MANUAL, BreadcrumbType.STATE);
        config.setEnabledBreadcrumbTypes(new HashSet<>(breadcrumbTypes));
        config.setMaxBreadcrumbs(2);
        client = generateClient(config);
        assertEquals(1, client.breadcrumbState.getStore().size());

        client.leaveBreadcrumb("test");
        client.leaveBreadcrumb("another");
        assertEquals(2, client.breadcrumbState.getStore().size());

        Breadcrumb poll = client.breadcrumbState.getStore().poll();
        assertEquals(BreadcrumbType.MANUAL, poll.getType());
        assertEquals("manual", poll.getMessage());
        assertEquals("test", poll.getMetadata().get("message"));
    }

    @Test
    public void testClientAddToTab() {
        client = generateClient();
        client.addMetadata("drink", "cola", "cherry");
        assertNotNull(client.getMetadata("drink"));
    }

    @Test
    public void testClientClearTab() {
        client = generateClient();
        client.addMetadata("drink", "cola", "cherry");

        client.clearMetadata("drink");
        assertNull(client.getMetadata("drink"));
    }

    @Test
    public void testClientUser() {
        client = generateClient();
        assertNotNull(client.getUser());
        assertNotNull(client.getUser().getId());
    }

    @Test
    public void testBreadcrumbGetter() {
        client = generateClient();
        Collection<Breadcrumb> breadcrumbs = client.getBreadcrumbs();

        int breadcrumbCount = breadcrumbs.size();
        client.leaveBreadcrumb("Foo");
        assertEquals(breadcrumbCount, breadcrumbs.size()); // should not pick up new breadcrumbs
    }

    @Test
    public void testBreadcrumbStoreNotModified() {
        client = generateClient();
        Collection<Breadcrumb> breadcrumbs = client.getBreadcrumbs();
        int breadcrumbCount = client.breadcrumbState.getStore().size();

        breadcrumbs.clear(); // only the copy should be cleared
        assertTrue(breadcrumbs.isEmpty());
        assertEquals(breadcrumbCount, client.breadcrumbState.getStore().size());
    }

    @Test
    public void testAppDataCollection() {
        client = generateClient();
        AppData appData = client.getAppData();
        assertEquals(client.getAppData(), appData);
    }

    @Test
    public void testAppDataMetadata() {
        client = generateClient();
        Map<String, Object> app = client.getAppData().getAppDataMetadata();
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
        DeviceData deviceData = client.getDeviceData();
        assertEquals(client.getDeviceData(), deviceData);
    }

    @Test
    public void testPopulateDeviceMetadata() {
        client = generateClient();
        Map<String, Object> metadata = client.getDeviceData().getDeviceMetadata();

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

    @Test
    public void testMetadataCloned() {
        config.addMetadata("test_section", "foo", "bar");
        client = new Client(context, config);
        client.addMetadata("test_section", "second", "another value");

        // metadata state should be deep copied
        assertNotSame(config.metadataState, client.metadataState);

        // metadata object should be deep copied
        Metadata configData = config.metadataState.getMetadata();
        Metadata clientData = client.metadataState.getMetadata();
        assertNotSame(configData, clientData);

        // metadata backing map should be deep copied

        // validate configuration metadata
        Map<String, Object> configExpected = Collections.<String, Object>singletonMap("foo", "bar");
        assertEquals(configExpected, config.getMetadata("test_section"));

        // validate client metadata
        Map<String, Object> data = new HashMap<>();
        data.put("foo", "bar");
        data.put("second", "another value");
        assertEquals(data, client.getMetadata("test_section"));
    }
}
