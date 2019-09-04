package com.bugsnag.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ConfigurationTest {

    private Configuration config;

    @Before
    public void setUp() throws Exception {
        config = BugsnagTestUtils.generateConfiguration();
    }

    @Test
    public void testEndpoints() {
        String notify = "https://notify.myexample.com";
        String sessions = "https://sessions.myexample.com";
        config.setEndpoints(new Endpoints(notify, sessions));

        assertEquals(notify, config.getEndpoints().getNotify());
        assertEquals(sessions, config.getEndpoints().getSessions());
    }

    @Test
    public void testShouldNotify() {
        // Should notify if notifyReleaseStages is null
        ImmutableConfig immutableConfig;
        immutableConfig = createConfigWithReleaseStages(config,
                config.getNotifyReleaseStages(), "development");
        assertTrue(immutableConfig.shouldNotifyForReleaseStage());

        // Shouldn't notify if notifyReleaseStages is set and releaseStage is null
        Set<String> example = Collections.singleton("example");
        immutableConfig = createConfigWithReleaseStages(config, example, null);
        assertFalse(immutableConfig.shouldNotifyForReleaseStage());

        // Shouldn't notify if releaseStage not in notifyReleaseStages
        Set<String> stages = Collections.singleton("production");
        immutableConfig = createConfigWithReleaseStages(config, stages, "not-production");
        assertFalse(immutableConfig.shouldNotifyForReleaseStage());

        // Should notify if releaseStage in notifyReleaseStages
        immutableConfig = createConfigWithReleaseStages(config, stages, "production");
        assertTrue(immutableConfig.shouldNotifyForReleaseStage());
    }

    private ImmutableConfig createConfigWithReleaseStages(Configuration config,
                                                          Collection<String> releaseStages,
                                                          String releaseStage) {
        config.setNotifyReleaseStages(releaseStages);
        config.setReleaseStage(releaseStage);
        return BugsnagTestUtils.convert(config);
    }

    @Test
    public void testLaunchThreshold() throws Exception {
        assertEquals(5000L, config.getLaunchCrashThresholdMs());

        config.setLaunchCrashThresholdMs(-5);
        assertEquals(0, config.getLaunchCrashThresholdMs());

        int expected = 1500;
        config.setLaunchCrashThresholdMs(expected);
        assertEquals(expected, config.getLaunchCrashThresholdMs());
    }

    @Test
    public void testAutoCaptureSessions() throws Exception {
        assertTrue(config.getAutoCaptureSessions());
        config.setAutoCaptureSessions(false);
        assertFalse(config.getAutoCaptureSessions());
    }

    @Test
    public void testOverrideContext() throws Exception {
        config.setContext("LevelOne");
        assertEquals("LevelOne", config.getContext());
    }

    @Test
    public void testOverrideFilters() throws Exception {
        config.setFilters(Collections.singleton("Foo"));
        assertEquals(Collections.singleton("Foo"), config.getFilters());
    }

    @Test
    public void testOverrideIgnoreClasses() throws Exception {
        config.setIgnoreClasses(Collections.singleton("Bar"));
        assertEquals(Collections.singleton("Bar"), config.getIgnoreClasses());
    }

    @Test
    public void testOverrideNotifyReleaseStages() throws Exception {
        config.setNotifyReleaseStages(Collections.singleton("Test"));
        assertEquals(Collections.singleton("Test"), config.getNotifyReleaseStages());
    }

    @Test
    public void testOverrideNotifierType() throws Exception {
        config.setNotifierType("React Native");
        assertEquals("React Native", config.getNotifierType());
    }

    @Test
    public void testOverrideCodeBundleId() throws Exception {
        config.setCodeBundleId("abc123");
        assertEquals("abc123", config.getCodeBundleId());
    }

    @Test
    public void testSetDelivery() {
        Configuration configuration = new Configuration("api-key");
        assertNull(configuration.getDelivery());
        Delivery delivery = new Delivery() {
            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull SessionTrackingPayload payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return null;
            }

            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull Report report,
                                          @NotNull DeliveryParams deliveryParams) {
                return null;
            }
        };
        configuration.setDelivery(delivery);

        assertFalse(configuration.getDelivery() instanceof DefaultDelivery);
        assertEquals(delivery, configuration.getDelivery());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNullDelivery() {
        config.setDelivery(null);
    }

}
