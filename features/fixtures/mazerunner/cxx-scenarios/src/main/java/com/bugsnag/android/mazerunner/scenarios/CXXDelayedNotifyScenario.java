package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;

public class CXXDelayedNotifyScenario extends Scenario {
    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native void activate();

    private boolean didActivate = false;
    private Handler handler = new Handler();

    public CXXDelayedNotifyScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        if (didActivate) {
            return;
        }
        didActivate = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                activate();
            }
        }, 3000);
    }
}
