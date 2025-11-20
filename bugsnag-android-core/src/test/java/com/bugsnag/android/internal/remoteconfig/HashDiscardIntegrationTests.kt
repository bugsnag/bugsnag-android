package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.DiscardRule
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.JsonCollectionParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.Date

@RunWith(Parameterized::class)
internal class HashDiscardIntegrationTests {
    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun parameters(): List<HashDiscardFixture> = listOf(
            HashDiscardFixture("anr01", "anr_config01"),
            HashDiscardFixture("anr02", "anr_config01"),
            HashDiscardFixture("anr03", "anr_config01"),
            HashDiscardFixture("native_crash01", "native_crash_config"),
            HashDiscardFixture("java_unhandled01", "unhandled_config01"),
            HashDiscardFixture("java_unhandled01", "unhandled_config02")
        )
    }

    @Parameterized.Parameter
    lateinit var testData: HashDiscardFixture

    private lateinit var remoteConfig: RemoteConfig

    private lateinit var event: Map<String, Any>

    @Before
    fun loadFixtures() {
        event = jsonFrom(testData.event)

        remoteConfig = RemoteConfig.fromJsonMap(
            null,
            Date(Long.MAX_VALUE),
            jsonFrom(testData.config)
        )
    }

    @Test
    fun testDiscard() {
        val hashRule = remoteConfig.discardRules.filterIsInstance<DiscardRule.Hash>().singleOrNull()
        assertNotNull("there must be exactly 1 HASH rule in the fixture: $testData")

        val shouldDiscard = hashRule!!.shouldDiscardJson(event)
        assertTrue("expected the payload to be discarded: $testData", shouldDiscard)
    }

    @Test
    fun failDiscard() {
        val hashRule = remoteConfig.discardRules.filterIsInstance<DiscardRule.Hash>().singleOrNull()
        assertNotNull("there must be exactly 1 HASH rule in the fixture: $testData")

        val modifiedHashRule = hashRule!!.copy(matches = setOf("abc123"))

        val shouldDiscard = modifiedHashRule.shouldDiscardJson(event)
        assertFalse("expected the payload to be retained: $testData", shouldDiscard)
    }

    private fun jsonFrom(resource: String): Map<String, Any> {
        val resourceAsStream = this::class.java.getResourceAsStream("/remoteConfig/$resource.json")
        requireNotNull(resourceAsStream) { "resource not found: $resource" }
        @Suppress("UNCHECKED_CAST")
        return resourceAsStream.use {
            JsonCollectionParser(it).parse() as Map<String, Any>
        }
    }

    data class HashDiscardFixture(
        val event: String,
        val config: String
    )
}
