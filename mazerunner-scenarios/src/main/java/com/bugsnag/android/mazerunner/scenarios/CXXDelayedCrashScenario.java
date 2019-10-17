package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;

import java.lang.Runnable;

public class CXXDelayedCrashScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native int activate(int value);

    private boolean didActivate = false;
    private Handler handler = new Handler();

    public CXXDelayedCrashScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void run() {
        super.run();
        if (didActivate) {
            return;
        }
        didActivate = true;
        String metadata = getEventMetaData();
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
