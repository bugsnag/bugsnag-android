package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mockito

@RunWith(Parameterized::class)
class TraceParserNativeStackframeTest {
    companion object {
        @JvmStatic
        @get:Parameterized.Parameters
        val stackTraces
            get() = listOf(
                "native: #02 pc 0000000000094690  /system/lib64/libbinder.so (android::IPCThreadState::joinThreadPool(bool)+316) (BuildId: ee18e52b95e38eaab55a9a48518c8c3b)"
                    to nativeStackframe(
                        "android::IPCThreadState::joinThreadPool(bool)",
                        "/system/lib64/libbinder.so",
                        0x94690L,
                        "ee18e52b95e38eaab55a9a48518c8c3b",
                    ),
                "native: #06 pc 0000000000250c9c  /system/lib64/libhwui.so (void* std::__1::__thread_proxy<std::__1::tuple<std::__1::unique_ptr<std::__1::__thread_struct, std::__1::default_delete<std::__1::__thread_struct> >, android::uirenderer::CommonPool::CommonPool()::\$_0> >(void*) (.__uniq.99815402873434996937524029735804459536)+40) (BuildId: 5e787210ce0f171dbee073e4a14a376c)"
                    to nativeStackframe(
                        "void* std::__1::__thread_proxy<std::__1::tuple<std::__1::unique_ptr<std::__1::__thread_struct, std::__1::default_delete<std::__1::__thread_struct> >, android::uirenderer::CommonPool::CommonPool()::\$_0> >(void*) (.__uniq.99815402873434996937524029735804459536)",
                        "/system/lib64/libhwui.so",
                        0x250c9cL,
                        "5e787210ce0f171dbee073e4a14a376c",
                    ),
                "#01 pc 0000000000000c5c  /data/app/~~sKQbJGqVJA5glcnvEjZCMg==/com.example.bugsnag.android-fVuoJh5GpAL7sRAeI3vjSw==/lib/arm64/libentrypoint.so (Java_com_example_bugsnag_android_BaseCrashyActivity_anrFromCXX+20) (BuildId: 7b4d63b86ec2611d13aa89960abc65771c9c9ce6)"
                    to nativeStackframe(
                        "Java_com_example_bugsnag_android_BaseCrashyActivity_anrFromCXX",
                        "/data/app/~~sKQbJGqVJA5glcnvEjZCMg==/com.example.bugsnag.android-fVuoJh5GpAL7sRAeI3vjSw==/lib/arm64/libentrypoint.so",
                        0xc5cL,
                        "7b4d63b86ec2611d13aa89960abc65771c9c9ce6",
                    ),
                "#10 pc 0000000000576414  /apex/com.android.art/lib64/libart.so (bool art::interpreter::DoCall<false, false>(art::ArtMethod*, art::Thread*, art::ShadowFrame&, art::Instruction const*, unsigned short, art::JValue*)+1496) (BuildId: e24a1818231cfb1649cb83a5d2869598)"
                    to nativeStackframe(
                        "bool art::interpreter::DoCall<false, false>(art::ArtMethod*, art::Thread*, art::ShadowFrame&, art::Instruction const*, unsigned short, art::JValue*)",
                        "/apex/com.android.art/lib64/libart.so",
                        0x576414L,
                        "e24a1818231cfb1649cb83a5d2869598",
                    ),
            )

        private fun nativeStackframe(
            method: String?,
            file: String?,
            lineNumber: Number?,
            buildId: String,
        ): Stackframe = Stackframe(
            method,
            file,
            lineNumber,
            null,
            null
        ).apply {
            codeIdentifier = buildId
        }
    }

    private val traceParser = TraceParser(Mockito.mock(Logger::class.java), emptySet())

    @Parameterized.Parameter
    lateinit var stackFrame: Pair<String, Stackframe>

    @Test
    fun testNativeFrame() {
        val (line, expectedFrame) = stackFrame
        val parsedFrame = traceParser.parseNativeFrame(line)

        assertNotNull(parsedFrame)
        assertEquals(expectedFrame.method, parsedFrame!!.method)
        assertEquals(expectedFrame.file, parsedFrame.file)
        assertEquals(expectedFrame.lineNumber, parsedFrame.lineNumber)
        assertEquals(expectedFrame.codeIdentifier, parsedFrame.codeIdentifier)

        assertNull("inProject should be considered unknown", expectedFrame.inProject)
        assertNull("no 'code' should be associated", expectedFrame.code)
        assertNull("no columnNumber should be parsed", expectedFrame.columnNumber)
    }
}
