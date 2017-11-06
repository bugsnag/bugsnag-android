package com.bugsnag.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

    private final Object lock = new Object();
    private final Queue<Session> sessionQueue = new ConcurrentLinkedQueue<>();
    private final Set<String> foregroundActivities = new HashSet<>();
    private Session currentSession;

    void startNewSession(Date date, User user) {
        synchronized (lock) {
            Session session = new Session();
            session.setId(UUID.randomUUID().toString());
            session.setStartedAt(date);
            session.setUser(user);

            // TODO handle sending/storing sessions here!
            sessionQueue.add(session); // store previous session
            currentSession = session;
        }
    }

    @Nullable Session getCurrentSession() {
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
        updateForegroundTracker(activity.getClass().getCanonicalName(), true);
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
        updateForegroundTracker(activity.getClass().getCanonicalName(), false);
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

    void updateForegroundTracker(String activityName, boolean inForeground) {
        if (inForeground) {
            foregroundActivities.add(activityName);

            if (getCurrentSession() == null) { // TODO should be expired via timeout
                startNewSession(new Date(), null); // TODO serialise user
            }
        } else {
            foregroundActivities.remove(activityName);
        }
    }

    boolean isInForeground() {
        return !foregroundActivities.isEmpty();
    }
}
