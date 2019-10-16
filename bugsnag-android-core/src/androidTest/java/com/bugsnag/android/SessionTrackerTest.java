package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateSessionStore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class SessionTrackerTest {

    private static final String ACTIVITY_NAME = "test";

    private SessionTracker sessionTracker;
    private User user;
    private Configuration configuration;
    private Client client;
    private ImmutableConfig immutableConfig;

    /**
     * Configures a session tracker that automatically captures sessions
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setDelivery(BugsnagTestUtils.generateDelivery());
        client = generateClient();
        immutableConfig = BugsnagTestUtils.generateImmutableConfig();
        sessionTracker = new SessionTracker(immutableConfig,
                configuration, client, generateSessionStore());
        configuration.setAutoTrackSessions(true);
        user = new User();
    }

    @After
    public void tearDown() {
        client.close();
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
        configuration.setAutoTrackSessions(false);

        Date date = new Date();
        sessionTracker.startNewSession(date, user, true);
        assertNotNull(sessionTracker.getCurrentSession());

        configuration.setAutoTrackSessions(true);
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
        sessionTracker.incrementHandledAndCopy();
        sessionTracker.incrementHandledAndCopy();
        sessionTracker.incrementUnhandledAndCopy();
        sessionTracker.incrementUnhandledAndCopy();
        sessionTracker.incrementUnhandledAndCopy();

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
        assertNotNull(sessionTracker.isInForeground());
        assertNull(sessionTracker.getCurrentSession());
        assertNull(sessionTracker.getContextActivity());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, System.currentTimeMillis());
        assertNotNull(sessionTracker.isInForeground());
        Session firstSession = sessionTracker.getCurrentSession();
        assertNotNull(firstSession);

        sessionTracker.updateForegroundTracker("other", true, System.currentTimeMillis());
        assertNotNull(sessionTracker.isInForeground());
        assertEquals(firstSession, sessionTracker.getCurrentSession());
        assertEquals("other", sessionTracker.getContextActivity());

        sessionTracker.updateForegroundTracker("other", false, System.currentTimeMillis());
        assertNotNull(sessionTracker.isInForeground());
        assertEquals(ACTIVITY_NAME, sessionTracker.getContextActivity());
    }

    @Test
    public void testZeroSessionTimeout() throws Exception {
        sessionTracker = new SessionTracker(immutableConfig, configuration, client,
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
        sessionTracker = new SessionTracker(immutableConfig, configuration, client,
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
    public void startSessionAutoCaptureEnabled() {
        assertNull(sessionTracker.getCurrentSession());
        sessionTracker.startNewSession(new Date(), user, false);
        assertNotNull(sessionTracker.getCurrentSession());
    }

    @Test
    public void startSessionAutoCaptureDisabled() {
        configuration.setAutoTrackSessions(false);
        assertNull(sessionTracker.getCurrentSession());
        sessionTracker.startNewSession(new Date(), user, false);
        assertNotNull(sessionTracker.getCurrentSession());
    }
}
