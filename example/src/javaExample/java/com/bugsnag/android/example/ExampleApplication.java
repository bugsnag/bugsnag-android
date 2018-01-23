package com.bugsnag.android.example;

import android.app.Application;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Configuration config = new Configuration("f35a2472bd230ac0ab0f52715bbdc65d");
        config.setSessionEndpoint("http://10.0.2.2:8092");
        config.setEndpoint("http://10.0.2.2:8092");

        Bugsnag.init(this, config);
        Bugsnag.setAutoCaptureSessions(true);

    }

}
