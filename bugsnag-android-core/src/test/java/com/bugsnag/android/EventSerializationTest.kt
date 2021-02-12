package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateAppWithState
import com.bugsnag.android.BugsnagTestUtils.generateDeviceWithState
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date

@RunWith(Parameterized::class)
internal class EventSerializationTest {

    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): Collection<Pair<Event, String>> {
            return generateSerializationTestCases(
                "event",
                createEvent(),

                // custom context
                createEvent { it.context = "ExampleContext" },

                // custom grouping hash
                createEvent { it.groupingHash = "herpderp" },

                // custom severity
                createEvent { it.severity = Severity.INFO },

                // session included
                createEvent {
                    val user = User("123", "foo@example.com", "Joe")
                    it.session = Session("123", Date(0), user, false, Notifier(), NoopLogger)
                },

                // threads included
                createEvent {
                    val stacktrace = Stacktrace.stacktraceFromJavaTrace(arrayOf(), emptySet(), NoopLogger)
                    it.threads.clear()
                    it.threads.add(Thread(5, "main", ThreadType.ANDROID, true, stacktrace, NoopLogger))
                },

                // threads included
                createEvent {
                    it.addMetadata("app", "foo", 55)
                    it.addMetadata("device", "bar", true)
                    it.addMetadata("wham", "some_key", "A value")
                    it.setUser(null, null, "Jamie")

                    val crumb = Breadcrumb("hello world", BreadcrumbType.MANUAL, mutableMapOf(), Date(0), NoopLogger)
                    it.breadcrumbs = listOf(crumb)

                    val stacktrace = Stacktrace.stacktraceFromJavaTrace(arrayOf(), emptySet(), NoopLogger)
                    val err = Error(ErrorInternal("WhoopsException", "Whoops", stacktrace), NoopLogger)
                    it.errors.clear()
                    it.errors.add(err)
                }
            )
        }

        private fun createEvent(cb: (event: Event) -> Unit = {}): Event {
            val event = Event(
                null,
                generateImmutableConfig(),
                SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
                NoopLogger
            )
            event.threads.clear()
            event.app = generateAppWithState()
            event.device = generateDeviceWithState()
            event.device.cpuAbi = emptyArray()
            cb(event)
            return event
        }
    }

    @Parameter
    lateinit var testCase: Pair<Event, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
