package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Event;
import com.bugsnag.android.OnErrorCallback;

public class CXXNotifySmokeScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXNotifySmokeScenario(@NonNull Configuration config,
                                  @NonNull Context context,
                                  @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setAutoTrackSessions(false);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Bugsnag.addOnError(new OnErrorCallback() {
            @Override
            public boolean onError(@NonNull Event event) {
                event.addMetadata("TestData", "Source", "ClientCallback");
                return true;
            }
        });
        activate();
    }
}
