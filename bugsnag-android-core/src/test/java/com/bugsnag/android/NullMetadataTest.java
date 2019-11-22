package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.lang.Thread;

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

    /**
     * Generates a bugsnag client with a NOP error api client
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        config = BugsnagTestUtils.generateImmutableConfig();
        throwable = new RuntimeException("Test");
    }

    @Test
    public void testErrorDefaultMetadata() throws Exception {
        Event event = new Event.Builder(config, throwable, null,
            Thread.currentThread(), false, new Metadata()).build();
        validateDefaultMetadata(event);
    }

    @Test
    public void testSecondErrorDefaultMetadata() throws Exception {
        Event event = new Event.Builder(config, "RuntimeException",
            "Something broke", new StackTraceElement[]{},
            null, Thread.currentThread(), new Metadata()).build();
        validateDefaultMetadata(event);
    }

    @Test
    public void testConfigSetMetadataRef() throws Exception {
        Configuration configuration = new Configuration("test");
        configuration.setMetadata(new Metadata());
        validateDefaultMetadata(configuration.getMetadata());
    }

    private void validateDefaultMetadata(MetadataAware event) {
        assertNull(event.getMetadata(TAB_KEY, null));
        event.addMetadata(TAB_KEY, "test", "data");
        assertEquals("data", event.getMetadata(TAB_KEY, "test"));
    }

}
