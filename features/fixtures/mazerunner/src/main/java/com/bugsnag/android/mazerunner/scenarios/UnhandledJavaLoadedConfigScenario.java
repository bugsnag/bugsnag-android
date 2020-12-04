package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.OnErrorCallback;

/**
 * Sends an unhandled exception to Bugsnag.
 */
public class UnhandledJavaLoadedConfigScenario extends Scenario {

    public UnhandledJavaLoadedConfigScenario(@NonNull Configuration config,
                                             @NonNull Context context,
                                             @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Configuration testConfig = Configuration.load(this.getContext());
        testConfig.setAutoTrackSessions(false);
        Bugsnag.start(this.getContext(), testConfig);
        throw new RuntimeException("UnhandledJavaLoadedConfigScenario");
    }

}
