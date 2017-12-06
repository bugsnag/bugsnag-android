package com.bugsnag.android.example;

import android.app.Application;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Configuration;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Bugsnag.init(this);

        Configuration config = new Configuration("9f2996871fb381de73bfb0bea455c28b");
        config.setSessionEndpoint("http://10.0.2.2:10000");
        config.setEndpoint("http://10.0.2.2:8000");
//        config.setAutoCaptureSessions(true);
        Bugsnag.init(this, config);
    }

}
