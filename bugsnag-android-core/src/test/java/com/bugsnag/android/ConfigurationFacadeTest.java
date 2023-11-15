package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@SuppressWarnings("ConstantConditions") // suppress warning about making redundant null checks
public class ConfigurationFacadeTest {

    private Configuration config;
    private InterceptingLogger logger;

    /**
     * Constructs a configuration with implementation + logger interceptor to verify messages
     * are logged when null is set
     */
    @Before
    public void setUp() {
        config = new Configuration("5d1ec5bd39a74caa1267142706a7fb21");
        logger = new InterceptingLogger();
        config.setLogger(logger);
    }

    @Test
    public void apiKeyValid() {
        config.setApiKey("ffffc5bd39a74caa1267142706a7fb21");
        assertEquals("ffffc5bd39a74caa1267142706a7fb21", config.impl.getApiKey());
    }

    @Test
    public void appVersionValid() {
        config.setAppVersion("1.23");
        assertEquals("1.23", config.impl.getAppVersion());
        config.setAppVersion(null);
        assertNull(config.impl.getAppVersion());
    }

    @Test
    public void versionCodeValid() {
        config.setVersionCode(55);
        assertEquals(55, (int) config.impl.getVersionCode());
        config.setVersionCode(null);
        assertNull(config.impl.getVersionCode());
    }

    @Test
    public void releaseStageValid() {
        config.setReleaseStage("prod");
        assertEquals("prod", config.impl.getReleaseStage());
        config.setReleaseStage(null);
        assertNull(config.impl.getReleaseStage());
    }

    @Test
    public void sendThreadsValid() {
        config.setSendThreads(ThreadSendPolicy.UNHANDLED_ONLY);
        assertEquals(ThreadSendPolicy.UNHANDLED_ONLY, config.impl.getSendThreads());
    }

    @Test
    public void sendThreadsInvalid() {
        config.setSendThreads(null);
        assertEquals(ThreadSendPolicy.ALWAYS, config.impl.getSendThreads());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void persistUserValid() {
        assertTrue(config.impl.getPersistUser());
        config.setPersistUser(false);
        assertFalse(config.impl.getPersistUser());
    }

    @Test
    public void launchDurationMillisValid() {
        config.setLaunchDurationMillis(123456);
        assertEquals(123456, config.impl.getLaunchDurationMillis());
    }

    @Test
    public void autoTrackSessionsValid() {
        config.setAutoTrackSessions(true);
        assertTrue(config.impl.getAutoTrackSessions());
    }

    @Test
    public void sendLaunchCrashesSynchronouslyValid() {
        config.setSendLaunchCrashesSynchronously(true);
        assertTrue(config.impl.getSendLaunchCrashesSynchronously());
    }

    @Test
    public void errorTypesValid() {
        ErrorTypes errorTypes = new ErrorTypes();
        errorTypes.setNdkCrashes(true);
        config.setEnabledErrorTypes(errorTypes);
        assertEquals(errorTypes, config.impl.getEnabledErrorTypes());
    }

    @Test
    public void errorTypesInvalid() {
        ErrorTypes errorTypes = config.getEnabledErrorTypes();
        config.setEnabledErrorTypes(null);
        assertEquals(errorTypes, config.impl.getEnabledErrorTypes());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void autoDetectErrorsValid() {
        config.setAutoDetectErrors(false);
        assertFalse(config.impl.getAutoDetectErrors());
    }

    @Test
    public void appTypeValid() {
        config.setAppType("react-native");
        assertEquals("react-native", config.impl.getAppType());
        config.setAppType(null);
        assertNull(config.impl.getAppType());
    }

    @Test
    public void loggerValid() {
        config.setLogger(NoopLogger.INSTANCE);
        assertEquals(NoopLogger.INSTANCE, config.impl.getLogger());
    }

    @Test
    public void deliveryValid() {
        Delivery delivery = BugsnagTestUtils.generateDelivery();
        config.setDelivery(delivery);
        assertEquals(delivery, config.impl.getDelivery());
    }

    @Test
    public void deliveryInvalid() {
        Delivery delivery = config.getDelivery();
        config.setDelivery(null);
        assertEquals(delivery, config.impl.getDelivery());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void endpointsValid() {
        EndpointConfiguration endpoints = new EndpointConfiguration("http://example.com:1234", "http://example.com:4567");
        config.setEndpoints(endpoints);
        assertEquals(endpoints, config.impl.getEndpoints());
    }

    @Test
    public void endpointsInvalid() {
        EndpointConfiguration endpoints = config.impl.getEndpoints();
        config.setEndpoints(null);
        assertEquals(endpoints, config.getEndpoints());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void maxBreadcrumbsValid() {
        config.setMaxBreadcrumbs(66);
        assertEquals(66, config.impl.getMaxBreadcrumbs());
    }

    @Test
    public void maxPersistedEventsValid() {
        config.setMaxPersistedEvents(55);
        assertEquals(55, config.impl.getMaxPersistedEvents());
    }

    @Test
    public void maxPersistedEventsInvalid() {
        config.setMaxPersistedEvents(-1);
        assertEquals(32, config.impl.getMaxPersistedEvents());
    }

    @Test
    public void maxPersistedSessionsValid() {
        config.setMaxPersistedSessions(55);
        assertEquals(55, config.impl.getMaxPersistedSessions());
    }

    @Test
    public void maxPersistedSessionsInvalid() {
        config.setMaxPersistedSessions(-1);
        assertEquals(128, config.impl.getMaxPersistedSessions());
    }

    @Test
    public void contextValid() {
        config.setContext("Whoops");
        assertEquals("Whoops", config.impl.getContext());
        config.setContext(null);
        assertNull(config.impl.getContext());
    }

    @Test
    public void redactedKeysValid() {
        Set<Pattern> redactedKeys = new HashSet<>();
        config.setRedactedKeys(redactedKeys);
        assertEquals(redactedKeys, config.impl.getRedactedKeys());
    }

    @Test
    public void redactedKeysInvalid() {
        Set<Pattern> keys = config.impl.getRedactedKeys();
        config.setRedactedKeys(null);
        assertEquals(keys, config.impl.getRedactedKeys());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void discardClassesValid() {
        Set<Pattern> discardClasses = new HashSet<>();
        discardClasses.add(Pattern.compile("com.example.Foo"));
        config.setDiscardClasses(discardClasses);
        assertEquals(discardClasses, config.impl.getDiscardClasses());
    }

    @Test
    public void discardClassesInvalid() {
        Set<Pattern> classes = config.impl.getDiscardClasses();
        config.setDiscardClasses(null);
        assertEquals(classes, config.impl.getDiscardClasses());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void enabledReleaseStagesValid() {
        Set<String> releaseStages = new HashSet<>();
        config.setEnabledReleaseStages(releaseStages);
        assertEquals(releaseStages, config.impl.getEnabledReleaseStages());
        config.setEnabledReleaseStages(null);
        assertNull(config.impl.getEnabledReleaseStages());
    }

    @Test
    public void enabledBreadcrumbTypesValid() {
        Set<BreadcrumbType> breadcrumbTypes = new HashSet<>();
        config.setEnabledBreadcrumbTypes(breadcrumbTypes);
        assertEquals(breadcrumbTypes, config.impl.getEnabledBreadcrumbTypes());
        config.setEnabledBreadcrumbTypes(null);
        assertNull(config.impl.getEnabledBreadcrumbTypes());
    }

    @Test
    public void projectPackagesValid() {
        Set<String> projectPackages = new HashSet<>();
        projectPackages.add("prod");
        config.setProjectPackages(projectPackages);
        assertEquals(projectPackages, config.impl.getProjectPackages());
    }

    @Test
    public void projectPackagesInvalid() {
        Set<String> packages = config.impl.getProjectPackages();
        config.setProjectPackages(null);
        assertEquals(packages, config.impl.getProjectPackages());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addRemoveOnErrorValid() {
        OnErrorCallback cb = new OnErrorCallback() {
            @Override
            public boolean onError(@NonNull Event event) {
                return false;
            }
        };
        config.addOnError(cb);

        // verify add on error
        Collection<OnErrorCallback> cbs = config.impl.callbackState.getOnErrorTasks();
        assertEquals(1, cbs.size());
        assertEquals(cb, cbs.iterator().next());

        // verify remove on error
        config.removeOnError(cb);
        assertEquals(0, cbs.size());
    }

    @Test
    public void addOnErrorInvalid() {
        config.addOnError(null);
        Collection<OnErrorCallback> cbs = config.impl.callbackState.getOnErrorTasks();
        assertEquals(0, cbs.size());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void removeOnErrorInvalid() {
        OnErrorCallback cb = new OnErrorCallback() {
            @Override
            public boolean onError(@NonNull Event event) {
                return false;
            }
        };
        config.addOnError(cb);
        config.removeOnError(null);
        Collection<OnErrorCallback> cbs = config.impl.callbackState.getOnErrorTasks();
        assertEquals(1, cbs.size());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addOnSessionValid() {
        OnSessionCallback cb = new OnSessionCallback() {
            @Override
            public boolean onSession(@NonNull Session session) {
                return false;
            }
        };
        config.addOnSession(cb);

        // verify add on session
        Collection<OnSessionCallback> cbs = config.impl.callbackState.getOnSessionTasks();
        assertEquals(1, cbs.size());
        assertEquals(cb, cbs.iterator().next());

        // verify remove on session
        config.removeOnSession(cb);
        assertEquals(0, cbs.size());
    }

    @Test
    public void addOnSessionInvalid() {
        config.addOnSession(null);
        Collection<OnSessionCallback> cbs = config.impl.callbackState.getOnSessionTasks();
        assertEquals(0, cbs.size());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void removeOnSessionInvalid() {
        OnSessionCallback cb = new OnSessionCallback() {
            @Override
            public boolean onSession(@NonNull Session session) {
                return false;
            }
        };
        config.addOnSession(cb);
        config.addOnSession(null);
        Collection<OnSessionCallback> cbs = config.impl.callbackState.getOnSessionTasks();
        assertEquals(1, cbs.size());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addOnBreadcrumbValid() {
        OnBreadcrumbCallback cb = new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        };
        config.addOnBreadcrumb(cb);

        // verify add on breadcrumb
        Collection<OnBreadcrumbCallback> cbs = config.impl.callbackState.getOnBreadcrumbTasks();
        assertEquals(1, cbs.size());
        assertEquals(cb, cbs.iterator().next());

        // verify remove on breadcrumb
        config.removeOnBreadcrumb(cb);
        assertEquals(0, cbs.size());
    }

    @Test
    public void addOnBreadcrumbInvalid() {
        config.addOnBreadcrumb(null);
        Collection<OnBreadcrumbCallback> cbs = config.impl.callbackState.getOnBreadcrumbTasks();
        assertEquals(0, cbs.size());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void removeOnBreadcrumbInvalid() {
        OnBreadcrumbCallback cb = new OnBreadcrumbCallback() {
            @Override
            public boolean onBreadcrumb(@NonNull Breadcrumb breadcrumb) {
                return false;
            }
        };
        config.addOnBreadcrumb(cb);
        config.addOnBreadcrumb(null);
        Collection<OnBreadcrumbCallback> cbs = config.impl.callbackState.getOnBreadcrumbTasks();
        assertEquals(1, cbs.size());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValid() {
        Map<String, Boolean> map = Collections.singletonMap("test", true);
        config.addMetadata("foo", map);
        assertEquals(map, config.getMetadata("foo"));
    }

    @Test
    public void addMetadataInvalid1() {
        config.addMetadata("foo", null);
        assertNull(config.getMetadata("foo"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValueValid() {
        config.addMetadata("foo", "test", true);
        assertTrue((Boolean) config.getMetadata("foo", "test"));
    }

    @Test
    public void addMetadataValueInvalid1() {
        config.addMetadata(null, "test", true);
        assertNull(config.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValueInvalid2() {
        config.addMetadata("foo", null, true);
        assertNull(config.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValid() {
        config.addMetadata("foo", "test", true);
        config.clearMetadata("foo");
        assertNull(config.getMetadata("foo"));
    }

    @Test
    public void clearMetadataInvalid() {
        config.addMetadata("foo", "test", true);
        config.clearMetadata(null);
        assertTrue((Boolean) config.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValueValid() {
        config.addMetadata("foo", "test", true);
        config.clearMetadata("foo", "test");
        assertNull(config.getMetadata("foo", "test"));
    }

    @Test
    public void clearMetadataValueInvalid1() {
        config.addMetadata("foo", "test", true);
        config.clearMetadata(null, "test");
        assertTrue((Boolean) config.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValueInvalid2() {
        config.addMetadata("foo", "test", true);
        config.clearMetadata("foo", null);
        assertTrue((Boolean) config.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValid() {
        config.addMetadata("foo", "test", true);
        assertTrue((Boolean) config.getMetadata("foo", "test"));
    }

    @Test
    public void getMetadataInvalid() {
        assertNull(config.getMetadata(null));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValueValid() {
        config.addMetadata("foo", "test", true);
        assertTrue((Boolean) config.getMetadata("foo", "test"));
    }

    @Test
    public void getMetadataValueInvalid1() {
        config.addMetadata("foo", "test", true);
        assertNull(config.getMetadata(null, "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValueInvalid2() {
        config.addMetadata("foo", "test", true);
        assertNull(config.getMetadata("foo", null));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void setUserValid() {
        config.setUser(null, null, null);
        assertEquals(new User(null, null, null), config.getUser());
    }

    @Test
    public void setPersistenceDirectoryValid() throws IOException {
        File dir = Files.createTempDirectory("foo").toFile();
        config.setPersistenceDirectory(dir);
        assertEquals(dir, config.getPersistenceDirectory());
        config.setPersistenceDirectory(null);
        assertNull(config.getPersistenceDirectory());
    }

    @Test
    public void addPluginValid() {
        Plugin plugin = new Plugin() {
            @Override
            public void load(@NonNull Client client) {
            }

            @Override
            public void unload() {
            }
        };
        config.addPlugin(plugin);
        assertEquals(plugin, config.impl.getPlugins().toArray()[0]);
    }

    @Test
    public void addPluginInvalid() {
        config.addPlugin(null);
        assertTrue(config.impl.getPlugins().isEmpty());
        assertNotNull(logger.getMsg());
    }
}
