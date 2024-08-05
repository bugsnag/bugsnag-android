package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.lang.IllegalStateException

@RunWith(Parameterized::class)
internal class StacktraceSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Stacktrace, String>> {
            val frame = Stackframe("foo()", "Bar.kt", 55, true, mapOf(Pair("54", "invoke()")), 99)
            return generateSerializationTestCases(
                "stacktrace",

                // empty stacktrace element ctor
                Stacktrace(arrayOf(), emptySet(), NoopLogger),

                // empty custom frames ctor
                Stacktrace(mutableListOf(frame)),

                // basic
                basic(),

                // in project frames
                inProject(),

                // stacktrace trimming
                trimStacktrace(),
                trimStacktraceListCtor()
            )
        }

        private fun basic() =
            Stacktrace(
                RuntimeException("Whoops").stackTrace.sliceArray(IntRange(0, 1)),
                emptySet(),
                NoopLogger
            )

        private fun inProject() = Stacktrace(
            RuntimeException("Whoops").stackTrace.sliceArray(IntRange(0, 1)),
            setOf("com.bugsnag.android"),
            NoopLogger
        )

        private fun trimStacktrace(): Stacktrace {
            val elements = (0..999).map {
                StackTraceElement("SomeClass", "someMethod", "someFile", it)
            }
            return Stacktrace(elements.toTypedArray(), emptyList(), NoopLogger)
        }

        private fun trimStacktraceListCtor(): Stacktrace {
            val elements = (0..999).mapTo(ArrayList()) { count ->
                Stackframe("Foo", "Bar.kt", count, true).also { frame ->
                    // set different type for each frame
                    frame.type = when (count % 3) {
                        0 -> ErrorType.C
                        1 -> ErrorType.ANDROID
                        2 -> ErrorType.REACTNATIVEJS
                        3 -> null
                        else -> throw IllegalStateException()
                    }
                }
            }
            return Stacktrace(elements)
        }
    }

    @Parameter
    lateinit var testCase: Pair<Stacktrace, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
