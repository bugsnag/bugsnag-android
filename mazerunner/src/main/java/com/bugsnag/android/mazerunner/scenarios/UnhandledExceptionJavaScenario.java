package com.bugsnag.android.mazerunner.scenarios;

/**
 * Sends an unhandled exception to Bugsnag.
 */
public class UnhandledExceptionJavaScenario extends Scenario {

    @Override
    public void run() {
        throw new RuntimeException("UnhandledExceptionJavaScenario");
    }

}
