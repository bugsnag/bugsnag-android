package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bugsnag.android.internal.ImmutableConfig;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class EventTest {

    private final SeverityReason severityReason
            = SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION);
    private ImmutableConfig config;
    private RuntimeException testException;
    private Event event;

    /**
     * Generates a new default event for use by tests
     *
     */
    @Before
    public void setUp() {
        config = BugsnagTestUtils.generateImmutableConfig();
        testException = new RuntimeException("Example message");
        SeverityReason severityReason = this.severityReason;
        event = new Event(testException, config, severityReason, NoopLogger.INSTANCE);
    }

    @Test
    public void checkExceptionMessageNullity() {
        Event err = new Event(new RuntimeException(), config, severityReason, NoopLogger.INSTANCE);
        assertNull(err.getErrors().get(0).getErrorMessage());
    }

    @Test
    public void testExceptionName() {
        RuntimeException exc = new RuntimeException("whoops");
        Event err = new Event(exc, config, severityReason, NoopLogger.INSTANCE);
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

    @Test
    public void testEventGetDevice() {
        DeviceWithState inDevice = BugsnagTestUtils.generateDeviceWithState();
        event.setDevice(inDevice);
        Device outDevice = event.getDevice();
        assertEquals(inDevice, outDevice);
    }

    @Test
    public void testGetOriginalError() {
        RuntimeException testRuntimeException = new RuntimeException("Something went wrong");
        Event testEvent = new Event(testRuntimeException, config,
                severityReason, NoopLogger.INSTANCE);
        Throwable outException = testEvent.getOriginalError();
        assertEquals(testRuntimeException, outException);
    }

    @Test
    public void testIsUnhandled() {
        final Event logEvent = new Event(testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_LOG),
            NoopLogger.INSTANCE);
        final Event anrEvent = new Event(testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_ANR),
            NoopLogger.INSTANCE);
        final Event handledEvent = new Event(testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
            NoopLogger.INSTANCE);
        final Event rejectionEvent = new Event(testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_PROMISE_REJECTION),
            NoopLogger.INSTANCE);
        final Event strictEvent = new Event(testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_STRICT_MODE, Severity.WARNING,
                    "Hello"), NoopLogger.INSTANCE);
        final Event unhandledEvent = new Event(testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_UNHANDLED_EXCEPTION),
            NoopLogger.INSTANCE);
        final Event userEvent = new Event(testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_USER_SPECIFIED),
            NoopLogger.INSTANCE);
        final Event callbackEvent = new Event(testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_CALLBACK_SPECIFIED),
            NoopLogger.INSTANCE);

        assertFalse(logEvent.isUnhandled());
        assertTrue(anrEvent.isUnhandled());
        assertFalse(handledEvent.isUnhandled());
        assertTrue(rejectionEvent.isUnhandled());
        assertTrue(strictEvent.isUnhandled());
        assertTrue(unhandledEvent.isUnhandled());
        assertFalse(userEvent.isUnhandled());
        assertFalse(callbackEvent.isUnhandled());
    }

    @Test
    public void testGetSetErrors() {
        RuntimeException testRuntimeException = new RuntimeException("Something went wrong");
        Event testEvent = new Event(testRuntimeException, config,
                severityReason, NoopLogger.INSTANCE);
        List<Error> errors = testEvent.getErrors();

        // First error should match the testException
        assertEquals(testRuntimeException.getClass().getName(), errors.get(0).getErrorClass());
        assertEquals(testRuntimeException.getMessage(), errors.get(0).getErrorMessage());
    }

    @Test
    public void testIsAnr() {
        RuntimeException exc = new RuntimeException("Something went wrong");
        Event event = new Event(exc, config, severityReason, NoopLogger.INSTANCE);
        assertFalse(event.getImpl().isAnr(event));

        // simulate ANR
        event.getErrors().get(0).setErrorClass("ANR");
        assertTrue(event.getImpl().isAnr(event));

        // clear all errors
        event.getErrors().clear();
        assertFalse(event.getImpl().isAnr(event));
    }

    @Test
    public void testSeverityReasonType() {
        RuntimeException exc = new RuntimeException("Something went wrong");
        Event event = new Event(exc, config, severityReason, NoopLogger.INSTANCE);
        String severityReasonType = event.getImpl().getSeverityReasonType();
        assertEquals(SeverityReason.REASON_HANDLED_EXCEPTION, severityReasonType);
    }

    @Test
    public void testSeverityReasonInternalOverload() {
        RuntimeException exc = new RuntimeException("Something went wrong");
        Event event = new Event(exc, config, severityReason, NoopLogger.INSTANCE);

        String severityReasonType = event.getImpl().getSeverityReasonType();
        assertEquals(SeverityReason.REASON_HANDLED_EXCEPTION, severityReasonType);
        assertEquals(Severity.WARNING, event.getSeverity());

        event.updateSeverityInternal(Severity.INFO);
        event.updateSeverityReason(SeverityReason.REASON_STRICT_MODE);
        severityReasonType = event.getImpl().getSeverityReasonType();
        assertEquals(SeverityReason.REASON_STRICT_MODE, severityReasonType);
        assertEquals(Severity.INFO, event.getSeverity());
    }
}
