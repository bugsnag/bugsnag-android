package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.bugsnag.android.internal.ImmutableConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 * Ensures that setting metadata to null doesn't result in NPEs
 * <p>
 * See https://github.com/bugsnag/bugsnag-android/issues/194
 */
public class NullMetadataTest {

    private static final String TAB_KEY = "tab";

    private ImmutableConfig config;
    private Throwable throwable;

    /**
     * Generates a bugsnag client with a NOP error api client
     *
     */
    @Before
    public void setUp() {
        config = BugsnagTestUtils.generateImmutableConfig();
        throwable = new RuntimeException("Test");
    }

    @Test
    public void testErrorDefaultMetadata() {
        SeverityReason severityReason
                = SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION);
        Event event = new Event(throwable, config, severityReason, NoopLogger.INSTANCE);
        validateDefaultMetadata(event);
    }

    @Test
    public void testSecondErrorDefaultMetadata() {
        SeverityReason reason
                = SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION);
        Event event = new Event(new RuntimeException(), config, reason, NoopLogger.INSTANCE);
        List<String> projectPackages = Collections.emptyList();
        Stacktrace trace = new Stacktrace(new StackTraceElement[]{},
                projectPackages,
                NoopLogger.INSTANCE);
        Error err = new Error(new ErrorInternal("RuntimeException", "Something broke",
                trace), NoopLogger.INSTANCE);
        event.getErrors().clear();
        event.getErrors().add(err);
        validateDefaultMetadata(event);
    }

    private void validateDefaultMetadata(MetadataAware error) {
        assertNull(error.getMetadata(TAB_KEY));
        error.addMetadata(TAB_KEY, "test", "data");
        assertEquals("data", error.getMetadata(TAB_KEY, "test"));
    }

}
