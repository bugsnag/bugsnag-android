package com.bugsnag.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class LifecycleBreadcrumbLogger implements Application.ActivityLifecycleCallbacks {

    private static final String KEY_LIFECYCLE_CALLBACK = "ActivityLifecycle";

    private final Queue<Pair<String, String>> queue = new ConcurrentLinkedQueue<>();
    private final Client client;

    LifecycleBreadcrumbLogger(Client client) {
        this.client = client;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        leaveLifecycleBreadcrumb(activity, "onCreate()");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(activity, "onStart()");
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
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, Bundle outState) {
        leaveLifecycleBreadcrumb(activity, "onSaveInstanceState()");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(activity, "onDestroy()");
    }

    private void leaveLifecycleBreadcrumb(@NonNull Activity activity, String lifecycleCallback) {
        String activityName = activity.getClass().getSimpleName();

        if (client == null) { // not initialised yet, enqueue breadcrumbs for later
            queue.add(new Pair<>(activityName, lifecycleCallback));
        } else {
            while (!queue.isEmpty()) {
                Pair<String, String> pair = queue.poll();
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

}
