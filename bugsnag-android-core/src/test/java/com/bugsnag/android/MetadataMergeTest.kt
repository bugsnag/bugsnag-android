package com.bugsnag.android

import com.bugsnag.android.Metadata.Companion.merge
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.HashMap

class MetadataMergeTest {

    @Test
    fun testBasicMerge() {
        val base = Metadata()
        base.addMetadata("example", "name", "bob")
        base.addMetadata("example", "awesome", false)

        val overrides = Metadata()
        overrides.addMetadata("example", "age", 30)
        overrides.addMetadata("example", "awesome", true)

        val merged = merge(base, overrides)
        val tab = merged.getMetadata("example")
        assertEquals("bob", tab!!["name"])
        assertEquals(30, tab["age"])
        assertEquals(true, tab["awesome"])
    }

    @Test
    fun testDeepMerge() {
        val baseMap = mapOf(Pair("key", "fromBase"))
        val base = Metadata()
        base.addMetadata("example", "map", baseMap)

        val overridesMap = mapOf(Pair("key", "fromOverrides"))
        val overrides = Metadata()
        overrides.addMetadata("example", "map", overridesMap)

        val merged = merge(base, overrides)
        val tab = merged.getMetadata("example")
        val mergedMap = tab!!["map"] as Map<*, *>?
        assertEquals("fromOverrides", mergedMap!!["key"])
    }

    @Test
    fun testNestedMapMerge() {
        val metadata = Metadata()
        val alphaMap = mapOf("alphaKey" to "alphaValue")
        val betaMap = mapOf("betaKey" to "betaValue")
        val gammaMap = mapOf("gammaKey" to "gammaValue")

        metadata.addMetadata(
            "mytab",
            mapOf("alpha" to alphaMap)
        )
        metadata.addMetadata(
            "mytab",
            mapOf("beta" to betaMap)
        )
        metadata.addMetadata(
            "mytab",
            mapOf("gamma" to gammaMap)
        )
        assertEquals(alphaMap, metadata.getMetadata("mytab", "alpha"))
        assertEquals(betaMap, metadata.getMetadata("mytab", "beta"))
        assertEquals(gammaMap, metadata.getMetadata("mytab", "gamma"))
    }

    @Test
    fun testNestedMapMergeOverride() {
        val metadata = Metadata()
        val baseMap = mapOf(
            "alpha" to 1,
            "beta" to 2
        )
        metadata.addMetadata("mytab", mapOf("custom" to baseMap))
        assertEquals(baseMap, metadata.getMetadata("mytab", "custom"))

        val overrideMap = mapOf("alpha" to 5)
        metadata.addMetadata("mytab", mapOf("custom" to overrideMap))
        val expected = mapOf(
            "alpha" to 5,
            "beta" to 2
        )
        assertEquals(expected, metadata.getMetadata("mytab", "custom"))
    }

    @Test
    fun testConcurrentMapMerge() {
        merge(generateMetadata(), generateMetadata())
    }

    /**
     * Generates a metadata object with a tab value containing a map with a null entry
     */
    private fun generateMetadata(): Metadata {
        val metadata = Metadata()
        val nestedMap: MutableMap<Any, Any?> = HashMap()
        metadata.addMetadata("foo", "bar", nestedMap)
        nestedMap["whoops"] = null
        return metadata
    }
}
