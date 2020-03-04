package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.HashMap

class UserDeserializerTest {

    private val map = HashMap<String, Any>()

    /**
     * Generates a map for verifying the serializer
     */
    @Before
    fun setup() {
        map["id"] = "123"
        map["email"] = "joe@example.com"
        map["name"] = "Joe Bloggs"
    }

    @Test
    fun deserialize() {
        val user = UserDeserializer().deserialize(map)
        assertEquals("123", user.id)
        assertEquals("joe@example.com", user.email)
        assertEquals("Joe Bloggs", user.name)
    }
}
