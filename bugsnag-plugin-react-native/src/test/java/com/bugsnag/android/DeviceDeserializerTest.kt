package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.HashMap

class DeviceDeserializerTest {

    private val map = HashMap<String, Any>()

    /**
     * Generates a map for verifying the serializer
     */
    @Before
    fun setup() {
        map["cpuAbi"] = arrayOf("x86")
        map["jailbroken"] = true
        map["id"] = "509a0f934"
        map["locale"] = "en-US"
        map["totalMemory"] = 490209823402
        map["manufacturer"] = "google"
        map["model"] = "pixel 3a"
        map["osName"] = "android"
        map["osBuild"] = "bulldog"
        map["fingerprint"] = "foo"
        map["cpuAbis"] = arrayOf("x86")
        map["tags"] = "enabled"
        map["brand"] = "pixel"
        map["osVersion"] = "8.1"
        map["apiLevel"] = 27
        map["runtimeVersions"] = mapOf(Pair("androidApiLevel", "27"), Pair("osBuild", "bulldog"))
        map["freeDisk"] = 4092340985
        map["freeMemory"] = 50923422234
        map["orientation"] = "portrait"
        map["time"] = DateUtils.toIso8601(Date(0))
    }

    @Test
    fun deserialize() {
        val device = DeviceDeserializer().deserialize(map)
        assertArrayEquals(arrayOf("x86"), device.cpuAbi)
        assertTrue(device.jailbroken!!)
        assertEquals("509a0f934", device.id)
        assertEquals("en-US", device.locale)
        assertEquals(490209823402, device.totalMemory)
        assertEquals("google", device.manufacturer)
        assertEquals("pixel 3a", device.model)
        assertEquals("android", device.osName)
        assertEquals("8.1", device.osVersion)
        assertEquals(mapOf(Pair("androidApiLevel", "27"), Pair("osBuild", "bulldog")), device.runtimeVersions)
        assertEquals(4092340985, device.freeDisk)
        assertEquals(50923422234, device.freeMemory)
        assertEquals("portrait", device.orientation)
        assertEquals(0, device.time!!.time)
    }
}
