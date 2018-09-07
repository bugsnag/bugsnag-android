package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.Bugsnag;

import android.support.annotation.NonNull;

import java.lang.reflect.Array;

public class CXXCustomMetadataNativeCrashScenario extends Scenario {
    static {
        System.loadLibrary("entrypoint");
    }

    public native int activate();

    public CXXCustomMetadataNativeCrashScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
    }

    @Override
    public void run() {
        super.run();
        String metadata = getEventMetaData();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        Bugsnag.addToTab("fruit", "apple", "gala");
        Bugsnag.addToTab("fruit", "counters", 47);
        Bugsnag.addToTab("fruit", "ripe", true);
        int value = activate();
        System.out.println("The result: " + value);
    }
}
