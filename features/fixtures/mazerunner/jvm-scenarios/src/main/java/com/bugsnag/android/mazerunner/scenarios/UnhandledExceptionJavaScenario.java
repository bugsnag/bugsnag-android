package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Sends an unhandled exception to Bugsnag.
 */
public class UnhandledExceptionJavaScenario extends Scenario {

    public UnhandledExceptionJavaScenario(@NonNull Configuration config,
                                          @NonNull Context context,
                                          @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        throw new RuntimeException("UnhandledExceptionJavaScenario");
    }

}
