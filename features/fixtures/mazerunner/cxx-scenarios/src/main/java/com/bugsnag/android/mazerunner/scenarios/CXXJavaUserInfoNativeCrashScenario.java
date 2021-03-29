package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXJavaUserInfoNativeCrashScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native void crash();

    public CXXJavaUserInfoNativeCrashScenario(@NonNull Configuration config,
                                              @NonNull Context context,
                                              @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Bugsnag.setUser("9816734", "j@example.com", "Strulyegha  Ghaumon  "
                + "Rabelban  Snefkal  Angengtai  Samperris  Dreperwar Raygariss  Haytther "
                + " Ackworkin  Turdrakin  Clardon");
        crash();
    }
}
