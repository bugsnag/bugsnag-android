package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.lang.IllegalArgumentException;

import java.util.Collections;
import java.util.Set;

public class ConfigurationNullableTest {

    private Configuration config;

    @Before
    public void setup() {
        config = new Configuration("12312312312312312312312312312312");
    }

    @Test
    public void testAppTypeNull() {
        config.setAppType(null);
    }

    @Test
    public void testAppVersionNull() {
        config.setAppVersion(null);
    }

    @Test
    public void testEnabledReleaseStagesNull() {
        config.setEnabledReleaseStages(null);
    }

    @Test
    public void testReleaseStageNull() {
        config.setReleaseStage(null);
    }

    @Test
    public void testVersionCodeNull() {
        config.setVersionCode(null);
    }

    @Test
    public void testEnabledBreadcrumbTypesNull() {
        config.setEnabledBreadcrumbTypes(null);
    }

    @Test
    public void testContextNull() {
        config.setContext(null);
    }

    // @Test
    // public void testDiscardClassesNull() {
    //     config.setDiscardClasses(null);
    // }

    // @Test
    // public void testEndpointsNull() {
    //     config.setEndpoints(null);
    // }

    @Test
    public void testLoggerNull() {
        config.setLogger(null);
    }
}
