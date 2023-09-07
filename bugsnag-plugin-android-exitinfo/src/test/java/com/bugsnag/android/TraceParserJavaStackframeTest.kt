package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.mockito.Mockito.mock

@RunWith(Parameterized::class)
class TraceParserJavaStackframeTest {
    companion object {
        @JvmStatic
        @get:Parameters
        val stackTraces
            get() = listOf(
                "at java.lang.Object.wait(Object.java:442)" to
                    Stackframe("java.lang.Object.wait", "Object.java", 442, null),
                "at java.lang.Object.wait(Native method)" to
                    Stackframe("java.lang.Object.wait", "Native method", null, null),
                "at com.example.bugsnag.android.BaseCrashyActivity.\$r8\$lambda\$1-82lPEn83zsSIVw12fUel9TE6s(unavailable:0)" to
                    Stackframe(
                        "com.example.bugsnag.android.BaseCrashyActivity.\$r8\$lambda\$1-82lPEn83zsSIVw12fUel9TE6s",
                        "unavailable",
                        0,
                        null
                    ),
                "at com.android.internal.os.RuntimeInit\$MethodAndArgsCaller.run(RuntimeInit.java:548)" to
                    Stackframe(
                        "com.android.internal.os.RuntimeInit\$MethodAndArgsCaller.run",
                        "RuntimeInit.java",
                        548,
                        null
                    ),
            )
    }

    private val traceParser = TraceParser(mock(Logger::class.java), emptySet())

    @Parameter
    lateinit var stackFrame: Pair<String, Stackframe>

    @Test
    fun testJavaFrame() {
        val (line, expectedFrame) = stackFrame
        val parsedFrame = traceParser.parseJavaFrame(line)

        assertNotNull(parsedFrame)
        assertEquals(expectedFrame.method, parsedFrame!!.method)
        assertEquals(expectedFrame.file, parsedFrame.file)
        assertEquals(expectedFrame.lineNumber, parsedFrame.lineNumber)

        assertNull("inProject should be considered unknown", expectedFrame.inProject)
        assertNull("no 'code' should be associated", expectedFrame.code)
        assertNull("no columnNumber should be parsed", expectedFrame.columnNumber)
    }
}
