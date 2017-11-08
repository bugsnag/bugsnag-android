package com.bugsnag.android;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

public class SessionTrackerTest {

    private static final String ACTIVITY_NAME = "test";

    private SessionTracker sessionTracker;
    private User user;
    private Configuration configuration;

    @Before
    public void setUp() throws Exception {
        configuration = new Configuration("test");
        sessionTracker = new SessionTracker(configuration);
        configuration.setAutoCaptureSessions(true);
        user = new User();
    }

    @Test
    public void startNewSession() throws Exception {
        assertNotNull(sessionTracker);
        assertNull(sessionTracker.getCurrentSession());
        Date date = new Date();
        sessionTracker.startNewSession(date, user);

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
        sessionTracker.startNewSession(date, user);
        assertNull(sessionTracker.getCurrentSession());

        configuration.setAutoCaptureSessions(true);
        sessionTracker.startNewSession(date, user);
        assertNotNull(sessionTracker.getCurrentSession());
    }

    @Test
    public void testUniqueSessionIds() throws Exception {
        sessionTracker.startNewSession(new Date(), user);
        Session firstSession = sessionTracker.getCurrentSession();

        sessionTracker.startNewSession(new Date(), user);
        Session secondSession = sessionTracker.getCurrentSession();
        assertNotEquals(firstSession, secondSession);
    }

    @Test
    public void testIncrementCounts() throws Exception {
        sessionTracker.startNewSession(new Date(), user);
        sessionTracker.incrementHandledError();
        sessionTracker.incrementHandledError();
        sessionTracker.incrementUnhandledError();
        sessionTracker.incrementUnhandledError();
        sessionTracker.incrementUnhandledError();

        Session session = sessionTracker.getCurrentSession();
        assertNotNull(session);
        assertEquals(2, session.getHandledCount());
        assertEquals(3, session.getUnhandledCount());

        sessionTracker.startNewSession(new Date(), user);
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
    public void testZeroSessionTimeout() throws Exception {
        sessionTracker = new SessionTracker(configuration, 0);

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
        sessionTracker = new SessionTracker(configuration, 100);

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
