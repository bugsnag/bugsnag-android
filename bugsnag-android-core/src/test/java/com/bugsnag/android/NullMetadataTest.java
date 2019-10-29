package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
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
    public void testErrorDefaultMetaData() throws Exception {
        Event event = new Event.Builder(config, throwable, null,
            Thread.currentThread(), false, new MetaData()).build();
        validateDefaultMetadata(event);
    }

    @Test
    public void testSecondErrorDefaultMetaData() throws Exception {
        Event event = new Event.Builder(config, "RuntimeException",
            "Something broke", new StackTraceElement[]{},
            null, Thread.currentThread(), new MetaData()).build();
        validateDefaultMetadata(event);
    }

    private void validateDefaultMetadata(MetaDataAware error) {
        assertNull(error.getMetadata(TAB_KEY, null));
        error.addMetadata(TAB_KEY, "test", "data");
        assertEquals("data", error.getMetadata(TAB_KEY, "test"));
    }

}
