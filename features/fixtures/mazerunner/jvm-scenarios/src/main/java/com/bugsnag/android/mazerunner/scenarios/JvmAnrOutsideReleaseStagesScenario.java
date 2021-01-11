package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;


public class JvmAnrOutsideReleaseStagesScenario extends Scenario {

    /**
     * Initializes bugsnag with custom release stages
     */
    public JvmAnrOutsideReleaseStagesScenario(@NonNull Configuration config,
                                              @NonNull Context context,
                                              @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
        config.setEnabledReleaseStages(Collections.singleton("fee-fi-fo-fum"));
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Handler main = new Handler(Looper.getMainLooper());
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) { }
                } catch (Exception exc) {
                    // Catch possible thread interruption exception
                }
            }
        }, 1); // Delayed to allow the UI to appear so there is something to tap
    }
}
