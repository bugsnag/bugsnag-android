package com.bugsnag.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
    private final SessionStore sessionStore;
    private final SessionTrackingApiClient apiClient;
    private final Context context;
    private final String endpoint;

    private long lastForegroundMs;
    private Long sessionStartMs;
    private Session currentSession;
    private boolean trackedFirstSession = false;

    SessionTracker(Configuration configuration, Client client, SessionStore sessionStore,
                   SessionTrackingApiClient apiClient, Context context) {
        this(configuration, client, DEFAULT_TIMEOUT_MS, sessionStore, apiClient, context);
    }

    SessionTracker(Configuration configuration, Client client, long timeoutMs,
                   SessionStore sessionStore, SessionTrackingApiClient apiClient, Context context) {
        this.configuration = configuration;
        this.client = client;
        this.timeoutMs = timeoutMs;
        this.sessionStore = sessionStore;
        this.apiClient = apiClient;
        this.context = context;
        this.endpoint = configuration.getSessionEndpoint();
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
        synchronized (sessionStore) {
            currentSession = generateSession(date, user, autoCaptured);
            trackSessionIfNeeded(autoCaptured, currentSession);
        }
    }

    @NonNull
    private Session generateSession(@NonNull Date date, @Nullable User user, boolean autoCaptured) {
        sessionStartMs = date.getTime();
        Session session = new Session(UUID.randomUUID().toString(), date, user);
        session.setAutoCaptured(autoCaptured);
        return session;
    }

    /**
     * Determines whether or not a session should be tracked. If this is true, the session will be
     * stored and sent to the Bugsnag API, otherwise no action will occur in this method.
     *
     * @param autoCaptured whether the session was automatically captured by the SDK or not
     * @param session      the session
     */
    private void trackSessionIfNeeded(boolean autoCaptured, Session session) {
        String releaseStage = getReleaseStage();
        boolean notifyForRelease = configuration.shouldNotifyForReleaseStage(releaseStage);

        if ((configuration.shouldAutoCaptureSessions() || !autoCaptured) && notifyForRelease) {
            sessionQueue.add(session);
            sessionStore.write(session); // store session for sending
            trackedFirstSession = true;
        }
    }

    /**
     * Track a new session when auto capture is enabled via config after initialisation.
     */
    void onAutoCaptureEnabled() {
        synchronized (sessionStore) {
            if (!trackedFirstSession) {
                if (currentSession == null) { // unlikely case, will be initialised later
                    return;
                }
                trackSessionIfNeeded(currentSession.isAutoCaptured(), currentSession);
            }
        }
    }

    private String getReleaseStage() {
        if (configuration.getReleaseStage() != null) {
            return configuration.getReleaseStage();
        } else {
            return AppDataSummary.guessReleaseStage(context);
        }
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

    /**
     * Attempts to flush session payloads stored on disk
     */
    void flushStoredSessions() {
        synchronized (sessionStore) {
            List<File> storedFiles = sessionStore.findStoredFiles();

            if (!storedFiles.isEmpty()) {
                AppData appData = new AppData(context, configuration, this);
                SessionTrackingPayload payload = new SessionTrackingPayload(storedFiles, appData);

                try {
                    apiClient.postSessionTrackingPayload(endpoint, payload, configuration.getSessionApiHeaders());
                    deleteStoredFiles(storedFiles);
                } catch (NetworkException e) { // store for later sending
                    Logger.info("Failed to post stored session payload");
                } catch (BadResponseException e) { // drop bad data
                    Logger.warn("Invalid session tracking payload", e);
                    deleteStoredFiles(storedFiles);
                }
            }
        }
    }

    private void deleteStoredFiles(Collection<File> storedFiles) {
        for (File storedFile : storedFiles) {
            storedFile.delete();
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
        if (configuration.isAutomaticallyCollectBreadcrumbs()) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(KEY_LIFECYCLE_CALLBACK, lifecycleCallback);
            client.leaveBreadcrumb(activityName, BreadcrumbType.NAVIGATION, metadata);
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
     * @param activityName the activity name
     * @param inForeground whether the activity is in the foreground or not
     */
    void updateForegroundTracker(String activityName, boolean inForeground, long now) {
        if (inForeground) {
            long delta = now - lastForegroundMs;

            if (foregroundActivities.isEmpty() && delta >= timeoutMs && configuration.shouldAutoCaptureSessions()) {
                User user = client != null ? client.user : null;
                startNewSession(new Date(now), user, true);
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
