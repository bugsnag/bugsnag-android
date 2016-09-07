package com.bugsnag.android;

public class ConfigurationTest extends BugsnagTestCase {
    public void testEndpoints() {
        Configuration config = new Configuration("api-key");

        // Default endpoints
        assertEquals("https://notify.bugsnag.com", config.getEndpoint());

        // Setting an endpoint
        config.setEndpoint("http://localhost:8000");
        assertEquals("http://localhost:8000", config.getEndpoint());
    }

    public void testShouldNotify() {
        Configuration config = new Configuration("api-key");

        // Should notify if notifyReleaseStages is null
        assertTrue(config.shouldNotifyForReleaseStage("development"));

        // Shouldn't notify if notifyReleaseStages is set and releaseStage is null
        config.setNotifyReleaseStages(new String[] {"example"});
        assertFalse(config.shouldNotifyForReleaseStage(null));

        // Shouldn't notify if releaseStage not in notifyReleaseStages
        config.setNotifyReleaseStages(new String[] {"production"});
        assertFalse(config.shouldNotifyForReleaseStage("not-production"));

        // Should notify if releaseStage in notifyReleaseStages
        config.setNotifyReleaseStages(new String[] {"production"});
        assertTrue(config.shouldNotifyForReleaseStage("production"));
    }

    public void testShouldIgnore() {
        Configuration config = new Configuration("api-key");

        // Should not ignore by default
        assertFalse(config.shouldIgnoreClass("java.io.IOException"));

        // Should ignore when added to ignoreClasses
        config.setIgnoreClasses(new String[] {"java.io.IOException"});
        assertTrue(config.shouldIgnoreClass("java.io.IOException"));
    }

    public void testInProject() {
        Configuration config = new Configuration("api-key");

        // Shouldn't be inProject if projectPackages hasn't been set
        assertFalse(config.inProject("com.bugsnag.android.Example"));

        // Should be inProject if class in projectPackages
        config.setProjectPackages(new String[] {"com.bugsnag.android"});
        assertTrue(config.inProject("com.bugsnag.android.Example"));

        // Shouldn't be inProject if class not in projectPackages
        config.setProjectPackages(new String[] {"com.bugsnag.android"});
        assertFalse(config.inProject("java.io.IOException"));
    }
}
