package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;

public class AutoDetectNdkDisabledScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void crash();

    /**
     */
    public AutoDetectNdkDisabledScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
        config.getEnabledErrorTypes().setNdkCrashes(false);
    }

    @Override
    public void run() {
        super.run();
        String metadata = getEventMetaData();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        crash();
    }
}
