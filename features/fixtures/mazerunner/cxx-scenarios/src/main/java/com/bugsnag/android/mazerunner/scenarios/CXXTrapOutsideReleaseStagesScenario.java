package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;

public class CXXTrapOutsideReleaseStagesScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native void crash();

    /**
     *
     */
    public CXXTrapOutsideReleaseStagesScenario(@NonNull Configuration config,
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
        crash();
    }
}
