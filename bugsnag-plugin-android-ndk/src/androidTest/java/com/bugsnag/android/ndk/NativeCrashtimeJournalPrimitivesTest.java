package com.bugsnag.android.ndk;

import static com.bugsnag.android.ndk.VerifyUtilsKt.verifyNativeRun;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NativeCrashtimeJournalPrimitivesTest {

    static {
        System.loadLibrary("bugsnag-ndk");
        System.loadLibrary("bugsnag-ndk-test");
    }

    public native int run(String path);

    @Test
    public void testPassesNativeSuite() throws Exception {
        TemporaryFolder folder = new TemporaryFolder();
        folder.create();
        verifyNativeRun(run(folder.newFolder().toString()));
        folder.delete();
    }
}
