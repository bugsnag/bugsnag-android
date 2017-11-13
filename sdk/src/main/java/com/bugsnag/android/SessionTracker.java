package com.bugsnag.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

class SessionTracker implements Application.ActivityLifecycleCallbacks {

    private static final String KEY_LIFECYCLE_CALLBACK = "ActivityLifecycle";
    private static final int DEFAULT_TIMEOUT_MS = 30000;

    private final Object lock = new Object();
    final Collection<Session> sessionQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> foregroundActivities = new HashSet<>();
    private final Configuration configuration;
    private final long timeoutMs;
    private long lastForegroundMs;

    private Session currentSession;

    SessionTracker(Configuration configuration) {
        this(configuration, DEFAULT_TIMEOUT_MS);
    }

    SessionTracker(Configuration configuration, long timeoutMs) {
        this.configuration = configuration;
        this.timeoutMs = timeoutMs;
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
    void startNewSession(@NonNull Date date, @Nullable User user) {
        synchronized (lock) {
            if (configuration.shouldAutoCaptureSessions()) {
                Session session = new Session();
                session.setId(UUID.randomUUID().toString());
                session.setStartedAt(date);
                session.setUser(user);

                // TODO handle sending/storing sessions here!
                sessionQueue.add(session); // store previous session
                currentSession = session;
            }
        }
    }

    @Nullable
    Session getCurrentSession() {
        synchronized (lock) {
            return currentSession;
        }
    }

    void incrementUnhandledError() {
        synchronized (lock) {
            if (currentSession != null) {
                currentSession.incrementUnhandledErrCount();
            }
        }
    }

    void incrementHandledError() {
        synchronized (lock) {
            if (currentSession != null) {
                currentSession.incrementHandledErrCount();
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        leaveLifecycleBreadcrumb(activity, "onCreate()");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(activity, "onStart()");
        updateForegroundTracker(activity.getClass().getCanonicalName(), true, System.currentTimeMillis());
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(activity, "onResume()");
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(activity, "onPause()");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(activity, "onStop()");
        updateForegroundTracker(activity.getClass().getCanonicalName(), false, System.currentTimeMillis());
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, Bundle outState) {
        leaveLifecycleBreadcrumb(activity, "onSaveInstanceState()");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(activity, "onDestroy()");
    }

    private synchronized void leaveLifecycleBreadcrumb(@NonNull Activity activity, String lifecycleCallback) {
        String activityName = activity.getClass().getSimpleName();
        Map<String, String> metadata = new HashMap<>();
        metadata.put(KEY_LIFECYCLE_CALLBACK, lifecycleCallback);
        Bugsnag.leaveBreadcrumb(activityName, BreadcrumbType.NAVIGATION, metadata);
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
     * @param activityName the activity name
     * @param inForeground whether the activity is in the foreground or not
     */
    void updateForegroundTracker(String activityName, boolean inForeground, long now) {
        if (inForeground) {
            long delta = now - lastForegroundMs;

            if (foregroundActivities.isEmpty() && delta >= timeoutMs) {
                startNewSession(new Date(), Bugsnag.getClient().user);
            }
            foregroundActivities.add(activityName);
        } else {
            foregroundActivities.remove(activityName);
            lastForegroundMs = now;
        }
    }

    boolean isInForeground() {
        return !foregroundActivities.isEmpty();
    }

    long getDurationInForeground() {
        long duration = 0;

        if (currentSession != null) {
            duration = currentSession.getStartedAt().getTime() - System.currentTimeMillis();
        }
        return duration > 0 ? duration : 0;
    }

}
