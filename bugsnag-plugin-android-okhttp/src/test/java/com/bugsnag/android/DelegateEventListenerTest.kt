package com.bugsnag.android

import com.bugsnag.android.okhttp.BugsnagOkHttpPlugin
import com.bugsnag.android.okhttp.util.DelegateEventListener
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.io.IOException

@RunWith(MockitoJUnitRunner::class)
class DelegateEventListenerTest {

    @Mock
    lateinit var client: Client

    @Mock
    lateinit var call: Call

    @Mock
    private lateinit var delegateEventListener: DelegateEventListener

    private lateinit var plugin: BugsnagOkHttpPlugin

    private val response = mock(okhttp3.Response::class.java)
    private val inetSocketAddress = mock(java.net.InetSocketAddress::class.java)
    private val proxy = mock(java.net.Proxy::class.java)
    private val protocol = okhttp3.Protocol.HTTP_1_1
    private val ioe = IOException("Exception for testing")
    private val connection = mock(okhttp3.Connection::class.java)
    private val request = mock(okhttp3.Request::class.java)

    @Before
    fun setUp() {
        plugin = BugsnagOkHttpPlugin(delegateEventListener)
        plugin.load(client)
    }

    @Test
    fun callStart() {
        plugin.callStart(call)
        verify(delegateEventListener, times(1)).callStart(call)
    }

    @Test
    fun callEnd() {
        plugin.callEnd(call)
        verify(delegateEventListener, times(1)).callEnd(call)
    }

    @Test
    fun callFailed() {
        plugin.callFailed(call, ioe)
        verify(delegateEventListener, times(1)).callFailed(call, ioe)
    }

    @Test
    fun canceled() {
        plugin.canceled(call)
        verify(delegateEventListener, times(1)).canceled(call)
    }

    @Test
    fun cacheConditionalHit() {
        plugin.cacheConditionalHit(call, response)
        verify(delegateEventListener, times(1)).cacheConditionalHit(call, response)
    }

    @Test
    fun cacheHit() {
        plugin.cacheHit(call, response)
        verify(delegateEventListener, times(1)).cacheHit(call, response)
    }

    @Test
    fun cacheMiss() {
        plugin.cacheMiss(call)
        verify(delegateEventListener, times(1)).cacheMiss(call)
    }

    @Test
    fun connectEnd() {
        plugin.connectEnd(call, inetSocketAddress, proxy, protocol)
        verify(delegateEventListener, times(1)).connectEnd(
            call,
            inetSocketAddress,
            proxy,
            protocol
        )
    }

    @Test
    fun connectFailed() {
        plugin.connectFailed(call, inetSocketAddress, proxy, protocol, ioe)
        verify(delegateEventListener, times(1)).connectFailed(
            call,
            inetSocketAddress,
            proxy,
            protocol,
            ioe
        )
    }

    @Test
    fun connectStart() {
        plugin.connectStart(call, inetSocketAddress, proxy)
        verify(delegateEventListener, times(1)).connectStart(call, inetSocketAddress, proxy)
    }

    @Test
    fun connectionAcquired() {
        plugin.connectionAcquired(call, connection)
        verify(delegateEventListener, times(1)).connectionAcquired(call, connection)
    }

    @Test
    fun connectionReleased() {
        plugin.connectionReleased(call, connection)
        verify(delegateEventListener, times(1)).connectionReleased(call, connection)
    }

    @Test
    fun dnsEnd() {
        plugin.dnsEnd(call, "example.com", listOf())
        verify(delegateEventListener, times(1)).dnsEnd(call, "example.com", listOf())
    }

    @Test
    fun dnsStart() {
        plugin.dnsStart(call, "example.com")
        verify(delegateEventListener, times(1)).dnsStart(call, "example.com")
    }

    @Test
    fun proxySelectEnd() {
        plugin.proxySelectEnd(call, "https://example.com".toHttpUrl(), listOf(proxy))
        verify(delegateEventListener, times(1)).proxySelectEnd(
            call,
            "https://example.com".toHttpUrl(),
            listOf(proxy)
        )
    }

    @Test
    fun proxySelectStart() {
        plugin.proxySelectStart(call, "https://example.com".toHttpUrl())
        verify(delegateEventListener, times(1)).proxySelectStart(
            call,
            "https://example.com".toHttpUrl()
        )
    }

    @Test
    fun requestBodyStart() {
        plugin.requestBodyStart(call)
        verify(delegateEventListener, times(1)).requestBodyStart(call)
    }

    @Test
    fun requestFailed() {
        plugin.requestFailed(call, ioe)
        verify(delegateEventListener, times(1)).requestFailed(call, ioe)
    }

    @Test
    fun requestHeadersEnd() {
        plugin.requestHeadersEnd(call, request)
        verify(delegateEventListener, times(1)).requestHeadersEnd(call, request)
    }

    @Test
    fun requestHeadersStart() {
        plugin.requestHeadersStart(call)
        verify(delegateEventListener, times(1)).requestHeadersStart(call)
    }

    @Test
    fun responseBodyStart() {
        plugin.responseBodyStart(call)
        verify(delegateEventListener, times(1)).responseBodyStart(call)
    }

    @Test
    fun responseFailed() {
        plugin.responseFailed(call, ioe)
        verify(delegateEventListener, times(1)).responseFailed(call, ioe)
    }

    @Test
    fun responseHeadersStart() {
        plugin.responseHeadersStart(call)
        verify(delegateEventListener, times(1)).responseHeadersStart(call)
    }

    @Test
    fun satisfactionFailure() {
        plugin.satisfactionFailure(call, response)
        verify(delegateEventListener, times(1)).satisfactionFailure(call, response)
    }

    @Test
    fun secureConnectEnd() {
        plugin.secureConnectEnd(call, null)
        verify(delegateEventListener, times(1)).secureConnectEnd(call, null)
    }

    @Test
    fun secureConnectStart() {
        plugin.secureConnectStart(call)
        verify(delegateEventListener, times(1)).secureConnectStart(call)
    }
}
