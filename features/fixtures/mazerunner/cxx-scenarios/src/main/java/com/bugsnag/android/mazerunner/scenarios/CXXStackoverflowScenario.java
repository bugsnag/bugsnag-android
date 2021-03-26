package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXStackoverflowScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native void crash(int counter, @NonNull String longText);

    public CXXStackoverflowScenario(@NonNull Configuration config,
                                    @NonNull Context context,
                                    @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        crash(1209, "some moderately long text, longer than 7 characters at least.");
    }
}
