package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateSessionStore;
import static com.bugsnag.android.BugsnagTestUtils.generateSessionTrackingApiClient;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class SessionTrackerTest {

    private static final String ACTIVITY_NAME = "test";

    private SessionTracker sessionTracker;
    private User user;
    private Configuration configuration;

    /**
     * Configures a session tracker that automatically captures sessions
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        configuration = new Configuration("test");
        configuration.setDelivery(BugsnagTestUtils.generateDelivery());
        sessionTracker
            = new SessionTracker(configuration, generateClient(), generateSessionStore());
        configuration.setAutoCaptureSessions(true);
        user = new User();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
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
        sessionTracker.startNewSession(date, user, true);
        assertNotNull(sessionTracker.getCurrentSession());

        configuration.setAutoCaptureSessions(true);
        sessionTracker.startNewSession(date, user, false);
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
        assertNull(sessionTracker.getContextActivity());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, System.currentTimeMillis());
        assertTrue(sessionTracker.isInForeground());
        Session firstSession = sessionTracker.getCurrentSession();
        assertNotNull(firstSession);

        sessionTracker.updateForegroundTracker("other", true, System.currentTimeMillis());
        assertTrue(sessionTracker.isInForeground());
        assertEquals(firstSession, sessionTracker.getCurrentSession());
        assertEquals("other", sessionTracker.getContextActivity());

        sessionTracker.updateForegroundTracker("other", false, System.currentTimeMillis());
        assertTrue(sessionTracker.isInForeground());
        assertEquals(ACTIVITY_NAME, sessionTracker.getContextActivity());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, System.currentTimeMillis());
        assertFalse(sessionTracker.isInForeground());
        assertEquals(firstSession, sessionTracker.getCurrentSession());
        assertNull(sessionTracker.getContextActivity());
    }

    @Test
    public void testInForegroundDuration() throws Exception {
        long now = System.currentTimeMillis();
        sessionTracker = new SessionTracker(configuration, generateClient(),
            0, generateSessionStore());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        assertEquals(0, sessionTracker.getDurationInForegroundMs(now));

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now);
        assertEquals(0, sessionTracker.getDurationInForegroundMs(now));

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now);
        assertEquals(100, sessionTracker.getDurationInForegroundMs(now + 100));

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        assertEquals(200, sessionTracker.getDurationInForegroundMs(now + 200));

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        assertEquals(0, sessionTracker.getDurationInForegroundMs(now + 300));
    }

    @Test
    public void testZeroSessionTimeout() throws Exception {
        sessionTracker = new SessionTracker(configuration, generateClient(),
            0, generateSessionStore());

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
        sessionTracker = new SessionTracker(configuration, generateClient(),
            100, generateSessionStore());

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

    @Test
    public void startSessionNoEndpoint() throws Exception {
        assertNull(sessionTracker.getCurrentSession());
        configuration.setEndpoints("http://localhost:1234", "");
        sessionTracker.startNewSession(new Date(), user, false);
        assertNull(sessionTracker.getCurrentSession());
    }

    @Test
    public void startSessionAutoCaptureEnabled() {
        assertNull(sessionTracker.getCurrentSession());
        sessionTracker.startNewSession(new Date(), user, false);
        assertNotNull(sessionTracker.getCurrentSession());
    }

    @Test
    public void startSessionAutoCaptureDisabled() {
        configuration.setAutoCaptureSessions(false);
        assertNull(sessionTracker.getCurrentSession());
        sessionTracker.startNewSession(new Date(), user, false);
        assertNotNull(sessionTracker.getCurrentSession());
    }
}
