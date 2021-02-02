package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.Runnable;
import java.util.concurrent.atomic.AtomicBoolean;

public class CXXDelayedCrashScenario extends Scenario {

    static {
        System.loadLibrary("cxx-scenarios");
    }

    public native int activate(int value);

    private final AtomicBoolean didActivate = new AtomicBoolean(false);
    private final Handler handler = new Handler();

    public CXXDelayedCrashScenario(@NonNull Configuration config,
                                   @NonNull Context context,
                                   @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        if (didActivate.getAndSet(true)) {
            return;
        }
        String metadata = getEventMetadata();
        if (metadata != null && metadata.equals("non-crashy")) {
            return;
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                activate(405);
            }
        }, 6000);
    }
}
