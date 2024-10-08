package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateAppWithState
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateDeviceWithState
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.util.Date
import java.util.UUID

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
                    val apiKey = "BUGSNAG_API_KEY"
                    it.session =
                        Session("123", Date(0), user, false, Notifier(), NoopLogger, apiKey)
                },

                // threads included
                createEvent {
                    val stacktrace = Stacktrace(arrayOf(), emptySet(), NoopLogger)
                    it.threads.clear()
                    it.threads.add(
                        Thread(
                            "5",
                            "main",
                            ErrorType.ANDROID,
                            true,
                            Thread.State.RUNNABLE,
                            stacktrace,
                            NoopLogger
                        )
                    )
                },

                // threads included
                createEvent {
                    it.addMetadata("app", "foo", 55)
                    it.addMetadata("device", "bar", true)
                    it.addMetadata("wham", "some_key", "A value")
                    it.setUser(null, null, "Jamie")

                    val crumb = Breadcrumb(
                        "hello world",
                        BreadcrumbType.MANUAL,
                        mutableMapOf(),
                        Date(0),
                        NoopLogger
                    )
                    it.breadcrumbs = listOf(crumb)

                    val stacktrace = Stacktrace(arrayOf(), emptySet(), NoopLogger)
                    val err =
                        Error(ErrorInternal("WhoopsException", "Whoops", stacktrace), NoopLogger)
                    it.errors.clear()
                    it.errors.add(err)
                },

                // featureFlags included
                createEvent {
                    it.addFeatureFlag("no_variant")
                    it.addFeatureFlag("flag", "with_variant")
                },

                // with a trace correlation
                createEvent {
                    it.setTraceCorrelation(
                        UUID(0x24b8b82900d34da3, -0x659434b74a5b9edc),
                        0x3dbe7c7ae84945b9
                    )
                }
            )
        }

        private fun createEvent(cb: (event: Event) -> Unit = {}): Event {
            val event = Event(
                null,
                generateImmutableConfig(
                    generateConfiguration().apply {
                        projectPackages = setOf("com.example.foo")
                    }
                ),
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

    private val eventMapper = BugsnagEventMapper(NoopLogger)

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)

    @Test
    fun testJsonDeserializion() {
        verifyJsonParser(testCase.first, testCase.second) {
            Event(
                eventMapper.convertToEventImpl(it, "5d1ec5bd39a74caa1267142706a7fb21"),
                NoopLogger
            )
        }
    }
}
