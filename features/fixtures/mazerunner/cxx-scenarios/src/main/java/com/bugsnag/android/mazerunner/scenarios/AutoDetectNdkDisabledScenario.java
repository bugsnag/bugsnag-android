package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AutoDetectNdkDisabledScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native void crash();

    /**
     */
    public AutoDetectNdkDisabledScenario(@NonNull Configuration config,
                                         @NonNull Context context,
                                         @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
        config.getEnabledErrorTypes().setNdkCrashes(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        crash();
    }
}
