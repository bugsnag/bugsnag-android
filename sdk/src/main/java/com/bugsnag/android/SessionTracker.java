package com.bugsnag.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

class SessionTracker implements Application.ActivityLifecycleCallbacks {

    private static final String KEY_LIFECYCLE_CALLBACK = "ActivityLifecycle";
    private static final int DEFAULT_TIMEOUT_MS = 30000;

    final Collection<Session> sessionQueue = new ConcurrentLinkedQueue<>();
    final Queue<Pair<String, String>> breadcrumbQueue = new ConcurrentLinkedQueue<>();

    private final Set<String> foregroundActivities = new HashSet<>();
    private final Configuration configuration;
    private final long timeoutMs;
    private final Client client;

    private long lastForegroundMs;
    private Long sessionStartMs;
    private Session currentSession;

    SessionTracker(Configuration configuration, Client client) {
        this(configuration, client, DEFAULT_TIMEOUT_MS);
    }

    SessionTracker(Configuration configuration, Client client, long timeoutMs) {
        this.configuration = configuration;
        this.client = client;
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
    synchronized void startNewSession(@NonNull Date date, @Nullable User user) {
        sessionStartMs = date.getTime();

        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setStartedAt(date);
        session.setUser(user);

        if (configuration.shouldAutoCaptureSessions()) {
            sessionQueue.add(session); // store session for sending
        }
        currentSession = session;
    }

    @Nullable
    synchronized Session getCurrentSession() {
        return currentSession;
    }

    synchronized void incrementUnhandledError() {
        if (currentSession != null) {
            currentSession.incrementUnhandledErrCount();
        }
    }

    synchronized void incrementHandledError() {
        if (currentSession != null) {
            currentSession.incrementHandledErrCount();
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onCreate()");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onStart()");
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
        leaveLifecycleBreadcrumb(getActivityName(activity), "onStop()");
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

    synchronized void leaveLifecycleBreadcrumb(String activityName, String lifecycleCallback) {
        if (client == null) { // not initialised yet, enqueue breadcrumbs for later
            breadcrumbQueue.add(new Pair<>(activityName, lifecycleCallback));
        } else {
            while (!breadcrumbQueue.isEmpty()) {
                Pair<String, String> pair = breadcrumbQueue.poll();
                leaveBreadcrumb(pair.first, pair.second);
            }
            leaveBreadcrumb(activityName, lifecycleCallback);
        }
    }

    private void leaveBreadcrumb(String activityName, String lifecycleCallback) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(KEY_LIFECYCLE_CALLBACK, lifecycleCallback);
        client.leaveBreadcrumb(activityName, BreadcrumbType.NAVIGATION, metadata);
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

            if (foregroundActivities.isEmpty() && delta >= timeoutMs && configuration.shouldAutoCaptureSessions()) {
                startNewSession(new Date(now), Bugsnag.getClient().user);
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

    long getDurationInForeground(long now) {
        long duration = 0;

        if (isInForeground() && sessionStartMs != null) {
            duration = now - sessionStartMs;
        }
        return duration > 0 ? duration : 0;
    }

}
