package com.bugsnag.android;

import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
public class StrictModeWrapper {

    public static void setUp() throws Exception {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .penaltyDeath()
            .build());
    }

    public static void tearDown() throws Exception {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.LAX);
    }

}
