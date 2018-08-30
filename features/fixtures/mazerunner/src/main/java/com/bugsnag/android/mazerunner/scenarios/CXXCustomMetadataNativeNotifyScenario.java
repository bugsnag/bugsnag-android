package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.Bugsnag;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;

public class CXXCustomMetadataNativeNotifyScenario extends Scenario {
    static {
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXCustomMetadataNativeNotifyScenario(@NotNull Configuration config, @NotNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.addToTab("fruit", "apple", "gala");
        Bugsnag.addToTab("fruit", "counters", 47);
        Bugsnag.addToTab("fruit", "ripe", true);
        activate();
    }
}
