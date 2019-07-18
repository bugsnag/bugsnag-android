package com.bugsnag.android;

import android.os.StrictMode;

class StrictModeWrapper {

    static void setUp() throws Exception {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .penaltyDeath()
            .build());
    }

    static void tearDown() throws Exception {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
    }

}
