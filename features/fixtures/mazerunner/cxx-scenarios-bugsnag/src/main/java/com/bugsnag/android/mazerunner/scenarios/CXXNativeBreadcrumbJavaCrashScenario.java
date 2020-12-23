package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;

public class CXXNativeBreadcrumbJavaCrashScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("cxx-scenarios-bugsnag");
    }

    public native void activate();

    public CXXNativeBreadcrumbJavaCrashScenario(@NonNull Configuration config,
                                                @NonNull Context context) {
        super(config, context);
    }

    @Override
    public void run() {
        super.run();
        activate();
        String[] items = new String[]{"one","two"};
        System.out.println("Last item is: " + items[2]);
    }
}
