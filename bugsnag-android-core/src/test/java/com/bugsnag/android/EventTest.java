package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

@SuppressWarnings("unchecked")
public class EventTest {

    private final HandledState handledState
            = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION);
    private ImmutableConfig config;
    private Event event;

    /**
     * Generates a new default event for use by tests
     *
     */
    @Before
    public void setUp() {
        config = BugsnagTestUtils.generateImmutableConfig();
        RuntimeException exception = new RuntimeException("Example message");
        HandledState handledState = this.handledState;
        event = new Event(exception, config, handledState, NoopLogger.INSTANCE);
    }

    @Test
    public void checkExceptionMessageNullity() {
        Event err = new Event(new RuntimeException(), config, handledState, NoopLogger.INSTANCE);
        assertNull(err.getErrors().get(0).getErrorMessage());
    }

    @Test
    public void testExceptionName() {
        RuntimeException exc = new RuntimeException("whoops");
        Event err = new Event(exc, config, handledState, NoopLogger.INSTANCE);
        err.getErrors().get(0).setErrorClass("Busgang");
        assertEquals("Busgang", err.getErrors().get(0).getErrorClass());
    }

    @Test
    public void testNullContext() {
        event.setContext(null);
        assertNull(event.getContext());
    }

    @Test
    public void testSetUser() {
        String firstId = "123";
        String firstEmail = "fake@example.com";
        String firstName = "Bob Swaggins";
        event.setUser(firstId, firstEmail, firstName);

        assertEquals(firstId, event.getUser().getId());
        assertEquals(firstEmail, event.getUser().getEmail());
        assertEquals(firstName, event.getUser().getName());

        String userId = "foo";
        event.setUser(userId, event.getUser().getEmail(), event.getUser().getName());
        assertEquals(userId, event.getUser().getId());
        assertEquals(firstEmail, event.getUser().getEmail());
        assertEquals(firstName, event.getUser().getName());

        String userEmail = "another@example.com";
        event.setUser(event.getUser().getId(), userEmail, event.getUser().getName());
        assertEquals(userId, event.getUser().getId());
        assertEquals(userEmail, event.getUser().getEmail());
        assertEquals(firstName, event.getUser().getName());

        String userName = "Isaac";
        event.setUser(event.getUser().getId(), event.getUser().getEmail(), userName);
        assertEquals(userId, event.getUser().getId());
        assertEquals(userEmail, event.getUser().getEmail());
        assertEquals(userName, event.getUser().getName());
    }

    @Test
    public void testErrorMetadata() {
        event.addMetadata("rocks", "geode", "a shiny mineral");
        Map<String, Object> rocks = event.getMetadata("rocks");
        assertNotNull(rocks);

        event.clearMetadata("rocks");
        assertFalse(rocks.isEmpty());
        assertNull(event.getMetadata("rocks"));
    }
}
