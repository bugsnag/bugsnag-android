package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.Bugsnag;

import androidx.annotation.NonNull;

import java.lang.reflect.Array;

public class CXXCustomMetadataNativeNotifyScenario extends Scenario {
    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native void activate();

    public CXXCustomMetadataNativeNotifyScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.addMetadata("fruit", "orange", "meyer");
        Bugsnag.addMetadata("fruit", "counters", 302);
        Bugsnag.addMetadata("fruit", "ripe", false);
        activate();
    }
}
