package com.bugsnag.android

import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito

/**
 * Test that [TraceParser.parseStackframe] returns `null` for stackframes that are not valid, but
 * does not cause exceptions.
 */
@RunWith(Parameterized::class)
class TraceParserInvalidStackframesTest {
    companion object {
        @JvmStatic
        @get:Parameterized.Parameters
        val stackTraces = listOf(
            "axed java.lang.Object.wait(Object.java:442)",
            "at java.lang.Object.wait)Native method(",
            "at java.lang.Object.wait)Object.java:442)",
            "at java.lang.Object.wait(Object.java:442(",
            "nananaaaa: /system/lib64/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+316) (BuildId: ee18e52b95e38eaab55a9a48518c8c3b)",
            "native: #02 pc 0000000000094690  /system/lib64/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+316)",
            "0000000000094690  /system/lib64/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+316)",
            "#01 pc 0000000000000c5c",
            "#01 pc 0000000000000c5c  ",
            "#01 pc 0000000000000c5c  /data/app/~~sKQbJGqVJA5glcnvEjZCMg==/com.example.bugsnag.android-fVuoJh5GpAL7sRAeI3vjSw==/lib/arm64/libentrypoint.so ",
            "#01 pc 0000000000000c5c  /data/app/~~sKQbJGqVJA5glcnvEjZCMg==/com.example.bugsnag.android-fVuoJh5GpAL7sRAeI3vjSw==/lib/arm64/libentrypoint.so (Java_com_example_bugsnag_android_BaseCrashyActivity_anrFromCXX+20",
            "#01 pc 0000000000000c5c  /data/app/~~sKQbJGqVJA5glcnvEjZCMg==/com.example.bugsnag.android-fVuoJh5GpAL7sRAeI3vjSw==/lib/arm64/libentrypoint.so (Java_com_example_bugsnag_android_BaseCrashyActivity_anrFromCXX+20) (",
            "#01 pc 0000000000000c5c  /data/app/~~sKQbJGqVJA5glcnvEjZCMg==/com.example.bugsnag.android-fVuoJh5GpAL7sRAeI3vjSw==/lib/arm64/libentrypoint.so (Java_com_example_bugsnag_android_BaseCrashyActivity_anrFromCXX+20) (BuildId: 7b4d63b86ec2611d13aa89960abc65771c9c9ce6",
            "#01 pc 0000000000000c5c  /data/app/~~sKQbJGqVJA5glcnvEjZCMg==/com.example.bugsnag.android-fVuoJh5GpAL7sRAeI3vjSw==/lib/arm64/libentrypoint.so Java_com_example_bugsnag_android_BaseCrashyActivity_anrFromCXX+20 BuildId: 7b4d63b86ec2611d13aa89960abc65771c9c9ce6",
            "#01 pc 0000000000000c5c  /data/app/~~sKQbJGqVJA5glcnvEjZCMg==/com.example.bugsnag.android-fVuoJh5GpAL7sRAeI3vjSw==/lib/arm64/libentrypoint.so (Java_com_example_bugsnag_android_BaseCrashyActivity_anrFromCXX+20) )BuildId: 7b4d63b86ec2611d13aa89960abc65771c9c9ce6(",
        )
    }

    private val traceParser = TraceParser(Mockito.mock(Logger::class.java), emptySet())

    @Parameterized.Parameter
    lateinit var stackFrame: String

    @Test
    fun parseFrameFails() {
        assertNull(stackFrame, traceParser.parseStackframe(stackFrame))
    }
}
