package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.bugsnag.android.internal.ImmutableConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

@SuppressWarnings("ConstantConditions")
public class EventFacadeTest {

    private Event event;
    private InterceptingLogger logger;
    private ImmutableConfig config;

    /**
     * Constructs an event for testing the wrapper interface
     */
    @Before
    public void setUp() {
        logger = new InterceptingLogger();
        config = BugsnagTestUtils.generateImmutableConfig();
        event = new Event(new RuntimeException(), config,
                SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION), logger);
    }

    @Test
    public void apiKeyValid() {
        assertEquals(config.getApiKey(), event.getApiKey());
        event.setApiKey("5d1ec5bd39a74caa1267142706a7fb21");
        assertEquals("5d1ec5bd39a74caa1267142706a7fb21", event.getApiKey());
    }

    @Test
    public void apiKeyInvalid() {
        event.setApiKey(null);
        assertEquals(config.getApiKey(), event.getApiKey());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void severityValid() {
        assertEquals(Severity.WARNING, event.getSeverity());
        event.setSeverity(Severity.INFO);
        assertEquals(Severity.INFO, event.getSeverity());
    }

    @Test
    public void severityInvalid() {
        assertEquals(Severity.WARNING, event.getSeverity());
        event.setSeverity(null);
        assertEquals(Severity.WARNING, event.getSeverity());
        assertNotNull(logger.getMsg());
    }

    @Test
    public void groupingHashValid() {
        event.setGroupingHash(null);
        assertNull(event.getGroupingHash());
        event.setGroupingHash("crash-id");
        assertEquals("crash-id", event.getGroupingHash());
    }

    @Test
    public void contextValid() {
        event.setContext(null);
        assertNull(event.getContext());
        event.setContext("MyActivity");
        assertEquals("MyActivity", event.getContext());
    }

    @Test
    public void addMetadataValid() {
        Map<String, Boolean> map = Collections.singletonMap("test", true);
        event.addMetadata("foo", map);
        assertEquals(map, event.getMetadata("foo"));
    }

    @Test
    public void addMetadataInvalid1() {
        event.addMetadata("foo", null);
        assertNull(event.getMetadata("foo"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValueValid() {
        event.addMetadata("foo", "test", true);
        assertTrue((Boolean) event.getMetadata("foo", "test"));
    }

    @Test
    public void addMetadataValueInvalid1() {
        event.addMetadata(null, "test", true);
        assertNull(event.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void addMetadataValueInvalid2() {
        event.addMetadata("foo", null, true);
        assertNull(event.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValid() {
        event.addMetadata("foo", "test", true);
        event.clearMetadata("foo");
        assertNull(event.getMetadata("foo"));
    }

    @Test
    public void clearMetadataInvalid() {
        event.addMetadata("foo", "test", true);
        event.clearMetadata(null);
        assertTrue((Boolean) event.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValueValid() {
        event.addMetadata("foo", "test", true);
        event.clearMetadata("foo", "test");
        assertNull(event.getMetadata("foo", "test"));
    }

    @Test
    public void clearMetadataValueInvalid1() {
        event.addMetadata("foo", "test", true);
        event.clearMetadata(null, "test");
        assertTrue((Boolean) event.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void clearMetadataValueInvalid2() {
        event.addMetadata("foo", "test", true);
        event.clearMetadata("foo", null);
        assertTrue((Boolean) event.getMetadata("foo", "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValid() {
        event.addMetadata("foo", "test", true);
        assertTrue((Boolean) event.getMetadata("foo", "test"));
    }

    @Test
    public void getMetadataInvalid() {
        assertNull(event.getMetadata(null));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValueValid() {
        event.addMetadata("foo", "test", true);
        assertTrue((Boolean) event.getMetadata("foo", "test"));
    }

    @Test
    public void getMetadataValueInvalid1() {
        event.addMetadata("foo", "test", true);
        assertNull(event.getMetadata(null, "test"));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void getMetadataValueInvalid2() {
        event.addMetadata("foo", "test", true);
        assertNull(event.getMetadata("foo", null));
        assertNotNull(logger.getMsg());
    }

    @Test
    public void setUserValid() {
        event.setUser(null, null, null);
        assertEquals(new User(null, null, null), event.getUser());
    }

    @Test
    public void unhandledValid() {
        assertFalse(event.isUnhandled());
        event.setUnhandled(true);
        assertTrue(event.isUnhandled());
        event.setUnhandled(false);
        assertFalse(event.isUnhandled());
    }

    @Test
    public void addThread() {
        Thread newThread = event.addThread(123L, "Magic Thread");
        assertSame(newThread, event.getThreads().get(event.getThreads().size() - 1));
        assertEquals("123", newThread.getId());
        assertEquals("Magic Thread", newThread.getName());
        assertEquals(ErrorType.ANDROID, newThread.getType());
        assertEquals(Thread.State.RUNNABLE, newThread.getState());
        assertEquals(0, newThread.getStacktrace().size());
    }

    @Test
    public void addError() {
        Error newError = event.addError("ErrorClass", "No error message");
        assertSame(newError, event.getErrors().get(event.getErrors().size() - 1));
        assertEquals("ErrorClass", newError.getErrorClass());
        assertEquals("No error message", newError.getErrorMessage());
        assertEquals(ErrorType.ANDROID, newError.getType());
        assertEquals(0, newError.getStacktrace().size());
    }

    @Test
    public void addErrorWithType() {
        Error newError = event.addError("ErrorClass", "No error message", ErrorType.DART);
        assertSame(newError, event.getErrors().get(event.getErrors().size() - 1));
        assertEquals("ErrorClass", newError.getErrorClass());
        assertEquals("No error message", newError.getErrorMessage());
        assertEquals(ErrorType.DART, newError.getType());
        assertEquals(0, newError.getStacktrace().size());
    }

    @Test
    public void addErrorNullThrowable() {
        Error newError = event.addError(null);
        assertSame(newError, event.getErrors().get(event.getErrors().size() - 1));
        assertEquals("null", newError.getErrorClass());
        assertNull(newError.getErrorMessage());
        assertEquals(ErrorType.ANDROID, newError.getType());
        assertEquals(0, newError.getStacktrace().size());
    }

    @Test
    public void addThreadWithNulls() {
        Thread newThread = event.addThread(null, null);
        assertSame(newThread, event.getThreads().get(event.getThreads().size() - 1));
        assertEquals("null", newThread.getId());
        assertEquals("null", newThread.getName());
        assertEquals(ErrorType.ANDROID, newThread.getType());
        assertEquals(Thread.State.RUNNABLE, newThread.getState());
        assertEquals(0, newThread.getStacktrace().size());
    }

    @Test
    public void addBadError() {
        Error newError = event.addError(null, null);
        assertSame(newError, event.getErrors().get(event.getErrors().size() - 1));
        assertEquals("null", newError.getErrorClass());
        assertNull(newError.getErrorMessage());
        assertEquals(ErrorType.ANDROID, newError.getType());
        assertEquals(0, newError.getStacktrace().size());
    }
}
