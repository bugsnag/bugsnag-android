package com.bugsnag.android.testharness;

import android.app.Application;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

public class TestHarnessApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Configuration configuration = new Configuration("123");
        configuration.setEndpoint("http://localhost:9999");
        Bugsnag.init(this, configuration);
    }

}
