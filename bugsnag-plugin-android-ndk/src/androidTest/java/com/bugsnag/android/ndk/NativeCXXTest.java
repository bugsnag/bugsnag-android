package com.bugsnag.android.ndk;

import org.junit.Test;

import static com.bugsnag.android.ndk.VerifyUtilsKt.verifyNativeRun;

public class NativeCXXTest {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("bugsnag-ndk-test");
    }

    public native int run();

    @Test
    public void testPassesNativeSuite() throws Exception {
        verifyNativeRun(run());
    }
}
