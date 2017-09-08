package com.bugsnag.android;

import android.app.Application;
import android.support.annotation.NonNull;

/**
 * This class wraps the {@link LifecycleBreadcrumbLogger}. This is necessary because otherwise
 * {@link VerifyError} would be thrown when the implementation class was loaded, as older APIs don't
 * have a method definition for {@link android.app.Application.ActivityLifecycleCallbacks}.
 */
final class SdkCompatWrapper {

    private final LifecycleBreadcrumbLogger logger = new LifecycleBreadcrumbLogger();

    void setupLifecycleLogger(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(logger);
    }

}
