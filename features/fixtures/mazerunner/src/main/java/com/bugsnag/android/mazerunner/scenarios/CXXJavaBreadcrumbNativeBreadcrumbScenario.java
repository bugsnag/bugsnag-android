package com.bugsnag.android.mazerunner.scenarios;


import android.content.Context;
import android.os.Handler;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXJavaBreadcrumbNativeBreadcrumbScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    private Handler handler = new Handler();

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
