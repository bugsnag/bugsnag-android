package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXUpdateContextCrashScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native int crash(int value);

    public CXXUpdateContextCrashScenario(@NonNull Configuration config,
                                         @NonNull Context context,
                                         @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
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
