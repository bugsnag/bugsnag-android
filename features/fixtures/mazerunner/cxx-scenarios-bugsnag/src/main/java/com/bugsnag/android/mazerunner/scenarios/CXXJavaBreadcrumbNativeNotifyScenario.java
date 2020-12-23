package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;

public class CXXJavaBreadcrumbNativeNotifyScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("cxx-scenarios-bugsnag");
    }

    public native void activate();

    public CXXJavaBreadcrumbNativeNotifyScenario(@NonNull Configuration config,
                                                 @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.leaveBreadcrumb("Initiate lift");
        Bugsnag.leaveBreadcrumb("Disable lift");
        activate();
    }
}
