package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;
import androidx.annotation.NonNull;

import java.util.Collections;

public class CXXTrapOutsideReleaseStagesScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native void crash();

    CXXTrapOutsideReleaseStagesScenario(@NonNull Configuration config,
                                               @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
        config.setEnabledReleaseStages(Collections.singleton("fee-fi-fo-fum"));
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
