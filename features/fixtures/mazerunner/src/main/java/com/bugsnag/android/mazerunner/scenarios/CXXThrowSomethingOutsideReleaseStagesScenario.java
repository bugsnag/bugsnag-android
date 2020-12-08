package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;

import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXThrowSomethingOutsideReleaseStagesScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void crash(int num);

    public CXXThrowSomethingOutsideReleaseStagesScenario(@NonNull Configuration config,
                                                         @NonNull Context context,
                                                         @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
        config.setEnabledReleaseStages(Collections.singleton("fee-fi-fo-fum"));
    }

    @Override
    public void startScenario() {
        super.startScenario();
        String metadata = getEventMetadata();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        crash(23);
    }
}
