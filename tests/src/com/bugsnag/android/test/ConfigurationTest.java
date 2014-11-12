package com.bugsnag.android;

import android.test.AndroidTestCase;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.Client;

public class ConfigurationTest extends AndroidTestCase {
    private Configuration config;

    @Override
    protected void setUp() {
        config = new Configuration("api-key-here");
    }

    public void testDefaultEndpoints() {
        assertEquals(config.getNotifyEndpoint(), "http://notify.bugsnag.com");
        assertEquals(config.getMetricsEndpoint(), "http://notify.bugsnag.com/metrics");
    }
}
