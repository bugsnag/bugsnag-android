package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Event;
import com.bugsnag.android.OnErrorCallback;

public class CXXNotifySmokeScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXNotifySmokeScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.addOnError(new OnErrorCallback() {
            @Override
            public boolean onError(@NonNull Event event) {
                event.addMetadata("TestData", "Source", "ClientCallback");
                return true;
            }
        });
        activate();
    }
}
