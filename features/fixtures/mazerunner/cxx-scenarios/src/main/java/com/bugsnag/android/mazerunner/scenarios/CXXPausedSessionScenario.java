package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.TestHarnessHooksKt;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXPausedSessionScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    private Handler handler = new Handler();

    public native int crash(int counter);

    public CXXPausedSessionScenario(@NonNull Configuration config,
                                    @NonNull Context context,
                                    @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Bugsnag.getClient().startSession();
        Bugsnag.getClient().pauseSession();
        TestHarnessHooksKt.flushAllSessions();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                crash(0);
            }
        }, 500);
    }
}
