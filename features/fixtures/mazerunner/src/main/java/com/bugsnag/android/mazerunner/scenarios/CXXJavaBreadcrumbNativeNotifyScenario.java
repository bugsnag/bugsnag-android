package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import org.jetbrains.annotations.NotNull;


public class CXXJavaBreadcrumbNativeNotifyScenario extends Scenario {
    static {
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXJavaBreadcrumbNativeNotifyScenario(@NotNull Configuration config, @NotNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.leaveBreadcrumb("Initiate lift");
        activate();
    }
}
