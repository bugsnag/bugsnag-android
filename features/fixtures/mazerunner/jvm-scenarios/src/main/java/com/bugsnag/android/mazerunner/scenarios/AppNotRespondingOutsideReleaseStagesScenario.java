package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AppNotRespondingOutsideReleaseStagesScenario extends Scenario {

    /**
     *
     */
    public AppNotRespondingOutsideReleaseStagesScenario(@NonNull Configuration config,
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
                    Thread.sleep(50000); // Forever
                } catch (Exception exc) {
                    // Catch possible thread interruption exception
                }
            }
        }, 1); // Delayed to allow the UI to appear so there is something to tap
    }
}
