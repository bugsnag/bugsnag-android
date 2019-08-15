package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;

/**
 * Sends an unhandled exception to Bugsnag.
 */
public class UnhandledExceptionJavaScenario extends Scenario {

    public UnhandledExceptionJavaScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
    }

    @Override
    public void run() {
        super.run();
        throw new RuntimeException("UnhandledExceptionJavaScenario");
    }

}
