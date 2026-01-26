package com.bugsnag.android.okhttp

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.http.HttpInstrumentationBuilder
import com.bugsnag.android.http.HttpRequestCallback
import com.bugsnag.android.http.HttpResponseCallback
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.BitSet
import kotlin.math.max

/**
 * This class builds [Interceptor] integrations for OkHttp that can be configured to capture requests
 * as breadcrumbs and/or errors.
 *
 * To enable this functionality use [createInterceptor] to create an interceptor and use
 * [addInterceptor](okhttp3.OkHttpClient.Builder.addInterceptor) to configure it with your
 * `OkHttpClient`.
 */
@Suppress("SENSELESS_COMPARISON")
class BugsnagOkHttp : HttpInstrumentationBuilder<Request, Response> {
    private val errorCodes = BitSet()
    private var maxRequestBodyCapture = DEFAULT_BODY_CAPTURE_SIZE
    private var maxResponseBodyCapture = DEFAULT_BODY_CAPTURE_SIZE
    private var logRequestBreadcrumbs = false
    private val requestCallbacks = ArrayList<HttpRequestCallback<Request>>()
    private val responseCallbacks = ArrayList<HttpResponseCallback<Request, Response>>()

    override fun addHttpErrorCode(statusCode: Int): BugsnagOkHttp {
        errorCodes.set(statusCode)
        return this
    }

    override fun addHttpErrorCodes(
        minStatusCode: Int,
        maxStatusCode: Int
    ): BugsnagOkHttp {
        val start = max(0, minStatusCode)
        val end = max(start, maxStatusCode + 1)

        if (start < end) {
            errorCodes.set(start, end)
        }

        return this
    }

    override fun removeHttpErrorCode(statusCode: Int): BugsnagOkHttp {
        errorCodes.clear(statusCode)
        return this
    }

    override fun removeHttpErrorCodes(
        minStatusCode: Int,
        maxStatusCode: Int
    ): BugsnagOkHttp {
        val start = max(0, minStatusCode)
        val end = max(start, maxStatusCode + 1)

        if (start < end) {
            errorCodes.clear(start, end)
        }

        return this
    }

    override fun maxRequestBodyCapture(maxBytes: Long): BugsnagOkHttp {
        maxRequestBodyCapture = max(maxBytes, 0L)
        return this
    }

    override fun maxResponseBodyCapture(maxBytes: Long): BugsnagOkHttp {
        maxResponseBodyCapture = max(maxBytes, 0L)
        return this
    }

    override fun logBreadcrumbs(): BugsnagOkHttp {
        return logBreadcrumbs(true)
    }

    override fun logBreadcrumbs(logBreadcrumbs: Boolean): BugsnagOkHttp {
        logRequestBreadcrumbs = logBreadcrumbs
        return this
    }

    override fun addRequestCallback(callback: HttpRequestCallback<Request>): BugsnagOkHttp {
        if (callback != null) {
            requestCallbacks.add(callback)
        }

        return this
    }

    override fun addResponseCallback(callback: HttpResponseCallback<Request, Response>): BugsnagOkHttp {
        if (callback != null) {
            responseCallbacks.add(callback)
        }

        return this
    }

    /**
     * Create an OkHttp `Interceptor` based on the current config of this `BugsnagOkHttp` instance.
     * The new `Interceptor` should be added using [okhttp3.OkHttpClient.Builder.addInterceptor]
     * to instrument the configured requests.
     *
     * The `Interceptor` returned here will sent all breadcrumbs and errors to the global
     * [Bugsnag.getClient].
     */
    fun createInterceptor(): Interceptor {
        // shortcut if Bugsnag is already started, we can skip the check on every call
        if (Bugsnag.isStarted()) {
            return createInterceptor(Bugsnag.getClient())
        }

        return createInterceptor {
            if (Bugsnag.isStarted()) {
                Bugsnag.getClient()
            } else {
                null
            }
        }
    }

    /**
     * Create an OkHttp `Interceptor` based on the current config of this `BugsnagOkHttp` instance,
     * and send all its breadcrumbs and errors to the given [client].  The new `Interceptor` should
     * be added using [okhttp3.OkHttpClient.Builder.addInterceptor] to instrument the configured
     * requests.
     */
    fun createInterceptor(client: Client): Interceptor {
        return createInterceptor { client }
    }

    private fun createInterceptor(clientSupplier: () -> Client?): Interceptor {
        return BugsnagOkHttpInterceptor(
            errorCodes,
            maxRequestBodyCapture,
            maxResponseBodyCapture,
            logRequestBreadcrumbs,
            requestCallbacks.toList(),
            responseCallbacks.toList(),
            clientSupplier
        )
    }

    internal companion object {
        private const val DEFAULT_BODY_CAPTURE_SIZE: Long = 0L
    }
}
