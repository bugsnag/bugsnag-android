package com.bugsnag.android.ndk;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class NativeCXXTest {

    static {
        System.loadLibrary("bugsnag-ndk-test");
    }

    public native int run();

    @Test
    public void testPassesNativeSuite() throws Exception {
        if (run() != 0) {
            throw new Exception("Check device logs for native test results");
        }
    }
}
