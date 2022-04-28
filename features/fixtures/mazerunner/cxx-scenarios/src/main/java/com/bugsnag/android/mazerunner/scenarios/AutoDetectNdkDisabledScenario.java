package com.bugsnag.android.mazerunner.scenarios;

import static com.bugsnag.android.mazerunner.LogKt.getZeroEventsLogMessages;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

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
        config.getEnabledErrorTypes().setNdkCrashes(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        crash();
    }

    @Override
    public List<String> getInterceptedLogMessages() {
        return getZeroEventsLogMessages(getStartBugsnagOnly());
    }
}
