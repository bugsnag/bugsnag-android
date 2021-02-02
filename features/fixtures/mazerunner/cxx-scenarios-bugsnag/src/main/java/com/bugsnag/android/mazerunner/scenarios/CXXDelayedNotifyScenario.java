package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

public class CXXDelayedNotifyScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios-bugsnag");
    }

    public native void activate();

    private final AtomicBoolean didActivate = new AtomicBoolean(false);
    private final Handler handler = new Handler();

    public CXXDelayedNotifyScenario(@NonNull Configuration config,
                                    @NonNull Context context,
                                    @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        if (didActivate.getAndSet(true)) {
            return;
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                activate();
            }
        }, 3000);
    }
}
