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
public class ErrorTest {

    private ImmutableConfig config;
    private Error error;

    /**
     * Generates a new default error for use by tests
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        config = BugsnagTestUtils.generateImmutableConfig();
        RuntimeException exception = new RuntimeException("Example message");
        error = new Error.Builder(config, exception, null,
            Thread.currentThread(), false, new MetaData()).build();
    }

    @Test
    public void checkExceptionMessageNullity() throws Exception {
        String msg = "Foo";
        Error err = new Error.Builder(config,
            new RuntimeException(msg), null,
            Thread.currentThread(), false, new MetaData()).build();
        assertEquals(msg, err.getExceptionMessage());

        err = new Error.Builder(config,
            new RuntimeException(), null,
            Thread.currentThread(), false, new MetaData()).build();
        assertEquals("", err.getExceptionMessage());
    }

    @Test
    public void testNullSeverity() throws Exception {
        error.setSeverity(null);
        assertEquals(Severity.WARNING, error.getSeverity());
    }

    @Test
    public void testBugsnagExceptionName() throws Exception {
        BugsnagException exception = new BugsnagException("Busgang", "exceptional",
            new StackTraceElement[]{});
        Error err = new Error.Builder(config,
            exception, null, Thread.currentThread(), false, new MetaData()).build();
        assertEquals("Busgang", err.getExceptionName());
    }

    @Test
    public void testNullContext() throws Exception {
        error.setContext(null);
        error.setAppData(null);
        assertNull(error.getContext());
    }

    @Test
    public void testSetUser() throws Exception {
        String firstId = "123";
        String firstEmail = "fake@example.com";
        String firstName = "Bob Swaggins";
        error.setUser(firstId, firstEmail, firstName);

        assertEquals(firstId, error.getUser().getId());
        assertEquals(firstEmail, error.getUser().getEmail());
        assertEquals(firstName, error.getUser().getName());

        String userId = "foo";
        error.setUserId(userId);
        assertEquals(userId, error.getUser().getId());
        assertEquals(firstEmail, error.getUser().getEmail());
        assertEquals(firstName, error.getUser().getName());

        String userEmail = "another@example.com";
        error.setUserEmail(userEmail);
        assertEquals(userId, error.getUser().getId());
        assertEquals(userEmail, error.getUser().getEmail());
        assertEquals(firstName, error.getUser().getName());

        String userName = "Isaac";
        error.setUserName(userName);
        assertEquals(userId, error.getUser().getId());
        assertEquals(userEmail, error.getUser().getEmail());
        assertEquals(userName, error.getUser().getName());
    }

    @Test
    public void testBuilderMetaData() {
        Error.Builder builder = new Error.Builder(config,
            new RuntimeException("foo"), null,
            Thread.currentThread(), false, new MetaData());

        assertNotNull(builder.metaData(new MetaData()).build());

        MetaData metaData = new MetaData();
        metaData.addMetadata("foo", "bar", true);

        Error error = builder.metaData(metaData).build();
        Map<String, Object> foo = (Map<String, Object>) error.getMetadata("foo", null);
        assertEquals(1, foo.size());
    }

    @Test
    public void testErrorMetaData() {
        error.addMetadata("rocks", "geode", "a shiny mineral");
        Map<String, Object> rocks = (Map<String, Object>) error.getMetadata("rocks", null);
        assertNotNull(rocks);

        error.clearMetadata("rocks", null);
        assertFalse(rocks.isEmpty());
        assertNull(error.getMetadata("rocks", null));
    }
}
