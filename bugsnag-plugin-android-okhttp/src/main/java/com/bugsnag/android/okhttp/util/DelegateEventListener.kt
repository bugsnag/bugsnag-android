package com.bugsnag.android.okhttp.util

import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.HttpUrl
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

open class DelegateEventListener(
    protected val delegateEventListener: EventListener?
) : EventListener() {
    override fun callStart(call: Call) {
        delegateEventListener?.callStart(call)
    }

    override fun callEnd(call: Call) {
        delegateEventListener?.callEnd(call)
    }

    override fun callFailed(call: Call, ioe: IOException) {
        delegateEventListener?.callFailed(call, ioe)
    }

    override fun canceled(call: Call) {
        delegateEventListener?.canceled(call)
    }

    override fun cacheConditionalHit(call: Call, cachedResponse: Response) {
        delegateEventListener?.cacheConditionalHit(call, cachedResponse)
    }

    override fun cacheHit(call: Call, response: Response) {
        delegateEventListener?.cacheHit(call, response)
    }

    override fun cacheMiss(call: Call) {
        delegateEventListener?.cacheMiss(call)
    }

    override fun connectEnd(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?
    ) {
        delegateEventListener?.connectEnd(call, inetSocketAddress, proxy, protocol)
    }

    override fun connectFailed(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy,
        protocol: Protocol?,
        ioe: IOException
    ) {
        delegateEventListener?.connectFailed(call, inetSocketAddress, proxy, protocol, ioe)
    }

    override fun connectStart(
        call: Call,
        inetSocketAddress: InetSocketAddress,
        proxy: Proxy
    ) {
        delegateEventListener?.connectStart(call, inetSocketAddress, proxy)
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        delegateEventListener?.connectionAcquired(call, connection)
    }

    override fun connectionReleased(call: Call, connection: Connection) {
        delegateEventListener?.connectionReleased(call, connection)
    }

    override fun dnsEnd(
        call: Call,
        domainName: String,
        inetAddressList: List<@JvmSuppressWildcards InetAddress>
    ) {
        delegateEventListener?.dnsEnd(call, domainName, inetAddressList)
    }

    override fun dnsStart(call: Call, domainName: String) {
        delegateEventListener?.dnsStart(call, domainName)
    }

    override fun proxySelectEnd(
        call: Call,
        url: HttpUrl,
        proxies: List<@JvmSuppressWildcards Proxy>
    ) {
        delegateEventListener?.proxySelectEnd(call, url, proxies)
    }

    override fun proxySelectStart(call: Call, url: HttpUrl) {
        delegateEventListener?.proxySelectStart(call, url)
    }

    override fun requestBodyStart(call: Call) {
        delegateEventListener?.requestBodyStart(call)
    }

    override fun requestFailed(call: Call, ioe: IOException) {
        delegateEventListener?.requestFailed(call, ioe)
    }

    override fun requestHeadersEnd(call: Call, request: Request) {
        delegateEventListener?.requestHeadersEnd(call, request)
    }

    override fun requestHeadersStart(call: Call) {
        delegateEventListener?.requestHeadersStart(call)
    }

    override fun responseBodyStart(call: Call) {
        delegateEventListener?.responseBodyStart(call)
    }

    override fun responseFailed(call: Call, ioe: IOException) {
        delegateEventListener?.responseFailed(call, ioe)
    }

    override fun responseHeadersStart(call: Call) {
        delegateEventListener?.responseHeadersStart(call)
    }

    override fun satisfactionFailure(call: Call, response: Response) {
        delegateEventListener?.satisfactionFailure(call, response)
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        delegateEventListener?.secureConnectEnd(call, handshake)
    }

    override fun secureConnectStart(call: Call) {
        delegateEventListener?.secureConnectStart(call)
    }
}
