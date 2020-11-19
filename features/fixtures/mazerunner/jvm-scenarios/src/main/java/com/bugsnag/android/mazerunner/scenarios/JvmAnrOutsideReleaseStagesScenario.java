package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import java.util.Collections;

import androidx.annotation.NonNull;

public class JvmAnrOutsideReleaseStagesScenario extends Scenario {

    public JvmAnrOutsideReleaseStagesScenario(@NonNull Configuration config, @NonNull Context context) {
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
                    while (true) { }
                } catch (Exception _ex) {
                    // Catch possible thread interruption exception
                }
            }
        }, 1); // Delayed to allow the UI to appear so there is something to tap
    }
}
