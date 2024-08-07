package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class ErrorSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases() = generateSerializationTestCases(
            "error",
            Error(ErrorInternal("foo", "bar", Stacktrace(mutableListOf())), NoopLogger),
            Error(
                ErrorInternal(
                    "foo",
                    "bar",
                    Stacktrace(
                        mutableListOf(
                            Stackframe(
                                method = "foo()",
                                file = "Bar.kt",
                                lineNumber = 55,
                                inProject = true,
                                columnNumber = 99,
                                code = mapOf("54" to "invoke()")
                            ),
                        )
                    )
                ),
                NoopLogger
            ),
            Error(
                ErrorInternal(
                    "com.bugsnag.android.StacktraceSerializationTest",
                    "bar",
                    Stacktrace(
                        mutableListOf(
                            Stackframe(
                                method = "com.bugsnag.android.StacktraceSerializationTest\$Companion.inProject",
                                file = "StacktraceSerializationTest.kt",
                                lineNumber = 47,
                                inProject = true,
                            ),
                            Stackframe(
                                method = "com.bugsnag.android.StacktraceSerializationTest\$Companion.testCases",
                                file = "StacktraceSerializationTest.kt",
                                lineNumber = 31,
                                inProject = true,
                            )
                        )
                    )
                ),
                NoopLogger
            )
        )
    }

    @Parameter
    lateinit var testCase: Pair<Error, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
