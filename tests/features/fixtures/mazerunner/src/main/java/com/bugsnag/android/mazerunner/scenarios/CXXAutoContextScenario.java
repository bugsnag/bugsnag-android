package com.bugsnag.android.mazerunner.scenarios;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.mazerunner.SecondActivity;

import android.support.annotation.NonNull;

public class CXXAutoContextScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("monochrome");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public CXXAutoContextScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
    }

    @Override
    public void run() {
        super.run();
        Context context = getContext();
        registerActivityLifecycleCallbacks();
        context.startActivity(new Intent(context, SecondActivity.class));
    }

    @Override
    public void onActivityStarted(Activity activity) {
        activate();
    }
}
