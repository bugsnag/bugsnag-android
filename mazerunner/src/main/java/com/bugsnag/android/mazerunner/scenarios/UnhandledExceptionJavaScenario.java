package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;

import org.jetbrains.annotations.NotNull;

/**
 * Sends an unhandled exception to Bugsnag.
 */
public class UnhandledExceptionJavaScenario extends Scenario {

    public UnhandledExceptionJavaScenario(@NotNull Configuration config, @NotNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        throw new RuntimeException("UnhandledExceptionJavaScenario");
    }

}
