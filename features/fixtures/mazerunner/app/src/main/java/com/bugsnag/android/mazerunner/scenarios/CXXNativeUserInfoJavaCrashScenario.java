package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;

public class CXXNativeUserInfoJavaCrashScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXNativeUserInfoJavaCrashScenario(@NonNull Configuration config,
                                              @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void run() {
        super.run();
        activate();
        throw new RuntimeException("Oh no");
    }
}
