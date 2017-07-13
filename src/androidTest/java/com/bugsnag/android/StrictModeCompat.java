package com.bugsnag.android;

import android.os.Build;

/**
 * Wraps {@link android.os.StrictMode} in a class so a {@link VerifyError} doesn't get thrown on
 * lower API levels
 */
public class StrictModeCompat {

    public static void setUp() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictModeWrapper.setUp();
        }
    }

    public static void tearDown() throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            StrictModeWrapper.tearDown();
        }
    }

}
