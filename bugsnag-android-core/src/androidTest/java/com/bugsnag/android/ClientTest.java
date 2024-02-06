package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

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

    private Context context;
    private Configuration config;
    private Client client;

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     */
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        config = generateConfiguration();
    }

    /**
     * Clears sharedPreferences to remove any values persisted
     */
    @After
    public void tearDown() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Test(expected = IllegalStateException.class)
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
    public void testMaxBreadcrumbs() {
        Configuration config = generateConfiguration();
        List<BreadcrumbType> breadcrumbTypes = Arrays.asList(BreadcrumbType.MANUAL);
        config.setEnabledBreadcrumbTypes(new HashSet<>(breadcrumbTypes));
        config.setMaxBreadcrumbs(2);
        client = generateClient(config);
        assertEquals(0, client.breadcrumbState.copy().size());

        client.leaveBreadcrumb("test");
        client.leaveBreadcrumb("another");
        client.leaveBreadcrumb("yet another");
        assertEquals(2, client.breadcrumbState.copy().size());

        Breadcrumb poll = client.breadcrumbState.copy().get(0);
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
        Configuration config = new Configuration("api-key");
        config.setEnabledBreadcrumbTypes(Collections.<BreadcrumbType>emptySet());
        client = generateClient(config);
        client.leaveBreadcrumb("Hello World");
        List<Breadcrumb> breadcrumbs = client.getBreadcrumbs();
        List<Breadcrumb> store = new ArrayList<>(client.breadcrumbState.copy());
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
        assertEquals(1, client.breadcrumbState.copy().size());
        assertEquals("Manual breadcrumb", client.breadcrumbState.copy().get(0).getMessage());
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
        assertEquals(11, app.size());
        assertEquals("Bugsnag Android Tests", app.get("name"));
        assertEquals("com.bugsnag.android.core.test", app.get("processName"));
        assertNotNull(app.get("memoryUsage"));
        assertTrue(app.containsKey("activeScreen"));
        assertTrue(app.containsKey("installerPackage"));
        assertNotNull(app.get("lowMemory"));
        assertNotNull(app.get("memoryTrimLevel"));
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

    /**
     * Verifies that calling notify() concurrently delivers event payloads and
     * does not crash the app.
     */
    @Test
    public void testClientMultiNotify() throws InterruptedException {
        // concurrently call notify()
        client = BugsnagTestUtils.generateClient();
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

    @Test
    public void testFeatureFlagsCloned() {
        config.addFeatureFlag("sample_group", "a");
        client = new Client(context, config);
        client.addFeatureFlag("demo_mode");

        assertNotSame(config.impl.featureFlagState, client.featureFlagState);

        FeatureFlagState configFlags = config.impl.featureFlagState;
        FeatureFlagState clientFlags = client.featureFlagState;
        assertNotSame(configFlags, clientFlags);

        List<FeatureFlag> configExpected = Collections.singletonList(
                new FeatureFlag("sample_group", "a"));
        assertEquals(configExpected, config.impl.featureFlagState.toList());

        List<FeatureFlag> clientExpected = Arrays.asList(
                new FeatureFlag("demo_mode"),
                new FeatureFlag("sample_group", "a")
        );

        List<FeatureFlag> clientActual = client.featureFlagState.toList();
        Collections.sort(clientActual, BugsnagTestUtils.featureFlagComparator());

        assertEquals(clientExpected, clientActual);
    }

    @Test
    public void testMetadataIsMutableInBreadCrumb() {
        Configuration config = new Configuration("api keys");
        config.addOnBreadcrumb(new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                Map<String, Object> metadata = breadcrumb.getMetadata();
                metadata.put("Test", 1);
                metadata.put("Test2", 2);
                metadata.remove("Test");
                metadata.clear();
                metadata.put("Test3", 3);
                return true;
            }
        });
        client = BugsnagTestUtils.generateClient(config);

        List<Breadcrumb> breadcrumbs = client.getBreadcrumbs();
        assertEquals(1, breadcrumbs.size());
        Integer testMetadataValue = (Integer) breadcrumbs.get(0).getMetadata().get("Test3");
        assertEquals(Integer.valueOf(3), testMetadataValue);
    }
}
