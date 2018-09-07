package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.support.annotation.NonNull;

public class CXXJavaUserInfoNativeCrashScenario extends Scenario {
    static {
        System.loadLibrary("entrypoint");
    }

    public native void crash();

    public CXXJavaUserInfoNativeCrashScenario(@NonNull Configuration config, @NonNull Context context) {
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
        Bugsnag.setUser("9816734", "j@example.com", "J");
        crash();
    }
}
