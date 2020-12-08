package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bugsnag.android.Configuration;

public class CXXSignalOnErrorTrueScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void crash();

    public CXXSignalOnErrorTrueScenario(@NonNull Configuration config,
                                        @NonNull Context context,
                                        @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        String metadata = getEventMetadata();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        crash();
    }
}
