package com.bugsnag.android.mazerunner.scenarios;


import android.content.Context;

import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXNativeBreadcrumbNativeCrashScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXNativeBreadcrumbNativeCrashScenario(@NonNull Configuration config,
                                                  @NonNull Context context,
                                                  @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        activate();
    }
}
