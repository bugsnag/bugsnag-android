package com.bugsnag.android;

import android.app.Application;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

/**
 * This class wraps the {@link LifecycleBreadcrumbLogger}. This is necessary because otherwise
 * {@link VerifyError} would be thrown when the implementation class was loaded, as older APIs don't
 * have a method definition for {@link android.app.Application.ActivityLifecycleCallbacks}.
 */
@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
final class SdkCompatWrapper {

    private final LifecycleBreadcrumbLogger logger = new LifecycleBreadcrumbLogger();

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void setupLifecycleLogger(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(logger);
    }

}
