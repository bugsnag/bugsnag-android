package com.bugsnag.android.mazerunner.scenarios;

import android.content.Context;

import com.bugsnag.android.Configuration;

import org.jetbrains.annotations.NotNull;

import android.content.Intent;
import android.os.Handler;

import java.lang.Runnable;

public class CXXBackgroundCrashScenario extends Scenario {
    static {
        System.loadLibrary("entrypoint");
    }

    public native int activate(int value);

    private boolean didActivate = false;
    private Handler handler = new Handler();

    public CXXBackgroundCrashScenario(@NotNull Configuration config, @NotNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
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
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        this.getContext().startActivity(intent);
    }
}
