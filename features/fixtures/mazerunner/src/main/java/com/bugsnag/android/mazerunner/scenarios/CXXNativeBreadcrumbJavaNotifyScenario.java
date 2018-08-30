package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;

import android.content.Context;

import com.bugsnag.android.Configuration;

import org.jetbrains.annotations.NotNull;

public class CXXNativeBreadcrumbJavaNotifyScenario extends Scenario {
    static {
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXNativeBreadcrumbJavaNotifyScenario(@NotNull Configuration config, @NotNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        activate();
        Bugsnag.notify(new Exception("Did not like"));
    }
}
