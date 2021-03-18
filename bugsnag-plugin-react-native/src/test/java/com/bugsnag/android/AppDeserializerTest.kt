package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.HashMap

class AppDeserializerTest {

    private val map = HashMap<String, Any>()

    /**
     * Generates a map for verifying the serializer
     */
    @Before
    fun setup() {
        map["binaryArch"] = "x86"
        map["id"] = "com.example.foo"
        map["releaseStage"] = "prod"
        map["version"] = "1.5.3"
        map["buildUuid"] = "build-uuid-123"
        map["codeBundleId"] = "code-id-123"
        map["type"] = "android"
        map["versionCode"] = 55
        map["duration"] = 2094
        map["durationInForeground"] = 1095
        map["inForeground"] = true
        map["isLaunching"] = true
    }

    @Test
    fun deserialize() {
        val app = AppDeserializer().deserialize(map)
        assertEquals("x86", app.binaryArch)
        assertEquals("com.example.foo", app.id)
        assertEquals("prod", app.releaseStage)
        assertEquals("1.5.3", app.version)
        assertEquals("build-uuid-123", app.buildUuid)
        assertEquals("code-id-123", app.codeBundleId)
        assertEquals("android", app.type)
        assertEquals(55, app.versionCode)
        assertEquals(2094, app.duration)
        assertEquals(1095, app.durationInForeground)
        assertTrue(app.inForeground!!)
        assertTrue(app.isLaunching!!)
    }
}
