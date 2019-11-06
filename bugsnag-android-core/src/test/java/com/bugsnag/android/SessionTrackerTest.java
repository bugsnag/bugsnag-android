package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class SessionTrackerTest {

    private static final String ACTIVITY_NAME = "test";

    private SessionTracker sessionTracker;
    private User user;
    private Configuration configuration;
    private ImmutableConfig immutableConfig;

    @Mock
    Client client;

    @Mock
    AppData appData;

    @Mock
    DeviceData deviceData;

    @Mock
    Context context;

    @Mock
    ActivityManager activityManager;

    @Mock
    SessionStore sessionStore;

    /**
     * Configures a session tracker that automatically captures sessions
     */
    @Before
    public void setUp() {
        when(client.getAppContext()).thenReturn(context);
        when(client.getAppData()).thenReturn(appData);
        when(client.getDeviceData()).thenReturn(deviceData);
        when(context.getSystemService("activity")).thenReturn(activityManager);

        configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setDelivery(BugsnagTestUtils.generateDelivery());
        immutableConfig = BugsnagTestUtils.generateImmutableConfig();
        sessionTracker = new SessionTracker(immutableConfig, configuration.callbackState,
                client, sessionStore, NoopLogger.INSTANCE);
        configuration.setAutoTrackSessions(true);
        user = new User(null, null, null);
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void startNewSession() {
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
    public void startSessionDisabled() {
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
    public void testUniqueSessionIds() {
        sessionTracker.startNewSession(new Date(), user, false);
        Session firstSession = sessionTracker.getCurrentSession();

        sessionTracker.startNewSession(new Date(), user, false);
        Session secondSession = sessionTracker.getCurrentSession();
        assertNotEquals(firstSession, secondSession);
    }

    @Test
    public void testIncrementCounts() {
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
    public void testBasicInForeground() {
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
    public void testZeroSessionTimeout() {
        sessionTracker = new SessionTracker(immutableConfig, configuration.callbackState, client,
            0, sessionStore, NoopLogger.INSTANCE);

        long now = System.currentTimeMillis();
        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now);
        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, false, now);
        Session firstSession = sessionTracker.getCurrentSession();
        assertNotNull(firstSession);

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, now);
        assertNotEquals(firstSession, sessionTracker.getCurrentSession());
    }

    @Test
    public void testSessionTimeout() {
        sessionTracker = new SessionTracker(immutableConfig, configuration.callbackState, client,
            100, sessionStore, NoopLogger.INSTANCE);

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
