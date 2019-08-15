package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectPackagesTest {

    @Test
    fun testDefaultProjectPackages() {
        val configuration = Configuration("api-key")
        assertTrue(configuration.projectPackages.isEmpty())

        val client = Client(ApplicationProvider.getApplicationContext<Context>(), configuration)
        assertEquals(setOf("com.bugsnag.android.core.test"), client.config.projectPackages)
        client.close()
    }

    @Test
    fun testProjectPackagesOverride() {
        val configuration = Configuration("api-key")
        configuration.projectPackages = setOf("com.foo.example")
        val client = Client(ApplicationProvider.getApplicationContext<Context>(), configuration)
        assertEquals(setOf("com.foo.example"), client.config.projectPackages)
        client.close()
    }
}
