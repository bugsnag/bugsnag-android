package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class SessionTracker extends BaseObservable {

    private static final int DEFAULT_TIMEOUT_MS = 30000;

    private final Collection<String>
            foregroundActivities = new ConcurrentLinkedQueue<>();
    private final long timeoutMs;

    private final ImmutableConfig configuration;
    private final CallbackState callbackState;
    private final Client client;
    final SessionStore sessionStore;

    // This most recent time an Activity was stopped.
    private final AtomicLong lastExitedForegroundMs = new AtomicLong(0);

    // The first Activity in this 'session' was started at this time.
    private final AtomicLong lastEnteredForegroundMs = new AtomicLong(0);
    private final AtomicReference<Session> currentSession = new AtomicReference<>();
    private final Semaphore flushingRequest = new Semaphore(1);
    private final ForegroundDetector foregroundDetector;
    final Logger logger;

    SessionTracker(ImmutableConfig configuration, CallbackState callbackState,
                   Client client, SessionStore sessionStore, Logger logger) {
        this(configuration, callbackState, client, DEFAULT_TIMEOUT_MS, sessionStore, logger);
    }

    SessionTracker(ImmutableConfig configuration, CallbackState callbackState,
                   Client client, long timeoutMs, SessionStore sessionStore, Logger logger) {
        this.configuration = configuration;
        this.callbackState = callbackState;
        this.client = client;
        this.timeoutMs = timeoutMs;
        this.sessionStore = sessionStore;
        this.foregroundDetector = new ForegroundDetector(client.getAppContext());
        this.logger = logger;
        notifyNdkInForeground();
    }

    /**
     * Starts a new session with the given date and user.
     * <p>
     * A session will only be created if {@link Configuration#getAutoTrackSessions()} returns
     * true.
     *
     * @param date the session start date
     * @param user the session user (if any)
     */
    @Nullable
    @VisibleForTesting
    Session startNewSession(@NonNull Date date, @Nullable User user,
                            boolean autoCaptured) {
        String id = UUID.randomUUID().toString();
        Session session = new Session(id, date, user, autoCaptured, client.getNotifier(), logger);
        currentSession.set(session);
        trackSessionIfNeeded(session);
        return session;
    }

    Session startSession(boolean autoCaptured) {
        return startNewSession(new Date(), client.getUser(), autoCaptured);
    }

    void pauseSession() {
        Session session = currentSession.get();

        if (session != null) {
            session.isPaused.set(true);
            notifyObservers(StateEvent.PauseSession.INSTANCE);
        }
    }

    boolean resumeSession() {
        Session session = currentSession.get();
        boolean resumed;

        if (session == null) {
            session = startSession(false);
            resumed = false;
        } else {
            resumed = session.isPaused.compareAndSet(true, false);
        }

        if (session != null) {
            notifySessionStartObserver(session);
        }
        return resumed;
    }

    private void notifySessionStartObserver(Session session) {
        String startedAt = DateUtils.toIso8601(session.getStartedAt());
        notifyObservers(new StateEvent.StartSession(session.getId(), startedAt,
                session.getHandledCount(), session.getUnhandledCount()));
    }

    /**
     * Cache details of a previously captured session.
     * Append session details to all subsequent reports.
     *
     * @param date           the session start date
     * @param sessionId      the unique session identifier
     * @param user           the session user (if any)
     * @param unhandledCount the number of unhandled events which have occurred during the session
     * @param handledCount   the number of handled events which have occurred during the session
     * @return the session
     */
    @Nullable
    Session registerExistingSession(@Nullable Date date, @Nullable String sessionId,
                                    @Nullable User user, int unhandledCount,
                                    int handledCount) {
        Session session = null;
        if (date != null && sessionId != null) {
            session = new Session(sessionId, date, user, unhandledCount, handledCount,
                    client.getNotifier(), logger);
            notifySessionStartObserver(session);
        } else {
            notifyObservers(StateEvent.PauseSession.INSTANCE);
        }
        currentSession.set(session);
        return session;
    }

    /**
     * Determines whether or not a session should be tracked. If this is true, the session will be
     * stored and sent to the Bugsnag API, otherwise no action will occur in this method.
     *
     * @param session the session
     */
    private void trackSessionIfNeeded(final Session session) {
        boolean notifyForRelease = configuration.shouldNotifyForReleaseStage();

        session.setApp(client.getAppDataCollector().generateApp());
        session.setDevice(client.getDeviceDataCollector().generateDevice());
        boolean deliverSession = callbackState.runOnSessionTasks(session, logger);

        if (deliverSession && notifyForRelease
                && (configuration.getAutoTrackSessions() || !session.isAutoCaptured())
                && session.isTracked().compareAndSet(false, true)) {
            notifySessionStartObserver(session);

            try {
                Async.run(new Runnable() {
                    @Override
                    public void run() {
                        //FUTURE:SM It would be good to optimise this
                        flushStoredSessions();

                        try {
                            DeliveryStatus deliveryStatus = deliverSessionPayload(session);

                            switch (deliveryStatus) {
                                case UNDELIVERED:
                                    logger.w("Storing session payload for future delivery");
                                    sessionStore.write(session);
                                    break;
                                case FAILURE:
                                    logger.w("Dropping invalid session tracking payload");
                                    break;
                                case DELIVERED:
                                default:
                                    break;
                            }
                        } catch (Exception exception) {
                            logger.w("Session tracking payload failed", exception);
                        }
                    }
                });
            } catch (RejectedExecutionException exception) {
                // This is on the current thread but there isn't much else we can do
                sessionStore.write(session);
            }
        }
    }

    @Nullable
    Session getCurrentSession() {
        Session session = currentSession.get();

        if (session != null && !session.isPaused.get()) {
            return session;
        }
        return null;
    }

    /**
     * Increments the unhandled error count on the current session, then returns a deep-copy
     * of the current session.
     *
     * @return a copy of the current session, or null if no session has been started.
     */
    Session incrementUnhandledAndCopy() {
        Session session = getCurrentSession();
        if (session != null) {
            return session.incrementUnhandledAndCopy();
        }
        return null;
    }

    /**
     * Increments the handled error count on the current session, then returns a deep-copy
     * of the current session.
     *
     * @return a copy of the current session, or null if no session has been started.
     */
    Session incrementHandledAndCopy() {
        Session session = getCurrentSession();
        if (session != null) {
            return session.incrementHandledAndCopy();
        }
        return null;
    }

    /**
     * Attempts to flush session payloads stored on disk
     */
    void flushStoredSessions() {
        if (flushingRequest.tryAcquire(1)) {
            try {
                List<File> storedFiles = sessionStore.findStoredFiles();

                for (File storedFile : storedFiles) {
                    flushStoredSession(storedFile);
                }
            } finally {
                flushingRequest.release(1);
            }
        }
    }

    void flushStoredSession(File storedFile) {
        Session payload = new Session(storedFile, client.getNotifier(), logger);

        if (!payload.isV2Payload()) { // collect data here
            payload.setApp(client.getAppDataCollector().generateApp());
            payload.setDevice(client.getDeviceDataCollector().generateDevice());
        }

        DeliveryStatus deliveryStatus = deliverSessionPayload(payload);

        switch (deliveryStatus) {
            case DELIVERED:
                sessionStore.deleteStoredFiles(Collections.singletonList(storedFile));
                break;
            case UNDELIVERED:
                sessionStore.cancelQueuedFiles(Collections.singletonList(storedFile));
                logger.w("Leaving session payload for future delivery");
                break;
            case FAILURE:
                // drop bad data
                logger.w("Deleting invalid session tracking payload");
                sessionStore.deleteStoredFiles(Collections.singletonList(storedFile));
                break;
            default:
                break;
        }
    }

    DeliveryStatus deliverSessionPayload(Session payload) {
        DeliveryParams params = configuration.getSessionApiDeliveryParams();
        Delivery delivery = configuration.getDelivery();
        return delivery.deliver(payload, params);
    }

    void onActivityStarted(String activityName) {
        updateForegroundTracker(activityName, true, System.currentTimeMillis());
    }

    void onActivityStopped(String activityName) {
        updateForegroundTracker(activityName, false, System.currentTimeMillis());
    }

    /**
     * Tracks whether an activity is in the foreground or not.
     * <p>
     * If an activity leaves the foreground, a timeout should be recorded (e.g. 30s), during which
     * no new sessions should be automatically started.
     * <p>
     * If an activity comes to the foreground and is the only foreground activity, a new session
     * should be started, unless the app is within a timeout period.
     *
     * @param activityName     the activity name
     * @param activityStarting whether the activity is being started or not
     * @param nowMs            The current time in ms
     */
    void updateForegroundTracker(String activityName, boolean activityStarting, long nowMs) {
        if (activityStarting) {
            long noActivityRunningForMs = nowMs - lastExitedForegroundMs.get();

            //FUTURE:SM Race condition between isEmpty and put
            if (foregroundActivities.isEmpty()) {
                lastEnteredForegroundMs.set(nowMs);

                if (noActivityRunningForMs >= timeoutMs
                        && configuration.getAutoTrackSessions()) {
                    startNewSession(new Date(nowMs), client.getUser(), true);
                }
            }
            foregroundActivities.add(activityName);
        } else {
            foregroundActivities.remove(activityName);

            if (foregroundActivities.isEmpty()) {
                lastExitedForegroundMs.set(nowMs);
            }
        }
        notifyNdkInForeground();
    }

    private void notifyNdkInForeground() {
        Boolean inForeground = isInForeground();
        boolean foreground = inForeground != null ? inForeground : false;
        notifyObservers(new StateEvent.UpdateInForeground(foreground, getContextActivity()));
    }

    @Nullable
    Boolean isInForeground() {
        return foregroundDetector.isInForeground();
    }

    //FUTURE:SM This shouldnt be here
    @Nullable
    Long getDurationInForegroundMs(long nowMs) {
        long durationMs = 0;
        long sessionStartTimeMs = lastEnteredForegroundMs.get();

        Boolean inForeground = isInForeground();

        if (inForeground == null) {
            return null;
        }
        if (inForeground && sessionStartTimeMs != 0) {
            durationMs = nowMs - sessionStartTimeMs;
        }
        return durationMs > 0 ? durationMs : 0;
    }

    @Nullable
    String getContextActivity() {
        if (foregroundActivities.isEmpty()) {
            return null;
        } else {
            // linked hash set retains order of added activity and ensures uniqueness
            // therefore obtain the most recently added
            int size = foregroundActivities.size();
            String[] activities = foregroundActivities.toArray(new String[size]);
            return activities[size - 1];
        }
    }
}
