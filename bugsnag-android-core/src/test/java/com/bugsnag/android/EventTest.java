package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import java.lang.Thread;
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
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        config = BugsnagTestUtils.generateImmutableConfig();
        RuntimeException exception = new RuntimeException("Example message");
        HandledState handledState = this.handledState;
        event = new Event(exception, config, handledState);
    }

    @Test
    public void testBugsnagExceptionName() throws Exception {
        Event err = new Event(new RuntimeException("whoops"), config, handledState);
        err.getErrors().get(0).setErrorClass("Busgang");
        assertEquals("Busgang", err.getErrors().get(0).getErrorClass());
    }

    @Test
    public void testNullContext() throws Exception {
        event.setContext(null);
        event.setApp(Collections.<String, Object>emptyMap());
        assertNull(event.getContext());
    }

    @Test
    public void testSetUser() throws Exception {
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
        Map<String, Object> rocks = (Map<String, Object>) event.getMetadata("rocks", null);
        assertNotNull(rocks);

        event.clearMetadata("rocks", null);
        assertFalse(rocks.isEmpty());
        assertNull(event.getMetadata("rocks", null));
    }
}
