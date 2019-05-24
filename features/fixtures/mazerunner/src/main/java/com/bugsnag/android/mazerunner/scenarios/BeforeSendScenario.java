package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.BeforeSend;
import com.bugsnag.android.Configuration;
import com.bugsnag.android.Report;

import android.content.Context;
import android.support.annotation.NonNull;


public class BeforeSendScenario extends Scenario {

    public BeforeSendScenario(@NonNull Configuration config, @NonNull Context context) {
        super(config, context);
        config.setAutoCaptureSessions(false);
        config.beforeSend(new BeforeSend() {
            @Override
            public boolean run(Report report) {
                report.getError().setContext("UNSET");

                return true;
            }
        });
    }

    @Override
    public void run() {
        super.run();
        throw new RuntimeException("Ruh-roh");
    }
}
