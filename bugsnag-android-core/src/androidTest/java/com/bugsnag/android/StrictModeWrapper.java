package com.bugsnag.android;

import android.os.StrictMode;

class StrictModeWrapper {

    static void setUp() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .penaltyDeath()
            .build());
    }

    static void tearDown() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
    }

}
