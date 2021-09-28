package com.bugsnag.android

import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import okio.BufferedSink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.nio.charset.Charset

/**
 * Performs POST/PATCH/PUT/DELETE requests with bodies to verify network breadcrumbs are
 * captured.
 */
@RunWith(MockitoJUnitRunner::class)
class PostRequestIntegrationTest {

    @Mock
    lateinit var client: Client

    @Captor
    lateinit var mapCaptor: ArgumentCaptor<Map<String, Any>>

    @Before
    fun setup() {
        Mockito.`when`(client.config).thenReturn(BugsnagTestUtils.generateImmutableConfig())
    }

    /**
     * Sends a string in a POST request with a 200 response.
     */
    @Test
    fun postRequest200() {
        val body = """
          /\_/\
         ( o.o )
          > ^ <"""

        val mediaType = "text/plain; charset=utf-8".toMediaType()
        val request = Request.Builder().post(body.toRequestBody(mediaType))
        val mockResponse = MockResponse().setResponseCode(201).setBody("Meow")
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("POST", get("method"))
            assertEquals(201, get("status"))
            assertEquals(49L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Sends a form URL-encoded POST request with a 503 response.
     */
    @Test
    fun formUrlPostRequest503() {
        val body = FormBody.Builder()
            .add("fruit", "apple")
            .build()
        val request = Request.Builder().post(body)
        val mockResponse = MockResponse().setResponseCode(503).setBody("Service unavailable")
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call failed"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("POST", get("method"))
            assertEquals(503, get("status"))
            assertEquals(11L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Sends a POST request with a 200 response where the body is streamed.
     */
    @Test
    fun postStreamedRequest200() {
        val mediaType = "text/plain; charset=utf-8".toMediaType()
        val body = object : RequestBody() {
            override fun contentType() = mediaType

            override fun writeTo(sink: BufferedSink) {
                repeat(1000) {
                    sink.writeUtf8("Number: $it\n")
                }
            }
        }

        val request = Request.Builder().post(body)
        val buffer: Buffer = Buffer().writeString("OK", Charset.defaultCharset())
        val mockResponse = MockResponse().setResponseCode(200).setBody(buffer)
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("POST", get("method"))
            assertEquals(200, get("status"))
            assertEquals(11890L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Sends a multipart POST request with a 202 response.
     */
    @Test
    fun postMultipartRequest200() {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("title", "Hello World")
            .addFormDataPart("message", "Another bit with more detail")
            .build()

        val request = Request.Builder().post(body)
        val mockResponse = MockResponse().setResponseCode(202)
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("POST", get("method"))
            assertEquals(202, get("status"))
            assertEquals(303L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Performs a PUT request that returns 200.
     */
    @Test
    fun putRequest202() {
        val body = """
          /\_/\
         ( o.o )
          > ^ <"""

        val mediaType = "text/plain; charset=utf-8".toMediaType()
        val request = Request.Builder().put(body.toRequestBody(mediaType))
        val mockResponse = MockResponse().setResponseCode(202).setBody("Putted")
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("PUT", get("method"))
            assertEquals(202, get("status"))
            assertEquals(49L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Performs a PATCH request that returns 200.
     */
    @Test
    fun patchRequest202() {
        val body = """
          /\_/\
         ( o.o )
          > ^ <"""

        val mediaType = "text/plain; charset=utf-8".toMediaType()
        val request = Request.Builder().patch(body.toRequestBody(mediaType))
        val mockResponse = MockResponse().setResponseCode(202).setBody("Patched")
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("PATCH", get("method"))
            assertEquals(202, get("status"))
            assertEquals(49L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }

    /**
     * Performs a DELETE request that returns 200.
     */
    @Test
    fun deleteRequest200() {
        val request = Request.Builder().delete()
        val mockResponse = MockResponse().setResponseCode(200).setBody("Deleted")
        val url = makeNetworkBreadcrumbRequest(client, request, mockResponse)

        verify(client, times(1)).leaveBreadcrumb(
            eq("OkHttp call succeeded"),
            mapCaptor.capture(),
            eq(BreadcrumbType.REQUEST)
        )
        with(mapCaptor.value) {
            assertEquals("DELETE", get("method"))
            assertEquals(200, get("status"))
            assertEquals(0L, get("requestContentLength"))
            assertEquals(0L, get("responseContentLength"))
            assertTrue(get("duration") is Long)
            assertNull(get("urlParams"))
            assertEquals(url, get("url"))
        }
    }
}
