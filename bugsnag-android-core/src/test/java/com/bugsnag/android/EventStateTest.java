package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.convert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

public class EventStateTest {

    private HandledState handledState
            = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION);
    private ImmutableConfig config;
    private Event event;

    /**
     * Generates a new default event for use by tests
     */
    @Before
    public void setUp() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        this.config = convert(configuration);
        RuntimeException exception = new RuntimeException("Example message");
        event = new Event(exception, config, handledState);
    }

    @Test
    public void shouldIgnoreMatches() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setIgnoreClasses(Collections.singleton("java.io.IOException"));

        event = new Event(new IOException(), BugsnagTestUtils.convert(configuration), handledState);
        assertTrue(event.shouldIgnoreClass());
    }

    @Test
    public void overrideSeverityInternal() {
        event.updateSeverityInternal(Severity.INFO);
        assertEquals(Severity.INFO, event.getSeverity());
    }
}
