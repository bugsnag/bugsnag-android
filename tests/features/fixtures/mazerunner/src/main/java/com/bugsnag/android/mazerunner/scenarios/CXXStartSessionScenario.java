package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;
import android.os.Handler;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.TestHarnessHooksKt;

import android.support.annotation.NonNull;

public class CXXStartSessionScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    private Handler handler = new Handler();

    public native int crash(int counter);

    public CXXStartSessionScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        Bugsnag.getClient().startSession();
        TestHarnessHooksKt.flushAllSessions();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                crash(0);
            }
        }, 500);
    }
}
