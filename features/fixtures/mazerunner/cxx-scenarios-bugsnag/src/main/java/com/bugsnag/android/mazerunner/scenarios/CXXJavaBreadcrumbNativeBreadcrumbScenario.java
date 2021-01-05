package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

public class CXXJavaBreadcrumbNativeBreadcrumbScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("cxx-scenarios-bugsnag");
    }

    public native void activate();

    private Handler handler = new Handler();

    public CXXJavaBreadcrumbNativeBreadcrumbScenario(@NonNull Configuration config,
                                                     @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.leaveBreadcrumb("Reverse thrusters");
        activate();
    }
}
