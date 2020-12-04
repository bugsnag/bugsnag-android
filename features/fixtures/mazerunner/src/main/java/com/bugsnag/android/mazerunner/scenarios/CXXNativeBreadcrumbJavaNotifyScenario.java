package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;

import android.content.Context;

import com.bugsnag.android.Configuration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXNativeBreadcrumbJavaNotifyScenario extends Scenario {
    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXNativeBreadcrumbJavaNotifyScenario(@NonNull Configuration config,
                                                 @NonNull Context context,
                                                 @Nullable String eventMetadata) {
        super(config, context, eventMetadata);
    }

    @Override
    public void startScenario() {
        super.startScenario();
        activate();
        Bugsnag.notify(new Exception("Did not like"));
    }
}
