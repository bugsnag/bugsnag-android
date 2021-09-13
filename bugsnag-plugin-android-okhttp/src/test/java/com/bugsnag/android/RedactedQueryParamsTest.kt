package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttpPlugin
import okhttp3.Call
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RedactedQueryParamsTest {

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var call: Call

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    /**
     * Verifies that sensitive keys are redacted from network breadcrumb metadata
     */
    @Test
    fun testRedactedQueryParams() {
        val request = Request.Builder().url(
            "https://example.com?debug=true&password=hunter2&fruit=apple&fruit=banana"
        ).build()
        `when`(call.request()).thenReturn(request)
        `when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())

        BugsnagOkHttpPlugin { 0 }.apply {
            load(client)
            callStart(call)
            callEnd(call)
        }

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        assertEquals(
            mapOf(
                "debug" to "true",
                "fruit" to listOf("apple", "banana"),
                "password" to "[REDACTED]"
            ),
            mapCaptor.value["urlParams"]
        )
    }
}
