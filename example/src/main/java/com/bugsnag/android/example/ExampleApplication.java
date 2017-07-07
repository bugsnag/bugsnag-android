package com.bugsnag.android.example;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.bugsnag.android.Bugsnag;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Bugsnag client
        Bugsnag.init(this);

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });

    }

}
