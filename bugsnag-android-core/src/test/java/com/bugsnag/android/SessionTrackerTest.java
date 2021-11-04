package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateDevice;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import com.bugsnag.android.internal.ImmutableConfig;

import android.app.ActivityManager;
import android.content.Context;

import androidx.annotation.NonNull;

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
    private final BackgroundTaskService bgTaskService = new BackgroundTaskService();

    @Mock
    Client client;

    @Mock
    ImmutableConfig cfg;

    @Mock
    AppDataCollector appDataCollector;

    @Mock
    DeviceDataCollector deviceDataCollector;

    @Mock
    Context context;

    @Mock
    ActivityManager activityManager;

    @Mock
    SessionStore sessionStore;

    @Mock
    App app;

    ContextState contextState;

    /**
     * Configures a session tracker that automatically captures sessions
     */
    @Before
    public void setUp() {
        contextState = new ContextState();
        when(client.getNotifier()).thenReturn(new Notifier());
        when(client.getAppContext()).thenReturn(context);
        when(client.getAppDataCollector()).thenReturn(appDataCollector);
        when(client.getConfig()).thenReturn(cfg);
        when(client.getContextState()).thenReturn(contextState);
        when(appDataCollector.generateApp()).thenReturn(app);
        when(client.getDeviceDataCollector()).thenReturn(deviceDataCollector);
        when(deviceDataCollector.generateDevice()).thenReturn(generateDevice());
        when(context.getSystemService("activity")).thenReturn(activityManager);

        configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setDelivery(BugsnagTestUtils.generateDelivery());
        immutableConfig = BugsnagTestUtils.generateImmutableConfig();
        sessionTracker = new SessionTracker(immutableConfig,
                configuration.impl.callbackState, client, sessionStore, NoopLogger.INSTANCE,
                bgTaskService);
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
        assertNull(sessionTracker.getCurrentSession());
        assertNull(sessionTracker.getContextActivity());
        assertNull(contextState.getContext());

        sessionTracker.updateForegroundTracker(ACTIVITY_NAME, true, System.currentTimeMillis());
        Session firstSession = sessionTracker.getCurrentSession();
        assertNotNull(firstSession);

        sessionTracker.updateForegroundTracker("other", true, System.currentTimeMillis());
        assertEquals(firstSession, sessionTracker.getCurrentSession());
        assertEquals("other", sessionTracker.getContextActivity());
        assertEquals("other", contextState.getContext());

        sessionTracker.updateForegroundTracker("other", false, System.currentTimeMillis());
        assertEquals(ACTIVITY_NAME, sessionTracker.getContextActivity());
        assertEquals(ACTIVITY_NAME, contextState.getContext());
    }

    @Test
    public void testZeroSessionTimeout() {
        CallbackState callbackState = configuration.impl.callbackState;
        sessionTracker = new SessionTracker(immutableConfig, callbackState, client,
            0, sessionStore, NoopLogger.INSTANCE, bgTaskService);

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
        CallbackState callbackState = configuration.impl.callbackState;
        sessionTracker = new SessionTracker(immutableConfig, callbackState, client,
            100, sessionStore, NoopLogger.INSTANCE, bgTaskService);

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

    @Test
    public void blockSessionInCallback() {
        CallbackState callbackState = configuration.impl.callbackState;
        callbackState.addOnSession(new OnSessionCallback() {
            @Override
            public boolean onSession(@NonNull Session session) {
                return false;
            }
        });

        Date date = new Date();
        sessionTracker.startNewSession(date, user, false);
        assertNull(sessionTracker.getCurrentSession());
    }
}
