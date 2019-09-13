package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.bugsnag.android.NativeInterface;
import com.bugsnag.android.Configuration;

import android.support.annotation.NonNull;

public class AppNotRespondingLaterDisabledScenario extends Scenario {

    public AppNotRespondingLaterDisabledScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
    }

    @Override
    public void run() {
        super.run();
        NativeInterface.disableAnrReporting();
        Handler main = new Handler(Looper.getMainLooper());
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50000); // Forever
                } catch (Exception _ex) {
                    // Catch possible thread interruption exception
                }
            }
        }, 1); // Delayed to allow the UI to appear so there is something to tap
    }
}

