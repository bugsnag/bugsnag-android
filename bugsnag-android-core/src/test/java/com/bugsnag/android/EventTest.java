package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.lang.Thread;
import java.util.Map;

@SuppressWarnings("unchecked")
public class EventTest {

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
        event = new Event.Builder(config, exception, null,
            Thread.currentThread(), false, new Metadata()).build();
    }

    @Test
    public void checkExceptionMessageNullity() throws Exception {
        String msg = "Foo";
        Event err = new Event.Builder(config,
            new RuntimeException(msg), null,
            Thread.currentThread(), false, new Metadata()).build();
        assertEquals(msg, err.getExceptionMessage());

        err = new Event.Builder(config,
            new RuntimeException(), null,
            Thread.currentThread(), false, new Metadata()).build();
        assertEquals("", err.getExceptionMessage());
    }

    @Test
    public void testNullSeverity() throws Exception {
        event.setSeverity(null);
        assertEquals(Severity.WARNING, event.getSeverity());
    }

    @Test
    public void testBugsnagExceptionName() throws Exception {
        BugsnagException exception = new BugsnagException("Busgang", "exceptional",
            new StackTraceElement[]{});
        Event err = new Event.Builder(config,
            exception, null, Thread.currentThread(), false, new Metadata()).build();
        assertEquals("Busgang", err.getExceptionName());
    }

    @Test
    public void testNullContext() throws Exception {
        event.setContext(null);
        event.setAppData(null);
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
        event.setUserId(userId);
        assertEquals(userId, event.getUser().getId());
        assertEquals(firstEmail, event.getUser().getEmail());
        assertEquals(firstName, event.getUser().getName());

        String userEmail = "another@example.com";
        event.setUserEmail(userEmail);
        assertEquals(userId, event.getUser().getId());
        assertEquals(userEmail, event.getUser().getEmail());
        assertEquals(firstName, event.getUser().getName());

        String userName = "Isaac";
        event.setUserName(userName);
        assertEquals(userId, event.getUser().getId());
        assertEquals(userEmail, event.getUser().getEmail());
        assertEquals(userName, event.getUser().getName());
    }

    @Test
    public void testBuilderMetaData() {
        Event.Builder builder = new Event.Builder(config,
            new RuntimeException("foo"), null,
            Thread.currentThread(), false, new Metadata());

        assertNotNull(builder.metaData(new Metadata()).build());

        Metadata metaData = new Metadata();
        metaData.addMetadata("foo", "bar", true);

        Event event = builder.metaData(metaData).build();
        Map<String, Object> foo = (Map<String, Object>) event.getMetadata("foo", null);
        assertEquals(1, foo.size());
    }

    @Test
    public void testErrorMetaData() {
        event.addMetadata("rocks", "geode", "a shiny mineral");
        Map<String, Object> rocks = (Map<String, Object>) event.getMetadata("rocks", null);
        assertNotNull(rocks);

        event.clearMetadata("rocks", null);
        assertFalse(rocks.isEmpty());
        assertNull(event.getMetadata("rocks", null));
    }
}
