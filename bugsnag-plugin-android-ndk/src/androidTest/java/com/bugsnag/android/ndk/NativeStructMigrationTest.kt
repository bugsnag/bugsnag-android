package com.bugsnag.android.ndk

class NativeStructMigrationTest {

    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("bugsnag-ndk-test")
        }
    }

    external fun run(): Int

// TODO PLAT-7589
//    @Test
//    fun testPassesNativeSuite() {
//        verifyNativeRun(run())
//    }
}
