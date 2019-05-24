package com.bugsnag.android.mazerunner.scenarios;

import android.app.Activity;
import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.bugsnag.android.mazerunner.SecondActivity;

public class CXXUpdateContextCrashScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native int crash(int value);

    public CXXUpdateContextCrashScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
    }

    @Override
    public void run() {
        super.run();
        Context context = getContext();
        Bugsnag.setContext("Everest");

        if (context instanceof Activity) {
            Activity activity = (Activity)context;
            activity.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    crash(405);
                }
            }, 1000);
        }
    }
}
