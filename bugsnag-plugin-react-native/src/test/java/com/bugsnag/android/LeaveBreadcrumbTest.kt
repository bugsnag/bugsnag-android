package com.bugsnag.android

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class LeaveBreadcrumbTest {

    @Mock
    lateinit var client: Client

    private lateinit var plugin: BugsnagReactNativePlugin

    @Before
    fun setUp() {
        plugin = BugsnagReactNativePlugin()
        plugin.client = client
        plugin.logger = object : Logger {}
    }

    @Test
    fun leaveBreadcrumb() {
        val crumb = mutableMapOf<String, Any?>()
        crumb["message"] = "JS: invoked API"
        crumb["type"] = "request"

        val metadata = hashMapOf<String, Any?>(
            "customFoo" to "Flobber",
            "isJs" to true,
            "naughtyValue" to null
        )
        crumb["metadata"] = metadata

        // leave a breadcrumb and verify its structure
        plugin.leaveBreadcrumb(crumb)

        verify(client, times(1)).leaveBreadcrumb(
            eq("JS: invoked API"),
            eq(metadata),
            eq(BreadcrumbType.REQUEST)
        )
    }

    @Test
    fun leaveBreadcrumbNoMetadata() {
        // leave a breadcrumb and verify its structure
        val crumb = mutableMapOf<String, Any?>()
        crumb["message"] = "JS: invoked API"
        crumb["type"] = "request"
        plugin.leaveBreadcrumb(crumb)

        verify(client, times(1)).leaveBreadcrumb(
            eq("JS: invoked API"),
            eq(emptyMap()),
            eq(BreadcrumbType.REQUEST)
        )
    }
}
