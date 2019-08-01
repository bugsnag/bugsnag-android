package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.BeforeSend;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Report;

import android.content.Context;
import android.support.annotation.NonNull;


public class NativeBeforeSendScenario extends Scenario {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("entrypoint");
    }

    public native void crash();

    public NativeBeforeSendScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.addBeforeSend(new BeforeSend() {
            @Override
            public boolean run(Report report) {
                report.getError().setContext("!important");

                return true;
            }
        });
    }

    @Override
    public void run() {
        super.run();
        crash();
    }
}
