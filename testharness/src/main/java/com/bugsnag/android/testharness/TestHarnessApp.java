package com.bugsnag.android.testharness;

import android.app.Application;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

public class TestHarnessApp extends Application {

    /**
     * See https://developer.android.com/studio/run/emulator-networking.html
     */
    private static final String MACHINE_IP = "http://10.0.2.2:9999";

    @Override
    public void onCreate() {
        super.onCreate();
        Configuration configuration = new Configuration("123");
        configuration.setEndpoint(MACHINE_IP);
        Bugsnag.init(this, configuration);
    }

}
