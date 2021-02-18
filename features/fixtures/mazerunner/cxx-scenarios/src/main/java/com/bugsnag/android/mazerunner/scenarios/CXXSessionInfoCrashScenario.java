package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.TestHarnessHooksKt;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXSessionInfoCrashScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native int crash(int value);

    private Handler handler = new Handler();

    public CXXSessionInfoCrashScenario(@NonNull Configuration config,
                                       @NonNull Context context,
                                       @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        String metadata = getEventMetadata();

        Bugsnag.startSession();
        TestHarnessHooksKt.flushAllSessions();
        Bugsnag.notify(new Exception("For the first"));
        Bugsnag.notify(new Exception("For the second"));
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                crash(3837);
            }
        }, 2500);
    }
}
