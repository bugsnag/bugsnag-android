package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ThreadDeserializationTest {
    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Thread, String>> {
            return generateDeserializationTestCases(
                "thread",
                Thread(
                    ThreadInternal(
                        "riker",
                        "will.riker",
                        ErrorType.C,
                        false,
                        "",
                        Stacktrace(mutableListOf())
                    ),
                    NoopLogger
                ),
                Thread(
                    ThreadInternal(
                        "321",
                        "mayne",
                        ErrorType.ANDROID,
                        false,
                        "",
                        Stacktrace(mutableListOf())
                    ),
                    NoopLogger
                ),
                Thread(
                    ThreadInternal(
                        "1415926535897932384626433832795028841971693993751058209749445923078" +
                            "164062862089986280348253421170679821480865132823066470938446095" +
                            "505822317253594081284811174502841027019385211055596446229489549" +
                            "303819644288109756659334461284756482337867831652712019091456485" +
                            "669234603486104543266482",
                        "smoke signal handler",
                        ErrorType.ANDROID,
                        false,
                        "happy",
                        Stacktrace(mutableListOf())
                    ),
                    NoopLogger
                )
            )
        }
    }

    @Parameter
    lateinit var testCase: Pair<Thread, String>

    private val eventMapper = BugsnagEventMapper(NoopLogger)

    @Test
    fun testJsonDeserialization() =
        verifyJsonParser(testCase.first, testCase.second) {
            eventMapper.convertThread(it)
        }
}
