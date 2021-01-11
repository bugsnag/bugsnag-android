package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
public class HandledExceptionJavaScenario extends Scenario {

    public HandledExceptionJavaScenario(@NonNull Configuration config,
                                        @NonNull Context context,
                                        @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Bugsnag.notify(new RuntimeException(getClass().getSimpleName()));
    }

}
