package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.convert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

public class EventStateTest {

    private final SeverityReason severityReason
            = SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION);
    private Event event;

    /**
     * Generates a new default event for use by tests
     */
    @Before
    public void setUp() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        ImmutableConfig config = convert(configuration);
        RuntimeException exception = new RuntimeException("Example message");
        event = new Event(exception, config, severityReason, NoopLogger.INSTANCE);
    }

    @Test
    public void shouldIgnoreMatches() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setDiscardClasses(Collections.singleton("java.io.IOException"));

        ImmutableConfig conig = convert(configuration);
        event = new Event(new IOException(), conig, severityReason, NoopLogger.INSTANCE);
        assertTrue(event.shouldDiscardClass());
    }

    @Test
    public void shouldIgnoreMatchesMultiple() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setDiscardClasses(Collections.singleton("java.io.IOException"));

        RuntimeException exc = new RuntimeException(new IOException());
        ImmutableConfig conig = convert(configuration);
        event = new Event(exc, conig, severityReason, NoopLogger.INSTANCE);
        assertTrue(event.shouldDiscardClass());
    }

    @Test
    public void shouldIgnoreDoesNotMatch() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setDiscardClasses(Collections.<String>emptySet());
        ImmutableConfig config = convert(configuration);
        event = new Event(new IOException(), config, severityReason, NoopLogger.INSTANCE);
        assertFalse(event.shouldDiscardClass());
    }

    @Test
    public void shouldIgnoreDoesNotMatchMultiple() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setDiscardClasses(Collections.<String>emptySet());
        RuntimeException exc = new RuntimeException(new IllegalStateException());

        ImmutableConfig config = convert(configuration);
        event = new Event(exc, config, severityReason, NoopLogger.INSTANCE);
        assertFalse(event.shouldDiscardClass());
    }

    @Test
    public void overrideSeverityInternal() {
        event.updateSeverityInternal(Severity.INFO);
        assertEquals(Severity.INFO, event.getSeverity());
    }
}
