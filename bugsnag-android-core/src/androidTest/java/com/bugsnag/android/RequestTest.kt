package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RequestTest {
    private val testBodyString = "this is a body string with some content"
    private val customBodyLength = 5000L

    @Test
    fun urlQueryIsExtracted() {
        val request = Request(
            "1.1",
            "GET",
            "http://localhost/test?t1=arg1&test2=argument+2"
        )

        assertEquals("http://localhost/test", request.url)
        assertEquals("arg1", request.getQueryParameter("t1"))
        assertEquals("argument 2", request.getQueryParameter("test2"))
        assertEquals(setOf("t1", "test2"), request.queryParameterNames)
    }

    @Test
    fun setBody() {
        val request = Request("1.1", "GET", "http://localhost/")
        request.body = testBodyString
        assertEquals(testBodyString, request.body)
    }

    @Test
    fun setBodyWithUserLength() {
        val request = Request("1.1", "GET", "http://localhost/")
        request.bodyLength = customBodyLength
        request.body = testBodyString

        assertEquals(testBodyString, request.body)
        assertEquals(customBodyLength, request.bodyLength)
    }

    @Test
    fun setNullBody() {
        val request = Request("1.1", "GET", "http://localhost/")
        request.body = testBodyString
        request.body = null
        assertNull(request.body)
    }

    @Test
    fun setHttpMethod() {
        val request = Request("1.1", "GET", "http://localhost/")
        assertEquals("GET", request.httpMethod)

        request.httpMethod = "POST"
        assertEquals("POST", request.httpMethod)
    }

    @Test
    fun setHttpVersion() {
        val request = Request("1.1", "1.1", "http://localhost/")
        assertEquals("1.1", request.httpVersion)

        request.httpVersion = "1.0"
        assertEquals("1.0", request.httpVersion)
    }

    @Test
    fun setUrl() {
        val request = Request("1.1", "GET", "http://localhost/")
        assertEquals("http://localhost/", request.url)

        request.url = "https://google.com"
        assertEquals("https://google.com", request.url)
    }

    @Test
    fun setUrlWithQuery() {
        val request = Request("1.1", "GET", "http://localhost/")
        request.url = "http://foo.com?a=1&b=2"
        assertEquals("http://foo.com", request.url)
        assertEquals("1", request.getQueryParameter("a"))
        assertEquals("2", request.getQueryParameter("b"))
        assertEquals(setOf("a", "b"), request.queryParameterNames)
    }

    @Test
    fun queryParameters() {
        val request = Request("1.1", "GET", "http://localhost/")
        request.addQueryParameter("foo", "bar")
        request.addQueryParameter("another", "param")

        assertEquals("bar", request.getQueryParameter("foo"))
        assertEquals("param", request.getQueryParameter("another"))
        assertEquals(setOf("foo", "another"), request.queryParameterNames)

        request.removeQueryParameter("foo")
        assertNull(request.getQueryParameter("foo"))
        assertEquals(setOf("another"), request.queryParameterNames)
    }

    @Test
    fun headers() {
        val request = Request("1.1", "GET", "http://localhost/")
        request.addHeader("X-Test", "value")
        request.addHeader("Another-Header", "another-value")

        assertEquals("value", request.getHeader("X-Test"))
        assertEquals("another-value", request.getHeader("Another-Header"))
        assertEquals(setOf("X-Test", "Another-Header"), request.headerNames)

        request.removeHeader("X-Test")
        assertEquals("", request.getHeader("X-Test"))
        assertEquals(setOf("Another-Header"), request.headerNames)
    }

    @Test
    fun setBodyLength() {
        val request = Request("1.1", "GET", "http://localhost/")
        request.bodyLength = 1234
        assertEquals(1234, request.bodyLength)

        // test negative value is ignored
        request.bodyLength = -5
        assertEquals(1234, request.bodyLength)
    }
}
