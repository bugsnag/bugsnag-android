package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.NativeInterface;
import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;

public class CXXTrapLaterDisabledScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void crash();

    public CXXTrapLaterDisabledScenario(@NonNull Configuration config, @NonNull Context context) {
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
        NativeInterface.disableNdkCrashReporting();
        crash();
    }
}
