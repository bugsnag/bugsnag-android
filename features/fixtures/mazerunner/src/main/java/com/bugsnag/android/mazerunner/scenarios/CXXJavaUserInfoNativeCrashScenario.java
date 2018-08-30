package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import org.jetbrains.annotations.NotNull;

public class CXXJavaUserInfoNativeCrashScenario extends Scenario {
    static {
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXJavaUserInfoNativeCrashScenario(@NotNull Configuration config, @NotNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        String metadata = getEventMetaData();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        Bugsnag.setUser("9816734", "j@example.com", "J");
        activate();
    }
}
