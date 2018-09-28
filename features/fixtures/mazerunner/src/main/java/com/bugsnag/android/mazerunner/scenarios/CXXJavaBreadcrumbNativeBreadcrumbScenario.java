package com.bugsnag.android.mazerunner.scenarios;


import android.content.Context;
import android.os.Handler;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.support.annotation.NonNull;

public class CXXJavaBreadcrumbNativeBreadcrumbScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    private Handler handler = new Handler();

    public CXXJavaBreadcrumbNativeBreadcrumbScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
    }

    @Override
    public void run() {
        super.run();
        String metadata = getEventMetaData();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        Bugsnag.leaveBreadcrumb("Reverse thrusters");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                activate();
            }
        }, 1000);
    }
}
