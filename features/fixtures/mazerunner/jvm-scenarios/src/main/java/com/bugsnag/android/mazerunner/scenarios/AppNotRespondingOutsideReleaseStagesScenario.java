package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;

import java.util.Collections;

public class AppNotRespondingOutsideReleaseStagesScenario extends Scenario {

    /**
     *
     */
    public AppNotRespondingOutsideReleaseStagesScenario(@NonNull Configuration config,
                                                 @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
        config.setEnabledReleaseStages(Collections.singleton("fee-fi-fo-fum"));
    }

    @Override
    public void run() {
        super.run();
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
