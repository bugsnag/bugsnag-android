package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXSigfpeScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native int crash(int value);

    public CXXSigfpeScenario(@NonNull Configuration config,
                             @NonNull Context context,
                             @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        crash(2726);
    }
}
