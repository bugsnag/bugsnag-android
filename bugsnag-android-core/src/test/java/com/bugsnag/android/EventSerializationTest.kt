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
                    it.groupingDiscriminator = "123456789"
                },

                createEvent {
                    createMetadataStressTest(it)
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
    fun testJsonDeserialization() {
        verifyJsonParser(testCase.first, testCase.second) {
            Event(
                eventMapper.convertToEventImpl(it, "5d1ec5bd39a74caa1267142706a7fb21"),
                NoopLogger
            )
        }
    }
}

private fun createMetadataStressTest(event: Event) {
    event.addMetadata(
        "utf8Testing",
        mapOf(
            "asciiOnly" to "Hello, World!",
            "latin1Range" to "¡Hola México!",
            "twoByteSequences" to "こんにちは",
            "threeByteSequences" to "♠♣♥♦",
            "fourByteSequences" to "🌍🌎🌏",
            "mixedLength" to "Hello™●♠€한🌍",
            "surrogates" to "\uD83D\uDE00\uD83D\uDE03\uD83D\uDE04",
            "allEscapes" to "\"\\/\b\u000c\n\r\t\u0001",
            "longString" to "prefix_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa_suffix"
        )
    )

    event.addMetadata(
        "numberEdgeCases", mapOf(
            "integers" to mapOf(
                "zero" to 0,
                "minusZero" to -0,
                "positiveOne" to 1,
                "negativeOne" to -1,
                "longMax" to Long.MAX_VALUE,
                "longMin" to Long.MIN_VALUE
            ),
            "decimals" to mapOf(
                "simpleDecimal" to 123.456,
                "negativeDecimal" to -123.456,
                "leadingZero" to 0.123,
                "trailingZero" to 123.0,
                "manyDecimals" to 0.123456789012345
            ),
            "exponents" to mapOf(
                "positiveExp" to 1.23e+11,
                "negativeExp" to 1.23e-11,
                "zeroExp" to 0e0,
                "largeExp" to 1.23e+308,
                "tinyExp" to 1.23e-308
            )
        )
    )

    event.addMetadata(
        "structuralTests", mapOf(
            "deepNesting" to mapOf(
                "level1" to mapOf(
                    "level2" to mapOf(
                        "level3" to mapOf(
                            "level4" to mapOf(
                                "level5" to "deep value"
                            )
                        )
                    )
                )
            ),
            "arrayTests" to mapOf(
                "empty" to emptyList<Any>(),
                "nested" to listOf(
                    listOf(1, 2),
                    listOf(3, listOf(4, 5)),
                    listOf(6, listOf(7, listOf(8, 9)))
                ),
                "mixed" to listOf(
                    null,
                    true,
                    false,
                    123,
                    "string",
                    listOf("nested"),
                    mapOf("key" to "value")
                )
            ),
            "specialValues" to mapOf(
                "nullValue" to null,
                "boolTrue" to true,
                "boolFalse" to false,
                "emptyString" to "",
                "emptyObject" to emptyMap<String, Any>(),
                "objectWithEmpty" to mapOf("" to "")
            )
        )
    )
}
