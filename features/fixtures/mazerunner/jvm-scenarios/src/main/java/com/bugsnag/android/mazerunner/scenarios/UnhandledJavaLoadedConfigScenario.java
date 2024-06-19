package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Sends an unhandled exception to Bugsnag.
 */
public class UnhandledJavaLoadedConfigScenario extends Scenario {

    public UnhandledJavaLoadedConfigScenario(@NonNull Configuration config,
                                             @NonNull Context context,
                                             @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.getEnabledErrorTypes().setAnrs(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Configuration testConfig = Configuration.load(this.getContext());
        testConfig.setAutoTrackSessions(false);
        testConfig.setContext("FooContext");
        Bugsnag.start(this.getContext(), testConfig);
        Bugsnag.addMetadata("TestData", "password", "NotTellingYou");
        throw new RuntimeException("UnhandledJavaLoadedConfigScenario");
    }

}
