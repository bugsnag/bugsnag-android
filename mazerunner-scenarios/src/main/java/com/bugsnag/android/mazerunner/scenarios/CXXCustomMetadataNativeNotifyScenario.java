package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;

public class CXXCustomMetadataNativeNotifyScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXCustomMetadataNativeNotifyScenario(@NonNull Configuration config,
                                                 @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.addToTab("fruit", "orange", "meyer");
        Bugsnag.addToTab("fruit", "counters", 302);
        Bugsnag.addToTab("fruit", "ripe", false);
        activate();
    }
}
