package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static com.bugsnag.android.BugsnagTestUtils.getSharedPrefs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        assertFalse(sharedPref.contains("user.id"));
        assertFalse(sharedPref.contains("user.email"));
        assertFalse(sharedPref.contains("user.name"));
    }

    @Test
    public void testMaxBreadcrumbs() {
        Configuration config = generateConfiguration();
        List<BreadcrumbType> breadcrumbTypes = Arrays.asList(BreadcrumbType.MANUAL);
        config.setEnabledBreadcrumbTypes(new HashSet<>(breadcrumbTypes));
        config.setMaxBreadcrumbs(2);
        client = generateClient(config);
        assertEquals(0, client.breadcrumbState.getStore().size());

        client.leaveBreadcrumb("test");
        client.leaveBreadcrumb("another");
        client.leaveBreadcrumb("yet another");
        assertEquals(2, client.breadcrumbState.getStore().size());

        Breadcrumb poll = client.breadcrumbState.getStore().poll();
        assertEquals(BreadcrumbType.MANUAL, poll.getType());
        assertEquals("another", poll.getMessage());
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
        client.addMetadata("food", "berries", "raspberry");

        client.clearMetadata("drink");
        assertNull(client.getMetadata("drink"));
        assertEquals("raspberry", client.getMetadata("food", "berries"));
    }

    @Test
    public void testClientClearValue() {
        client = generateClient();
        client.addMetadata("drink", "cola", "cherry");
        client.addMetadata("drink", "soda", "cream");

        client.clearMetadata("drink", "cola");
        assertNull(client.getMetadata("drink", "cola"));
        assertEquals("cream", client.getMetadata("drink", "soda"));
    }

    @Test
    public void testClientUser() {
        client = generateClient();
        assertNotNull(client.getUser());
        assertNotNull(client.getUser().getId());
    }

    @Test
    public void testClientBreadcrumbRetrieval() {
        client = generateClient();
        client.leaveBreadcrumb("Hello World");
        List<Breadcrumb> breadcrumbs = client.getBreadcrumbs();
        List<Breadcrumb> store = new ArrayList<>(client.breadcrumbState.getStore());
        assertEquals(store, breadcrumbs);
        assertNotSame(store, breadcrumbs);
    }

    @Test
    public void testBreadcrumbGetter() {
        client = generateClient();
        List<Breadcrumb> breadcrumbs = client.getBreadcrumbs();

        int breadcrumbCount = breadcrumbs.size();
        client.leaveBreadcrumb("Foo");
        assertEquals(breadcrumbCount, breadcrumbs.size()); // should not pick up new breadcrumbs
    }

    @Test
    public void testBreadcrumbStoreNotModified() {
        config.setEnabledBreadcrumbTypes(Collections.singleton(BreadcrumbType.MANUAL));
        client = generateClient(config);
        client.leaveBreadcrumb("Manual breadcrumb");
        List<Breadcrumb> breadcrumbs = client.getBreadcrumbs();

        breadcrumbs.clear(); // only the copy should be cleared
        assertTrue(breadcrumbs.isEmpty());
        assertEquals(1, client.breadcrumbState.getStore().size());
        assertEquals("Manual breadcrumb", client.breadcrumbState.getStore().remove().getMessage());
    }

    @Test
    public void testAppDataCollection() {
        client = generateClient();
        AppDataCollector appDataCollector = client.getAppDataCollector();
        assertEquals(client.getAppDataCollector(), appDataCollector);
    }

    @Test
    public void testAppDataMetadata() {
        client = generateClient();
        Map<String, Object> app = client.getAppDataCollector().getAppDataMetadata();
        assertEquals(4, app.size());
        assertEquals("Bugsnag Android Tests", app.get("name"));
        assertNotNull(app.get("memoryUsage"));
        assertTrue(app.containsKey("activeScreen"));
        assertNotNull(app.get("lowMemory"));
    }

    @Test
    public void testDeviceDataCollection() {
        client = generateClient();
        DeviceDataCollector deviceDataCollector = client.getDeviceDataCollector();
        assertEquals(client.getDeviceDataCollector(), deviceDataCollector);
    }

    @Test
    public void testPopulateDeviceMetadata() {
        client = generateClient();
        Map<String, Object> metadata = client.getDeviceDataCollector().getDeviceMetadata();

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
        assertNotSame(config.impl.metadataState, client.metadataState);

        // metadata object should be deep copied
        Metadata configData = config.impl.metadataState.getMetadata();
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

    @Test
    public void testUserCloned() {
        config.setUser("123", "test@example.com", "Tess Derby");
        client = new Client(context, config);
        User user = client.getUser();
        assertEquals("123", user.getId());
        assertEquals("Tess Derby", user.getName());
        assertEquals("test@example.com", user.getEmail());
    }

    @Test
    public void testUserNotCloned() {
        client = new Client(context, config);
        User user = client.getUser();
        assertNotNull(user.getId()); // use an auto-generated-id
        assertNull(user.getName());
        assertNull(user.getEmail());
    }

    @Test
    public void testDeviceIdNotUserId() {
        config.setUser("123", "test@example.com", "Tess Derby");
        client = new Client(context, config);
        assertEquals("123", client.getUser().getId());
        assertNotEquals("123", client.getDeviceDataCollector().generateDevice().getId());
    }

    @Test
    public void testDeviceIdEqualsUserId() {
        client = new Client(context, config);
        String userId = client.getUser().getId();
        String deviceId = client.getDeviceDataCollector().generateDevice().getId();
        assertEquals(userId, deviceId);
    }

    /**
     * Verifies that calling notify() concurrently delivers event payloads and
     * does not crash the app.
     */
    @Test
    public void testClientMultiNotify() throws InterruptedException {
        // concurrently call notify()
        client =  BugsnagTestUtils.generateClient();
        Executor executor = Executors.newSingleThreadExecutor();
        int count = 200;
        final CountDownLatch latch = new CountDownLatch(count);

        for (int k = 0; k < count / 2; k++) {
            client.notify(new RuntimeException("Whoops"));
            latch.countDown();

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    client.notify(new RuntimeException("Oh dear"));
                    latch.countDown();
                }
            });
        }
        // wait for all events to be delivered
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }
}
