package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CXXNativeBreadcrumbJavaNotifyScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("cxx-scenarios-bugsnag");
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
