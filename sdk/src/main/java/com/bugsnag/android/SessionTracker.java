package com.bugsnag.android;

import static com.bugsnag.android.MapUtils.getStringFromMap;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class SessionTracker implements Application.ActivityLifecycleCallbacks {

    private static final String KEY_LIFECYCLE_CALLBACK = "ActivityLifecycle";
    private static final int DEFAULT_TIMEOUT_MS = 30000;

    private final Collection<String>
        foregroundActivities = new ConcurrentLinkedQueue<>();
    private final Configuration configuration;
    private final long timeoutMs;
    private final Client client;
    private final SessionStore sessionStore;

    // This most recent time an Activity was stopped.
    private AtomicLong activityLastStoppedAtMs = new AtomicLong(0);

    // The first Activity in this 'session' was started at this time.
    private AtomicLong activityFirstStartedAtMs = new AtomicLong(0);
    private AtomicReference<Session> currentSession = new AtomicReference<>();
    private Semaphore flushingRequest = new Semaphore(1);

    SessionTracker(Configuration configuration, Client client, SessionStore sessionStore) {
        this(configuration, client, DEFAULT_TIMEOUT_MS, sessionStore);
    }

    SessionTracker(Configuration configuration, Client client, long timeoutMs,
                   SessionStore sessionStore) {
        this.configuration = configuration;
        this.client = client;
        this.timeoutMs = timeoutMs;
        this.sessionStore = sessionStore;
    }

    /**
     * Starts a new session with the given date and user.
     * <p>
     * A session will only be created if {@link Configuration#shouldAutoCaptureSessions()} returns
     * true.
     *
     * @param date the session start date
     * @param user the session user (if any)
     */
    void startNewSession(@NonNull Date date, @Nullable User user, boolean autoCaptured) {
        if (configuration.getSessionEndpoint() == null) {
            Logger.warn("The session tracking endpoint has not been set. "
                + "Session tracking is disabled");
            return;
        }
        Session session = new Session(UUID.randomUUID().toString(), date, user, autoCaptured);
        currentSession.set(session);
        trackSessionIfNeeded(session);
    }

    /**
     * Determines whether or not a session should be tracked. If this is true, the session will be
     * stored and sent to the Bugsnag API, otherwise no action will occur in this method.
     *
     * @param session the session
     */
    private void trackSessionIfNeeded(final Session session) {
        boolean notifyForRelease = configuration.shouldNotifyForReleaseStage(getReleaseStage());

        if (notifyForRelease
            && (configuration.shouldAutoCaptureSessions() || !session.isAutoCaptured())
            && session.isTracked().compareAndSet(false, true)) {
            try {
                final String endpoint = configuration.getSessionEndpoint();
                Async.run(new Runnable() {
                    @Override
                    public void run() {
                        //FUTURE:SM It would be good to optimise this
                        flushStoredSessions();

                        SessionTrackingPayload payload =
                            new SessionTrackingPayload(session, null,
                                client.appData, client.deviceData);

                        try {
                            configuration.getDelivery().deliver(payload, configuration);
                        } catch (DeliveryFailureException exception) { // store for later sending
                            Logger.warn("Storing session payload for future delivery", exception);
                            sessionStore.write(session);
                        } catch (Exception exception) {
                            Logger.warn("Dropping invalid session tracking payload", exception);
                        }
                    }
                });
            } catch (RejectedExecutionException exception) {
                // This is on the current thread but there isn't much else we can do
                sessionStore.write(session);
            }
        }
    }

    /**
     * Track a new session when auto capture is enabled via config after initialisation.
     */
    void onAutoCaptureEnabled() {
        Session session = currentSession.get();
        if (session != null && !foregroundActivities.isEmpty()) {
            // If there is no session we will wait for one to be created
            trackSessionIfNeeded(session);
        }
    }

    private String getReleaseStage() {
        return getStringFromMap("releaseStage", client.appData.getAppDataSummary());
    }

    @Nullable
    Session getCurrentSession() {
        return currentSession.get();
    }

    void incrementUnhandledError() {
        Session session = currentSession.get();
        if (session != null) {
            session.incrementUnhandledErrCount();
        }
    }

    void incrementHandledError() {
        Session session = currentSession.get();
        if (session != null) {
            session.incrementHandledErrCount();
        }
    }

    /**
     * Attempts to flush session payloads stored on disk
     */
    void flushStoredSessions() {
        if (flushingRequest.tryAcquire(1)) {
            try {
                List<File> storedFiles;

                storedFiles = sessionStore.findStoredFiles();

                if (!storedFiles.isEmpty()) {
                    SessionTrackingPayload payload =
                        new SessionTrackingPayload(null, storedFiles,
                            client.appData, client.deviceData);

                    //FUTURE:SM Reduce duplication here and above
                    try {
                        configuration.getDelivery().deliver(payload, configuration);
                        sessionStore.deleteStoredFiles(storedFiles);
                    } catch (DeliveryFailureException exception) {
                        sessionStore.cancelQueuedFiles(storedFiles);
                        Logger.warn("Leaving session payload for future delivery", exception);
                    } catch (Exception exception) {
                        // drop bad data
                        Logger.warn("Deleting invalid session tracking payload", exception);
                        sessionStore.deleteStoredFiles(storedFiles);
                    }
                }
            } finally {
                flushingRequest.release(1);
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onCreate()");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        String activityName = getActivityName(activity);
        leaveLifecycleBreadcrumb(activityName, "onStart()");
        updateForegroundTracker(activityName, true, System.currentTimeMillis());
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onResume()");
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onPause()");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        String activityName = getActivityName(activity);
        leaveLifecycleBreadcrumb(activityName, "onStop()");
        updateForegroundTracker(activityName, false, System.currentTimeMillis());
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, Bundle outState) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onSaveInstanceState()");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onDestroy()");
    }

    private String getActivityName(@NonNull Activity activity) {
        return activity.getClass().getSimpleName();
    }

    void leaveLifecycleBreadcrumb(String activityName, String lifecycleCallback) {
        leaveBreadcrumb(activityName, lifecycleCallback);
    }

    private void leaveBreadcrumb(String activityName, String lifecycleCallback) {
        if (configuration.isAutomaticallyCollectingBreadcrumbs()) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(KEY_LIFECYCLE_CALLBACK, lifecycleCallback);

            try {
                client.leaveBreadcrumb(activityName, BreadcrumbType.NAVIGATION, metadata);
            } catch (Exception ex) {
                Logger.warn("Failed to leave breadcrumb in SessionTracker: " + ex.getMessage());
            }
        }
    }

    /**
     * Tracks a session if a session has not yet been captured,
     * recording the session as auto-captured. Requires the current activity.
     *
     * @param activity the current activity
     */
    void startFirstSession(Activity activity) {
        Session session = currentSession.get();
        if (session == null) {
            long nowMs = System.currentTimeMillis();
            activityFirstStartedAtMs.set(nowMs);
            startNewSession(new Date(nowMs), client.getUser(), true);
            foregroundActivities.add(getActivityName(activity));
        }
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
            long noActivityRunningForMs = nowMs - activityLastStoppedAtMs.get();

            //FUTURE:SM Race condition between isEmpty and put
            if (foregroundActivities.isEmpty()
                && noActivityRunningForMs >= timeoutMs
                && configuration.shouldAutoCaptureSessions()) {

                activityFirstStartedAtMs.set(nowMs);
                startNewSession(new Date(nowMs), client.getUser(), true);
            }
            foregroundActivities.add(activityName);
        } else {
            foregroundActivities.remove(activityName);
            activityLastStoppedAtMs.set(nowMs);
        }
    }

    boolean isInForeground() {
        return !foregroundActivities.isEmpty();
    }

    //FUTURE:SM This shouldnt be here
    long getDurationInForegroundMs(long nowMs) {
        long durationMs = 0;
        long sessionStartTimeMs = activityFirstStartedAtMs.get();

        if (isInForeground() && sessionStartTimeMs != 0) {
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
