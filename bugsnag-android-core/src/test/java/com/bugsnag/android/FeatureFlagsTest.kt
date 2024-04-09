package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Test

class FeatureFlagsTest {
    private lateinit var flags: FeatureFlags

    @Before
    fun createEmptyFeatureFlags() {
        flags = FeatureFlags()
    }

    @Test
    fun addDistinctFeatureFlag() {
        flags.addFeatureFlag("empty")
        flags.addFeatureFlag("keyWith", "value")
        flags.addFeatureFlag("otherKey", "another value")

        assertEquals(
            listOf(
                FeatureFlag("empty"),
                FeatureFlag("keyWith", "value"),
                FeatureFlag("otherKey", "another value")
            ),
            flags.toList()
        )
    }

    @Test
    fun overwriteFeatureFlags() {
        flags.addFeatureFlag("empty")
        flags.addFeatureFlag("keyWith", "value")
        flags.addFeatureFlag("otherKey", "another value")

        flags.addFeatureFlag("empty")
        flags.addFeatureFlag("keyWith", "overwrite value")
        flags.addFeatureFlag("newKey", "new value")

        flags.clearFeatureFlag("otherKey")
        flags.clearFeatureFlag("no such key")

        assertEquals(
            listOf(
                FeatureFlag("empty"),
                FeatureFlag("keyWith", "overwrite value"),
                FeatureFlag("newKey", "new value")
            ),
            flags.toList()
        )
    }

    @Test
    fun clearFeatureFlags() {
        // make sure clearing an empty table doesn't break anything
        flags.clearFeatureFlags()

        flags.addFeatureFlag("empty")
        flags.addFeatureFlag("keyWith", "value")
        flags.addFeatureFlag("otherKey", "another value")

        flags.clearFeatureFlags()

        assertEquals(emptyList<FeatureFlag>(), flags.toList())

        // make sure that clearing the list doesn't break adding new flags afterwards
        flags.addFeatureFlag("empty key")
        flags.addFeatureFlag("keyWith", "magic value")
        flags.addFeatureFlag("newKey", "new value")

        assertEquals(
            listOf(
                FeatureFlag("empty key"),
                FeatureFlag("keyWith", "magic value"),
                FeatureFlag("newKey", "new value")
            ),
            flags.toList()
        )
    }

    @Test
    fun addFeatureFlags() {
        flags.addFeatureFlag("empty")
        flags.addFeatureFlag("key1", "value1")
        flags.addFeatureFlag("key2", "value2")

        flags.addFeatureFlags(
            listOf(
                FeatureFlag("key1", "overwrite value"),
                FeatureFlag("bulk key1", "bulk value1"),
                FeatureFlag("bulk key2", "bulk value2"),
            )
        )

        flags.addFeatureFlags(
            sequence {
                repeat(5) { index ->
                    yield(FeatureFlag("feature $index"))
                }
            }.asIterable()
        )

        val cloned = flags.copy()
        assertNotSame(flags, cloned)

        // add an extra value and make sure it doesn't show up in 'cloned'
        flags.addFeatureFlag("extra value", "extra value")

        assertEquals(
            listOf(
                FeatureFlag("empty"),
                FeatureFlag("key1", "overwrite value"),
                FeatureFlag("key2", "value2"),
                FeatureFlag("bulk key1", "bulk value1"),
                FeatureFlag("bulk key2", "bulk value2"),
                FeatureFlag("feature 0"),
                FeatureFlag("feature 1"),
                FeatureFlag("feature 2"),
                FeatureFlag("feature 3"),
                FeatureFlag("feature 4")
            ),
            cloned.toList()
        )
    }
}
