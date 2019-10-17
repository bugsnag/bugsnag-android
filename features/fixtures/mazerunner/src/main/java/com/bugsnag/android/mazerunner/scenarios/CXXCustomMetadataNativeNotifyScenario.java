package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.Bugsnag;

import androidx.annotation.NonNull;

import java.lang.reflect.Array;

public class CXXCustomMetadataNativeNotifyScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXCustomMetadataNativeNotifyScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
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
