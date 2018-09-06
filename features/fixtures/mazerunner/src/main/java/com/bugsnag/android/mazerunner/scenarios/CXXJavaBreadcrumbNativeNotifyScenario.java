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
        String metadata = getEventMetaData();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        Bugsnag.leaveBreadcrumb("Initiate lift");
        Bugsnag.leaveBreadcrumb("Disable lift");
        activate();
    }
}
