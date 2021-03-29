package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UnsatisfiedLinkErrorScenario extends Scenario {

    public native void doesNotExist();

    public UnsatisfiedLinkErrorScenario(@NonNull Configuration config,
                                        @NonNull Context context,
                                        @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        doesNotExist();
    }
}
