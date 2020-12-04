package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXConfigurationMetadataNativeCrashScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native int activate();

    /**
     */
    public CXXConfigurationMetadataNativeCrashScenario(@NonNull Configuration config,
                                                       @NonNull Context context,
                                                       @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
        String metadata = getEventMetadata();
        if (metadata == null || !metadata.equals("no-metadata")) {
            config.addMetadata("fruit", "apple", "gala");
            config.addMetadata("fruit", "counters", 47);
            config.addMetadata("fruit", "ripe", true);
        }
    }

    @Override
    public void startScenario() {
        super.startScenario();
        int value = activate();
        System.out.println("The result: " + value);
    }
}
