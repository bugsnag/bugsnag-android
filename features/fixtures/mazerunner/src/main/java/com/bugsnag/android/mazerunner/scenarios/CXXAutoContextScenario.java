package com.bugsnag.android.mazerunner.scenarios;

import android.app.Activity;
import android.content.Context;

import com.bugsnag.android.Configuration;

import android.content.Intent;
import android.support.annotation.NonNull;

import com.bugsnag.android.mazerunner.SecondActivity;

public class CXXAutoContextScenario extends Scenario {

    static {
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
        context.startActivity(new Intent(context, SecondActivity.class));

        if (context instanceof Activity) {
            Activity activity = (Activity)context;
            activity.getWindow().getDecorView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    activate();
                }
            }, 2000);
        }
    }
}
