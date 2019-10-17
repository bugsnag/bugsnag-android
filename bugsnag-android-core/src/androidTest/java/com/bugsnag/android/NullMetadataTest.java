package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateSessionTracker;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Ensures that setting metadata to null doesn't result in NPEs
 * <p>
 * See https://github.com/bugsnag/bugsnag-android/issues/194
 */
@SuppressWarnings("unchecked")
public class NullMetadataTest {

    private static final String TAB_KEY = "tab";

    private ImmutableConfig config;
    private Throwable throwable;
    private Client client;

    /**
     * Generates a bugsnag client with a NOP error api client
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        client = generateClient(new Configuration("api-key"));
        config = client.immutableConfig;
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
            Thread.currentThread(), false, new MetaData()).build();
        validateDefaultMetadata(error);
    }

    @Test
    public void testSecondErrorDefaultMetaData() throws Exception {
        Error error = new Error.Builder(config, "RuntimeException",
            "Something broke", new StackTraceElement[]{},
            generateSessionTracker(), Thread.currentThread(), new MetaData()).build();
        validateDefaultMetadata(error);
    }

    @Test
    public void testConfigDefaultMetadata() throws Exception {
        validateDefaultMetadata(client.getConfiguration().getMetaData());
    }

    @Test
    public void testConfigSetMetadataRef() throws Exception {
        Configuration configuration = new Configuration("test");
        configuration.setMetaData(new MetaData());
        validateDefaultMetadata(configuration.getMetaData());
    }

    @Test
    public void testNotify() throws Exception {
        client.addBeforeNotify(new BeforeNotify() {
            @Override
            public boolean run(@NonNull Error error) {
                validateDefaultMetadata(error);
                return false;
            }
        });
        Error error = new Error.Builder(config, new Throwable(),
            generateSessionTracker(), Thread.currentThread(), false, new MetaData()).build();
        client.notify(error, DeliveryStyle.SAME_THREAD, null);
    }

    private void validateDefaultMetadata(MetaDataAware metaData) {
        assertNotNull(metaData);
        Map<String, Object> metadata = (Map<String, Object>) metaData.getMetadata(TAB_KEY, null);
        assertEquals(0, metadata.size());

        metaData.addMetadata(TAB_KEY, "test", "data");
        assertEquals(1, metadata.size());
    }

}
