package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import com.bugsnag.android.internal.JsonHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class RemoteConfigSerializationTest {

    companion object {

        @JvmStatic
        @Parameters
        fun testCases() = generateSerializationTestCases(
            "remoteconfig",
            shouldDiscardAll(
                RemoteConfig(
                    "tag123",
                    DateUtils.fromIso8601("2024-01-15T10:30:45.123Z"),
                    listOf(DiscardRule.All, DiscardRule.AllHandled)
                )
            ),
            shouldDiscardNothing(
                RemoteConfig(
                    "tag456",
                    DateUtils.fromIso8601("2024-01-15T10:30:45.123Z"),
                    emptyList()
                )
            ),
            shouldDiscardNothing(
                RemoteConfig(
                    "tag789",
                    DateUtils.fromIso8601("2024-01-15T10:30:45.123Z"),
                    listOf(
                        DiscardRule.Hash(
                            listOf(
                                mapOf(
                                    "path" to "exceptions.-1.errorClass"
                                )
                            ),
                            setOf("1234567890abcdef", "abcdef1234567890")
                        )
                    )
                )
            )
        )

        private fun shouldDiscardNothing(remoteConfig: RemoteConfig) = RemoteConfigExpectation(
            remoteConfig,
            shouldDiscardHandled = false,
            shouldDiscardUnhandled = false
        )

        private fun shouldDiscardUnhandled(remoteConfig: RemoteConfig) = RemoteConfigExpectation(
            remoteConfig,
            shouldDiscardHandled = false,
            shouldDiscardUnhandled = true
        )

        private fun shouldDiscardAll(remoteConfig: RemoteConfig) = RemoteConfigExpectation(
            remoteConfig,
            shouldDiscardHandled = true,
            shouldDiscardUnhandled = true
        )
    }

    @Parameter
    lateinit var testCase: Pair<RemoteConfigExpectation, String>

    private val expectation: RemoteConfigExpectation
        get() = testCase.first

    private val expectedConfig: RemoteConfig
        get() = expectation.remoteConfig

    private val resourceName: String
        get() = testCase.second

    @Test
    fun testJsonSerialization() = verifyJsonMatches(expectedConfig, resourceName)

    @Test
    fun testDiscardHandled() = testDiscard(expectation.shouldDiscardHandled, unhandledEvent = false)

    @Test
    fun testDiscardUnhandled() =
        testDiscard(expectation.shouldDiscardUnhandled, unhandledEvent = true)

    private fun testDiscard(shouldDiscard: Boolean, unhandledEvent: Boolean) {
        val immutableConfig = BugsnagTestUtils.generateImmutableConfig()
        val event = BugsnagTestUtils.generateEvent()
        event.isUnhandled = unhandledEvent

        val payload = EventPayload(
            immutableConfig.apiKey,
            event,
            null,
            Notifier(),
            immutableConfig
        )

        val deserializedConfig = RemoteConfig.fromJsonMap(
            JsonHelper.deserialize(
                JsonParser().read(resourceName).byteInputStream()
            )
        )

        assertEquals(
            buildString {
                append("unexpected discard of ")
                if (unhandledEvent) {
                    append("un")
                }

                append("handled event")
            },
            shouldDiscard,
            deserializedConfig!!.discardRules.any { it.shouldDiscard(payload) }
        )
    }

    data class RemoteConfigExpectation(
        val remoteConfig: RemoteConfig,
        val shouldDiscardHandled: Boolean,
        val shouldDiscardUnhandled: Boolean,
    )
}
