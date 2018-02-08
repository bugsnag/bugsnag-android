package com.bugsnag.android.example;

import com.bugsnag.android.Bugsnag;

import android.app.Application;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Bugsnag.init(this);
    }

}
