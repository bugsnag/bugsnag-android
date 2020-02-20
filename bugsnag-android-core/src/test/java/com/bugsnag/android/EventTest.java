package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class EventTest {

    private final HandledState handledState
            = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION);
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
        HandledState handledState = this.handledState;
        event = new Event(testException, config, handledState);
    }

    @Test
    public void checkExceptionMessageNullity() {
        Event err = new Event(new RuntimeException(), config, handledState);
        assertNull(err.getErrors().get(0).getErrorMessage());
    }

    @Test
    public void testExceptionName() {
        Event err = new Event(new RuntimeException("whoops"), config, handledState);
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
        Event testEvent = new Event(testRuntimeException, config, handledState);
        Throwable outException = testEvent.getOriginalError();
        assertEquals(testRuntimeException, outException);
    }

    @Test
    public void testIsUnhandled() {
        final Event logEvent = new Event(testException, config,
            HandledState.newInstance(HandledState.REASON_LOG));
        final Event anrEvent = new Event(testException, config,
            HandledState.newInstance(HandledState.REASON_ANR));
        final Event handledEvent = new Event(testException, config,
            HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION));
        final Event rejectionEvent = new Event(testException, config,
            HandledState.newInstance(HandledState.REASON_PROMISE_REJECTION));
        final Event strictEvent = new Event(testException, config,
            HandledState.newInstance(HandledState.REASON_STRICT_MODE, Severity.WARNING, "Hello"));
        final Event unhandledEvent = new Event(testException, config,
            HandledState.newInstance(HandledState.REASON_UNHANDLED_EXCEPTION));
        final Event userEvent = new Event(testException, config,
            HandledState.newInstance(HandledState.REASON_USER_SPECIFIED));
        final Event callbackEvent = new Event(testException, config,
            HandledState.newInstance(HandledState.REASON_CALLBACK_SPECIFIED));

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
        Event testEvent = new Event(testRuntimeException, config, handledState);
        List<Error> errors = testEvent.getErrors();

        // First error should match the testException
        assertEquals(testRuntimeException.getClass().getName(), errors.get(0).getErrorClass());
        assertEquals(testRuntimeException.getMessage(), errors.get(0).getErrorMessage());

        String fakeErrorType = "CustomException";
        String fakeErrorMessage = "This is not a real error";
        List<Stackframe> fakeFrames = new ArrayList<>();
        List<Error> secondErrorsList = new ArrayList<>();

        Error secondError = new Error(fakeErrorType, fakeErrorMessage, fakeFrames);
        secondErrorsList.add(secondError);
        testEvent.setErrors(secondErrorsList);

        // Verify the first error is now our custom exception
        errors = testEvent.getErrors();
        assertEquals(secondError, errors.get(0));
    }
}
