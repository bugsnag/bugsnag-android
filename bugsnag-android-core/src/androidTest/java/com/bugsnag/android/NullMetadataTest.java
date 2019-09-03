package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateSessionTracker;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Ensures that setting metadata to null doesn't result in NPEs
 * <p>
 * See https://github.com/bugsnag/bugsnag-android/issues/194
 */
public class NullMetadataTest {

    private static final String TAB_KEY = "tab";

    private Configuration config;
    private Throwable throwable;
    private Client client;

    /**
     * Generates a bugsnag client with a NOP error api client
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        config = new Configuration("api-key");
        client = generateClient(config);
        throwable = new RuntimeException("Test");
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testConfigSetNullMetadata() throws Exception {
        Configuration configuration = new Configuration("test");
        configuration.setMetaData(null);
        validateDefaultMetadata(configuration.getMetaData());
    }

    @Test
    public void testErrorDefaultMetaData() throws Exception {
        Error error = new Error.Builder(config, throwable, generateSessionTracker(),
            Thread.currentThread(), false).build();
        validateDefaultMetadata(error.getMetaData());
    }

    @Test
    public void testSecondErrorDefaultMetaData() throws Exception {
        Error error = new Error.Builder(config, "RuntimeException",
            "Something broke", new StackTraceElement[]{},
            generateSessionTracker(), Thread.currentThread()).build();
        validateDefaultMetadata(error.getMetaData());
    }

    @Test
    public void testErrorSetMetadataRef() throws Exception {
        Error error = new Error.Builder(config, throwable,
            generateSessionTracker(),
            Thread.currentThread(), false).build();
        MetaData metaData = new MetaData();
        metaData.addToTab(TAB_KEY, "test", "data");
        error.setMetaData(metaData);
        assertNotNull(metaData.getTab(TAB_KEY));
    }

    @Test
    public void testErrorSetNullMetadata() throws Exception {
        Error error = new Error.Builder(config, throwable,
            generateSessionTracker(),
            Thread.currentThread(), false).build();
        error.setMetaData(null);
        validateDefaultMetadata(error.getMetaData());
    }

    @Test
    public void testConfigDefaultMetadata() throws Exception {
        validateDefaultMetadata(config.getMetaData());
    }

    @Test
    public void testConfigSetMetadataRef() throws Exception {
        Configuration configuration = new Configuration("test");
        configuration.setMetaData(new MetaData());
        validateDefaultMetadata(configuration.getMetaData());
    }

    @Test
    public void testNotify() throws Exception {
        client.beforeNotify(new BeforeNotify() {
            @Override
            public boolean run(@NonNull Error error) {
                validateDefaultMetadata(error.getMetaData());
                return false;
            }
        });
        Error error = new Error.Builder(config, new Throwable(),
            generateSessionTracker(), Thread.currentThread(), false).build();
        client.notify(error, DeliveryStyle.SAME_THREAD, null);
    }

    private void validateDefaultMetadata(MetaData metaData) {
        assertNotNull(metaData);
        assertEquals(0, metaData.getTab(TAB_KEY).size());

        metaData.addToTab(TAB_KEY, "test", "data");
        assertEquals(1, metaData.getTab(TAB_KEY).size());
    }

}
