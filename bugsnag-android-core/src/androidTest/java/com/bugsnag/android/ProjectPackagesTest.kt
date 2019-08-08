package com.bugsnag.android

import android.support.test.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectPackagesTest {

    @Test
    fun testDefaultProjectPackages() {
        val configuration = Configuration("api-key")
        assertNull(configuration.projectPackages)

        val client = Client(InstrumentationRegistry.getContext(), configuration)
        assertEquals(listOf("com.bugsnag.android.core.test"), client.config.projectPackages)
        client.close()
    }

    @Test
    fun testProjectPackagesOverride() {
        val configuration = Configuration("api-key")
        configuration.projectPackages = arrayOf("com.foo.example")
        val client = Client(InstrumentationRegistry.getContext(), configuration)
        assertEquals(listOf("com.foo.example"), client.config.projectPackages)
        client.close()
    }
}
