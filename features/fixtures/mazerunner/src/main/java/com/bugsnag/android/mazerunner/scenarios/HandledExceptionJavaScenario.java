package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
public class HandledExceptionJavaScenario extends Scenario {

    public HandledExceptionJavaScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.notify(new RuntimeException(getClass().getSimpleName()));
    }

}
