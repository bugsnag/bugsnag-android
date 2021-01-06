package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXRemoveOnErrorScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("cxx-scenarios-bugsnag");
    }

    public native void activate();

    /**
     * 
     */
    public CXXRemoveOnErrorScenario(@NonNull Configuration config,
                                    @NonNull Context context,
                                    @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
        config.setContext("CXXRemoveOnErrorScenario");
    }

    @Override
    public void startScenario() {
        super.startScenario();
        activate();
    }
}
