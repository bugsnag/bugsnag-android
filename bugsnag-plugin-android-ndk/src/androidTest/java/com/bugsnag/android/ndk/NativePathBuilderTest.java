package com.bugsnag.android.ndk;

import static com.bugsnag.android.ndk.VerifyUtilsKt.verifyNativeRun;

import org.junit.Test;

public class NativePathBuilderTest {
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
