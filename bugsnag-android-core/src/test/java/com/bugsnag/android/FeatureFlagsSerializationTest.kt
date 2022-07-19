package com.bugsnag.android

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class FeatureFlagsSerializationTest {
    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun testCases() = generateSerializationTestCases(
            "feature_flags",
            basic(),
            overrideVariants(),
            cleared()
        )

        private fun basic() = FeatureFlags().apply {
            addFeatureFlag("sample_group", "a")
            addFeatureFlag("demo_mode")
            addFeatureFlag("view_mode", "modern")
        }

        private fun overrideVariants() = FeatureFlags().apply {
            addFeatureFlag("sample_group", "a")
            addFeatureFlag("demo_mode")
            addFeatureFlag("sample_group", "b")
        }

        private fun cleared() = FeatureFlags().apply {
            addFeatureFlag("demo_mode")
            addFeatureFlag("sample_group", "a")
            addFeatureFlag("view_mode", "modern")

            clearFeatureFlags()
        }
    }

    @Parameterized.Parameter
    lateinit var testCase: Pair<FeatureFlags, String>

    @Test
    fun testJsonSerialisation() = verifyJsonMatches(testCase.first, testCase.second)
}
