package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectPackagesTest {

    @Test
    fun testProjectPackagesOverride() {
        val configuration = Configuration("api-key")
        configuration.projectPackages = setOf("com.foo.example")
        val client = Client(ApplicationProvider.getApplicationContext<Context>(), configuration)
        assertEquals(setOf("com.foo.example"), client.immutableConfig.projectPackages)
        client.close()
    }
}
