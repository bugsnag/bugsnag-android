package com.bugsnag.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

class LifecycleBreadcrumbLogger implements Application.ActivityLifecycleCallbacks {

    private static final String KEY_LIFECYCLE_CALLBACK = "ActivityLifecycle";

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
        Map<String, String> metadata = new HashMap<>();
        metadata.put(KEY_LIFECYCLE_CALLBACK, lifecycleCallback);
        Bugsnag.leaveBreadcrumb(activityName, BreadcrumbType.NAVIGATION, metadata);
    }

}
