package com.bugsnag.android;

import android.support.test.InstrumentationRegistry;
import android.util.Pair;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static com.bugsnag.android.BugsnagTestUtils.*;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

public class SessionTrackerTest {

    private static final String ACTIVITY_NAME = "test";
    private static final String FIRST_ACTIVITY = "MyActivity";
    private static final String SECOND_ACTIVITY = "SecondActivity";
    private static final String FIRST_CB = "onCreate";
    private static final String SECOND_CB = "onStart";

    private SessionTracker sessionTracker;
    private User user;
    private Configuration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = new Configuration("test");
        sessionTracker = new SessionTracker(configuration, generateClient(), generateSessionStore(),
            generateSessionTrackingApiClient(), InstrumentationRegistry.getContext());
        configuration.setAutoCaptureSessions(true);
        user = new User();
    }

    @Test
    public void testLifecycleQueueing() throws Exception {
        sessionTracker = new SessionTracker(configuration, null, generateSessionStore(),
            generateSessionTrackingApiClient(), InstrumentationRegistry.getContext());
        sessionTracker.leaveLifecycleBreadcrumb(FIRST_ACTIVITY, FIRST_CB);
        sessionTracker.leaveLifecycleBreadcrumb(SECOND_ACTIVITY, SECOND_CB);

        assertEquals(2, sessionTracker.breadcrumbQueue.size());

        Pair<String, String> poll = sessionTracker.breadcrumbQueue.poll();
        assertEquals(FIRST_ACTIVITY, poll.first);
        assertEquals(FIRST_CB, poll.second);

        poll = sessionTracker.breadcrumbQueue.poll();
        assertEquals(SECOND_ACTIVITY, poll.first);
        assertEquals(SECOND_CB, poll.second);
    }

    @Test
    public void testNullClientUpdate() throws Exception {
        // shouldn't throw npe attempting to access client
        sessionTracker = new SessionTracker(configuration, null, generateSessionStore(),
            generateSessionTrackingApiClient(), InstrumentationRegistry.getContext());
        sessionTracker.updateForegroundTracker(FIRST_ACTIVITY, true, System.currentTimeMillis());
    }

    @Test
    public void testLifecycleLogging() throws Exception {
        sessionTracker.leaveLifecycleBreadcrumb(FIRST_ACTIVITY, FIRST_CB);
        sessionTracker.leaveLifecycleBreadcrumb(SECOND_ACTIVITY, SECOND_CB);
        assertTrue(sessionTracker.breadcrumbQueue.isEmpty());
    }

    @Test
    public void startNewSession() throws Exception {
        assertNotNull(sessionTracker);
        assertNull(sessionTracker.getCurrentSession());
        Date date = new Date();
        sessionTracker.startNewSession(date, user, false);

        Session newSession = sessionTracker.getCurrentSession();
        assertNotNull(newSession);
        assertNotNull(newSession.getId());
        assertEquals(date.getTime(), newSession.getStartedAt().getTime());
        assertNotNull(newSession.getUser());
    }

    @Test
    public void startSessionDisabled() throws Exception {
        assertNull(sessionTracker.getCurrentSession());
        configuration.setAutoCaptureSessions(false);

        Date date = new Date();
        sessionTracker.startNewSession(date, user, false);
        assertTrue(sessionTracker.sessionQueue.isEmpty());
        assertNotNull(sessionTracker.getCurrentSession());

        configuration.setAutoCaptureSessions(true);
        sessionTracker.startNewSession(date, user, false);
        assertEquals(1, sessionTracker.sessionQueue.size());
        assertNotNull(sessionTracker.getCurrentSession());
    }

    @Test
    public void testUniqueSessionIds() throws Exception {
        sessionTracker.startNewSession(new Date(), user, false);
        Session firstSession = sessionTracker.getCurrentSession();

        sessionTracker.startNewSession(new Date(), user, false);
        Session secondSession = sessionTracker.getCurrentSession();
        assertNotEquals(firstSession, secondSession);
    }

    @Test
    public void testIncrementCounts() throws Exception {
        sessionTracker.startNewSession(new Date(), user, false);
        sessionTracker.incrementHandledError();
        sessionTracker.incrementHandledError();
        sessionTracker.incrementUnhandledError();
        sessionTracker.incrementUnhandledError();
        sessionTracker.incrementUnhandledError();

        Session session = sessionTracker.getCurrentSession();
        assertNotNull(session);
        assertEquals(2, session.getHandledCount());
        assertEquals(3, session.getUnhandledCount());

        sessionTracker.startNewSession(new Date(), user, false);
        Session nextSession = sessionTracker.getCurrentSession();
        assertEquals(0, nextSession.getHandledCount());
        assertEquals(0, nextSession.getUnhandledCount());
    }

    @Test
    public void testBasicInForeground() throws Exception {
        assertFalse(sessionTracker.isInForeground());
        assertNull(sessionTracker.getCurrentSession());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, System.currentTimeMillis());
        assertTrue(sessionTracker.isInForeground());
        Session firstSession = sessionTracker.getCurrentSession();
        assertNotNull(firstSession);

        sessionTracker.updateForegroundTracker("other", true, System.currentTimeMillis());
        assertTrue(sessionTracker.isInForeground());
        assertEquals(firstSession, sessionTracker.getCurrentSession());

        sessionTracker.updateForegroundTracker("other", false, System.currentTimeMillis());
        assertTrue(sessionTracker.isInForeground());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, System.currentTimeMillis());
        assertFalse(sessionTracker.isInForeground());
        assertEquals(firstSession, sessionTracker.getCurrentSession());
    }

    @Test
    public void testInForegroundDuration() throws Exception {
        long now = System.currentTimeMillis();
        sessionTracker = new SessionTracker(configuration, generateClient(), 0, generateSessionStore(),
            generateSessionTrackingApiClient(), InstrumentationRegistry.getContext());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        assertEquals(0, sessionTracker.getDurationInForeground(now));

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now);
        assertEquals(0, sessionTracker.getDurationInForeground(now));

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now);
        assertEquals(100, sessionTracker.getDurationInForeground(now + 100));

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        assertEquals(0, sessionTracker.getDurationInForeground(now + 200));
    }

    @Test
    public void testZeroSessionTimeout() throws Exception {
        sessionTracker = new SessionTracker(configuration, generateClient(), 0, generateSessionStore(),
            generateSessionTrackingApiClient(), InstrumentationRegistry.getContext());

        long now = System.currentTimeMillis();
        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now);
        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        Session firstSession = sessionTracker.getCurrentSession();
        assertNotNull(firstSession);

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now);
        assertNotEquals(firstSession, sessionTracker.getCurrentSession());
    }

    @Test
    public void testSessionTimeout() throws Exception {
        sessionTracker = new SessionTracker(configuration, generateClient(), 100, generateSessionStore(),
            generateSessionTrackingApiClient(), InstrumentationRegistry.getContext());

        long now = System.currentTimeMillis();
        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now);
        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        Session firstSession = sessionTracker.getCurrentSession();
        assertNotNull(firstSession);

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now + 5);
        assertEquals(firstSession, sessionTracker.getCurrentSession());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now + 99);
        assertEquals(firstSession, sessionTracker.getCurrentSession());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now + 100);
        assertNotEquals(firstSession, sessionTracker.getCurrentSession());
    }

}
