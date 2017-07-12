package com.bugsnag.android.example;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.bugsnag.android.Bugsnag;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the Bugsnag client
        Bugsnag.init(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .penaltyDeath()
                .build());

//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                .detectAll()
//                .penaltyDeath()
//                .build());
        }

        // TODO remove strictmode violation
        violateStrictModePolicy();

    }

    private void violateStrictModePolicy() {
        try {
            new FileWriter(new File(getCacheDir(), "test")).write("test");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
