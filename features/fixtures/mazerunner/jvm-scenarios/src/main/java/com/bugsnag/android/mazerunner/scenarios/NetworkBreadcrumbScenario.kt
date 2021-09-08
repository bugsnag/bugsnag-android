package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.StrictMode
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.ThreadSendPolicy
import com.bugsnag.android.okhttp.BugsnagOkHttpPlugin
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Make HTTP requests and confirm that Bugsnag captures them as breadcrumbs
 */
internal class NetworkBreadcrumbScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    private val plugin = BugsnagOkHttpPlugin()

    init {
        config.apply {
            sendThreads = ThreadSendPolicy.NEVER
            enabledBreadcrumbTypes = setOf(BreadcrumbType.REQUEST)
            addPlugin(plugin)
        }
    }

    override fun startScenario() {
        super.startScenario()

        val handlerThread = HandlerThread("mazerunner-okhttp").apply { start() }
        Handler(handlerThread.looper).post {
            makeNetworkRequests()
            Bugsnag.notify(generateException())
        }
    }

    private fun makeNetworkRequests() {
        // ignore StrictMode violations within OkHttp
        val policy = StrictMode.getVmPolicy()
        StrictMode.setVmPolicy(StrictMode.VmPolicy.LAX)

        val okHttpClient = OkHttpClient.Builder()
            .eventListener(plugin)
            .build()
        StrictMode.setVmPolicy(policy)

        // 1. make a GET request
        makeSimpleGetRequest(okHttpClient)
    }

    private fun makeSimpleGetRequest(okHttpClient: OkHttpClient) {
        val request = Request.Builder().url("https://google.com?test=true").build()
        val response = requireNotNull(okHttpClient.newCall(request).execute())
        response.close()
    }
}
