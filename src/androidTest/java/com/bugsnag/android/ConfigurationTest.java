package com.bugsnag.android;

public class ConfigurationTest extends BugsnagTestCase {
    public void testEndpoints() {
        Configuration config = new Configuration("api-key");

        // Default endpoints
        assertEquals("https://notify.bugsnag.com", config.getNotifyEndpoint());

        // Setting an endpoint
        config.endpoint = "http://localhost:8000";
        assertEquals("http://localhost:8000", config.getNotifyEndpoint());
    }

    public void testShouldNotify() {
        Configuration config = new Configuration("api-key");

        // Should notify if notifyReleaseStages is null
        assertTrue(config.shouldNotifyForReleaseStage("development"));

        // Shouldn't notify if notifyReleaseStages is set and releaseStage is null
        config.notifyReleaseStages = new String[] {"example"};
        assertFalse(config.shouldNotifyForReleaseStage(null));

        // Shouldn't notify if releaseStage not in notifyReleaseStages
        config.notifyReleaseStages = new String[] {"production"};
        assertFalse(config.shouldNotifyForReleaseStage("not-production"));

        // Should notify if releaseStage in notifyReleaseStages
        config.notifyReleaseStages = new String[] {"production"};
        assertTrue(config.shouldNotifyForReleaseStage("production"));
    }

    public void testShouldIgnore() {
        Configuration config = new Configuration("api-key");

        // Should not ignore by default
        assertFalse(config.shouldIgnoreClass("java.io.IOException"));

        // Should ignore when added to ignoreClasses
        config.ignoreClasses = new String[] {"java.io.IOException"};
        assertTrue(config.shouldIgnoreClass("java.io.IOException"));
    }

    public void testInProject() {
        Configuration config = new Configuration("api-key");

        // Shouldn't be inProject if projectPackages hasn't been set
        assertFalse(config.inProject("com.bugsnag.android.Example"));

        // Should be inProject if class in projectPackages
        config.projectPackages = new String[] {"com.bugsnag.android"};
        assertTrue(config.inProject("com.bugsnag.android.Example"));

        // Shouldn't be inProject if class not in projectPackages
        config.projectPackages = new String[] {"com.bugsnag.android"};
        assertFalse(config.inProject("java.io.IOException"));
    }
}
