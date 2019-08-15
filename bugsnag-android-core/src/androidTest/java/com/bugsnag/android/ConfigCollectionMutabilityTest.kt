package com.bugsnag.android

import org.junit.Test
import java.lang.UnsupportedOperationException

class ConfigCollectionMutabilityTest {

    private val configuration = Configuration("api-key")

    @Test(expected = UnsupportedOperationException::class)
    fun testProjectPackages() {
        configuration.projectPackages.add("")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testIgnoreClasses() {
        configuration.ignoreClasses.add("")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testNotifyReleaseStages() {
        configuration.notifyReleaseStages.add("")
    }

    @Test(expected = UnsupportedOperationException::class)
    fun testFilters() {
        configuration.filters.add("")
    }
}
