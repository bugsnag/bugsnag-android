package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
public class HandledExceptionJavaScenario extends Scenario {

    @Override
    public void run() {
        Bugsnag.notify(generateException());
    }

}
