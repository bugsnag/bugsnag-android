package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXJavaBreadcrumbNativeBreadcrumbScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("cxx-scenarios-bugsnag");
    }

    public native void activate();

    public CXXJavaBreadcrumbNativeBreadcrumbScenario(@NonNull Configuration config,
                                                     @NonNull Context context,
                                                     @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Bugsnag.leaveBreadcrumb("Reverse thrusters");
        activate();
    }
}
