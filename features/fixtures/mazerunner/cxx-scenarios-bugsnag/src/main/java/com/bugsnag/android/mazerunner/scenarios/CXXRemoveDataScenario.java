package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXRemoveDataScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("cxx-scenarios-bugsnag");
    }

    public native void activate();

    public CXXRemoveDataScenario(@NonNull Configuration config,
                                 @NonNull Context context,
                                 @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        Bugsnag.addMetadata("persist", "keep", "foo");
        Bugsnag.addMetadata("persist", "remove", "not bar");
        Bugsnag.addMetadata("persist", "overwrite", "value1");
        Bugsnag.addMetadata("persist", "overwrite", "value2");

        // repetatively overwrite the "remove" attribute enough times to cause the metadata
        // to require cleaning, and ensure that the existing values are not dropped
        for (int overload = 0; overload < 256; overload++) {
            Bugsnag.addMetadata("persist", "remove", overload);
        }

        Bugsnag.addMetadata("persist", "remove", "bar");
        Bugsnag.addMetadata("remove", "foo", "bar");

        activate();
        Handler main = new Handler(Looper.getMainLooper());
        main.postDelayed(new Runnable() {
            @Override
            public void run() {
                Bugsnag.clearMetadata("remove");
                Bugsnag.clearMetadata("persist", "remove");
                activate();
            }
        }, 150);
    }
}
