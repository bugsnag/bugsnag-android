package com.bugsnag.android;

import android.test.AndroidTestCase;

import com.bugsnag.android.BeforeNotify;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Error;

public class ConfigurationTest extends AndroidTestCase {
    @Override
    protected void setUp() {

    }

    public void testEndpoints() {
        Configuration config = new Configuration("api-key");

        // Default endpoints
        assertEquals(config.getNotifyEndpoint(), "http://notify.bugsnag.com");
        assertEquals(config.getMetricsEndpoint(), "http://notify.bugsnag.com/metrics");

        // Setting an endpoint
        config.endpoint = "http://localhost:8000";
        assertEquals(config.getNotifyEndpoint(), "http://localhost:8000");
        assertEquals(config.getMetricsEndpoint(), "http://localhost:8000/metrics");
    }

    public void testShouldNotify() {
        Configuration config = new Configuration("api-key");

        // Should notify if notifyReleaseStages is null
        assertTrue(config.shouldNotify());

        // Shouldn't notify if notifyReleaseStages is set and releaseStage is null
        config.notifyReleaseStages = new String[] {"example"};
        assertFalse(config.shouldNotify());

        // Shouldn't notify if releaseStage not in notifyReleaseStages
        config.notifyReleaseStages = new String[] {"production"};
        config.releaseStage = "not-production";
        assertFalse(config.shouldNotify());

        // Should notify if releaseStage in notifyReleaseStages
        config.notifyReleaseStages = new String[] {"production"};
        config.releaseStage = "production";
        assertTrue(config.shouldNotify());
    }

    public void testShouldIgnore() {
        Configuration config = new Configuration("api-key");

        // Should not ignore by default
        assertFalse(config.shouldIgnore("java.io.IOException"));

        // Should ignore when added to ignoreClasses
        config.ignoreClasses = new String[] {"java.io.IOException"};
        assertTrue(config.shouldIgnore("java.io.IOException"));
    }

    public void testBeforeNotify() {
        Configuration config = new Configuration("api-key");
        BeforeNotify beforeNotify = new BeforeNotify() {
            @Override
            public boolean run(Error error) {
                return false;
            }
        };



        // Should not ignore by default
        assertFalse(config.shouldIgnore("java.io.IOException"));

        // Should ignore when added to ignoreClasses
        config.ignoreClasses = new String[] {"java.io.IOException"};
        assertTrue(config.shouldIgnore("java.io.IOException"));
    }
}
