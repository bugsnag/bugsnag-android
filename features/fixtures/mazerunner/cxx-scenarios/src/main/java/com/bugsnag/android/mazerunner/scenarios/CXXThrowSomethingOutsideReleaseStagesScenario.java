package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;

public class CXXThrowSomethingOutsideReleaseStagesScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native void crash(int num);

    /**
     * Sets custom enabled release stages.
     */
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
