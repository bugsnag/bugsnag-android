package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXDelayedNotifyScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios-bugsnag");
    }

    public native void activate();

    private boolean didActivate = false;
    private Handler handler = new Handler();

    public CXXDelayedNotifyScenario(@NonNull Configuration config,
                                    @NonNull Context context,
                                    @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
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
