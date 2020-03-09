package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public class CXXRemoveDataScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXRemoveDataScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.addMetadata("persist", "keep", "foo");
        Bugsnag.addMetadata("persist", "remove", "bar");
        Bugsnag.addMetadata("remove", "foo", "bar");
        activate();
        Handler main = new Handler(Looper.getMainLooper());
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                Bugsnag.clearMetadata("remove");
                Bugsnag.clearMetadata("persist", "remove");
                activate();
            }
        }, 150);
    }
}
