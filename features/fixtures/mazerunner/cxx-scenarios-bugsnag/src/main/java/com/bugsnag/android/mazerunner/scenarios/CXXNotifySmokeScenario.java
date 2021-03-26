package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Event;
import com.bugsnag.android.OnErrorCallback;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXNotifySmokeScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("cxx-scenarios-bugsnag");
    }

    public native void activate();

    /**
     *
     */
    public CXXNotifySmokeScenario(@NonNull Configuration config,
                                  @NonNull Context context,
                                  @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
        config.setContext("FooContext");
    }

    @Override
    public void startScenario() {
        super.startScenario();

        Bugsnag.leaveBreadcrumb("Initiate lift");
        Bugsnag.leaveBreadcrumb("Disable lift");
        Bugsnag.addMetadata("TestData", "JVM", "pre notify()");
        Bugsnag.addMetadata("TestData", "password", "NotTellingYou");
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
