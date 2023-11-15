package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bugsnag.android.internal.ImmutableConfig;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Pattern;

public class ConfigurationTest {

    private Configuration config;

    @Before
    public void setUp() {
        config = generateConfiguration();
    }

    @Test
    public void testEndpoints() {
        String notify = "https://notify.myexample.com";
        String sessions = "https://sessions.myexample.com";
        config.setEndpoints(new EndpointConfiguration(notify, sessions));

        assertEquals(notify, config.getEndpoints().getNotify());
        assertEquals(sessions, config.getEndpoints().getSessions());
    }

    @Test
    public void testShouldNotify() {
        // Should notify if enabledReleaseStages is null
        ImmutableConfig immutableConfig;
        immutableConfig = createConfigWithReleaseStages(config,
                null, "development");
        assertFalse(immutableConfig.shouldDiscardByReleaseStage());

        // Should not notify if enabledReleaseStages is null
        immutableConfig = createConfigWithReleaseStages(config,
                Collections.<String>emptySet(), "development");
        assertTrue(immutableConfig.shouldDiscardByReleaseStage());

        // Shouldn't notify if enabledReleaseStages is set and releaseStage is null
        Set<String> example = Collections.singleton("example");
        immutableConfig = createConfigWithReleaseStages(config, example, null);
        assertTrue(immutableConfig.shouldDiscardByReleaseStage());

        // Shouldn't notify if releaseStage not in enabledReleaseStages
        Set<String> stages = Collections.singleton("production");
        immutableConfig = createConfigWithReleaseStages(config, stages, "not-production");
        assertTrue(immutableConfig.shouldDiscardByReleaseStage());

        // Should notify if releaseStage in enabledReleaseStages
        immutableConfig = createConfigWithReleaseStages(config, stages, "production");
        assertFalse(immutableConfig.shouldDiscardByReleaseStage());
    }

    private ImmutableConfig createConfigWithReleaseStages(Configuration config,
                                                          Set<String> releaseStages,
                                                          String releaseStage) {
        config.setEnabledReleaseStages(releaseStages);
        config.setReleaseStage(releaseStage);
        return BugsnagTestUtils.convert(config);
    }

    @Test
    public void testLaunchThreshold() {
        assertEquals(5000L, config.getLaunchDurationMillis());

        config.setLaunchDurationMillis(-5);
        assertEquals(5000, config.getLaunchDurationMillis());

        int expected = 1500;
        config.setLaunchDurationMillis(expected);
        assertEquals(expected, config.getLaunchDurationMillis());
    }

    @Test
    public void testAutoTrackSessions() {
        assertTrue(config.getAutoTrackSessions());
        config.setAutoTrackSessions(false);
        assertFalse(config.getAutoTrackSessions());
    }

    @Test
    public void testOverrideContext() {
        config.setContext("LevelOne");
        assertEquals("LevelOne", config.getContext());
    }

    @Test
    public void testOverrideRedactKeys() {
        Set<Pattern> redactedKeys = Collections.singleton(Pattern.compile("Foo", Pattern.LITERAL));
        config.setRedactedKeys(redactedKeys);
        assertEquals(redactedKeys, config.getRedactedKeys());
    }

    @Test
    public void testOverrideDiscardClasses() {
        Set<Pattern> discardClass = Collections.singleton(Pattern.compile("Bar", Pattern.LITERAL));
        config.setDiscardClasses(discardClass);
        assertEquals(discardClass, config.getDiscardClasses());
    }

    @Test
    public void testOverrideEnabledReleaseStages() {
        config.setEnabledReleaseStages(Collections.singleton("Test"));
        assertEquals(Collections.singleton("Test"), config.getEnabledReleaseStages());
    }

    @Test
    public void testOverrideAppType() {
        config.setAppType("React Native");
        assertEquals("React Native", config.getAppType());
    }

    @Test
    public void testSetDelivery() {
        Configuration configuration = new Configuration("5d1ec5bd39a74caa1267142706a7fb21");
        assertNull(configuration.getDelivery());
        Delivery delivery = new Delivery() {
            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull Session payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return null;
            }

            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull EventPayload payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return null;
            }
        };
        configuration.setDelivery(delivery);

        assertFalse(configuration.getDelivery() instanceof DefaultDelivery);
        assertEquals(delivery, configuration.getDelivery());
    }

    @Test
    public void testVersionCode() {
        Configuration configuration = generateConfiguration();
        assertEquals(0, (int) configuration.getVersionCode()); // populated in client ctor if null
        configuration.setVersionCode(577);
        assertEquals(577, (int) configuration.getVersionCode());
    }

    @Test
    public void testAddMetadata() {
        Configuration configuration = generateConfiguration();
        configuration.addMetadata("keyVal", "foo", "bar");
        HashMap<String, Object> testMap = new HashMap<>();
        testMap.put("key1", "val1");
        testMap.put("key2", "val2");
        configuration.addMetadata("mapVal", testMap);

        assertEquals("bar", configuration.getMetadata("keyVal", "foo"));
        assertEquals("val1", configuration.getMetadata("mapVal", "key1"));
        assertEquals("val2", configuration.getMetadata("mapVal", "key2"));
    }

    @Test
    public void testClearMetadata() {
        Configuration configuration = generateConfiguration();
        configuration.addMetadata("keyVal", "foo", "bar");
        HashMap<String, Object> testMap = new HashMap<>();
        testMap.put("key1", "val1");
        testMap.put("key2", "val2");
        configuration.addMetadata("mapVal", testMap);

        configuration.clearMetadata("keyVal");
        configuration.clearMetadata("mapVal", "key2");

        assertNull(configuration.getMetadata("keyVal", "foo"));
        assertEquals("val1", configuration.getMetadata("mapVal", "key1"));
        assertNull(configuration.getMetadata("mapVal", "key2"));
    }

    @Test
    public void testSetUser() {
        Configuration configuration = generateConfiguration();
        configuration.setUser("24601", "m@rp.fr", "MM");
        assertEquals("24601", configuration.getUser().getId());
        assertEquals("m@rp.fr", configuration.getUser().getEmail());
        assertEquals("MM", configuration.getUser().getName());
    }
}
