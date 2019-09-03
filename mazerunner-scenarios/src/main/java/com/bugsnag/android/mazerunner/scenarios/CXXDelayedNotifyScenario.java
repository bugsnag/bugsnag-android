package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

public class CXXDelayedNotifyScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    private boolean didActivate = false;
    private Handler handler = new Handler();

    public CXXDelayedNotifyScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
    }

    @Override
    public void run() {
        super.run();
        if (didActivate) {
            return;
        }
        didActivate = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                activate();
            }
        }, 6000);
    }
}
