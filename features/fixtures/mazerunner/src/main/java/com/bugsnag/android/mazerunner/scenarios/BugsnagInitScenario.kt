package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import java.lang.RuntimeException
import java.util.concurrent.Callable
import java.util.concurrent.Executors

internal class BugsnagInitScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    init {
        config.setAutoCaptureSessions(false)
    }

    override fun run() {
        val threadPool = Executors.newFixedThreadPool(8)
        val callables = mutableListOf<Callable<Client?>>()

        IntRange(1, 25).forEach {
            callables.add(Callable { Bugsnag.init(context) })
            callables.add(Callable { Bugsnag.init(context, config.apiKey) })
            callables.add(Callable { Bugsnag.init(context, config.apiKey, false) })
            callables.add(Callable { Bugsnag.init(context, Configuration(config.apiKey)) })
        }

        val futures = threadPool.invokeAll(callables)
        val uniqueClients = futures.map { it.get() }.distinct()

        val bugsnag = uniqueClients.first()!!
        bugsnag.addToTab("client", "count", uniqueClients.size)
        bugsnag.notify(RuntimeException())
    }

}
