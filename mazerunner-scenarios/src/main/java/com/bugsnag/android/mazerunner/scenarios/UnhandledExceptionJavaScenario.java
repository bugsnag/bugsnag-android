package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Sends an unhandled exception to Bugsnag.
 */
public class UnhandledExceptionJavaScenario extends Scenario {

    public UnhandledExceptionJavaScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void run() {
        super.run();
        throw new RuntimeException("UnhandledExceptionJavaScenario");
    }

}
