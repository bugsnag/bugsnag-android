package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.BeforeSend;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Report;

import android.content.Context;
import androidx.annotation.NonNull;


public class NativeNotifyBeforeSendScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("entrypoint");
    }

    public native void activate();

    public NativeNotifyBeforeSendScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoTrackSessions(false);
        config.addBeforeSend(new BeforeSend() {
            @Override
            public boolean run(Report report) {
                report.getEvent().setContext("hello");

                return true;
            }
        });
    }

    @Override
    public void run() {
        super.run();
        activate();
    }
}
