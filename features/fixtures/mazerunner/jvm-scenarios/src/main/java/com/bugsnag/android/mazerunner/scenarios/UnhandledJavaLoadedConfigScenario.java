package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;
import androidx.annotation.NonNull;

/**
 * Sends an unhandled exception to Bugsnag.
 */
public class UnhandledJavaLoadedConfigScenario extends Scenario {

    public UnhandledJavaLoadedConfigScenario(@NonNull Configuration config,
                                             @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Configuration testConfig = Configuration.load(this.getContext());
        testConfig.setAutoTrackSessions(false);
        Bugsnag.start(this.getContext(), testConfig);
        throw new RuntimeException("UnhandledJavaLoadedConfigScenario");
    }

}
