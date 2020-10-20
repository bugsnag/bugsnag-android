package com.bugsnag.android

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class UpdateMetadataTest {

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
    fun nullMetadataRemovesSection() {
        plugin.addMetadata("foo", null)
        verify(client, times(1)).clearMetadata("foo")
    }

    @Test
    fun metadataAddSection() {
        val data: HashMap<String, Any?> = hashMapOf(
            "customFoo" to "Flobber",
            "isJs" to true,
            "naughtyValue" to null
        )

        plugin.addMetadata("foo", data)
        verify(client, times(1)).addMetadata("foo", data)
    }
}
