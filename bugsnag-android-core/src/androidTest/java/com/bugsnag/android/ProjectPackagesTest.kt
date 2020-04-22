package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectPackagesTest {

    @Test
    fun testDefaultProjectPackages() {
        val configuration = generateConfiguration()
        assertTrue(configuration.projectPackages.isEmpty())

        val client = Client(ApplicationProvider.getApplicationContext<Context>(), configuration)
        assertEquals(setOf("com.bugsnag.android.core.test"), client.config.projectPackages)
        client.close()
    }

    @Test
    fun testProjectPackagesOverride() {
        val configuration = generateConfiguration()
        configuration.projectPackages = setOf("com.foo.example")
        val client = Client(ApplicationProvider.getApplicationContext<Context>(), configuration)
        assertEquals(setOf("com.foo.example"), client.config.projectPackages)
        client.close()
    }
}
