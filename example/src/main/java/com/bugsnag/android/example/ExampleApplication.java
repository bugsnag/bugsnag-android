package com.bugsnag.android.example;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.bugsnag.android.Bugsnag;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Bugsnag client
        Bugsnag.init(this);

        if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyDeath()
                .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyDeath()
                .build());
        }

    }

}
