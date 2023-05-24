package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import java.util.concurrent.Callable
import java.util.concurrent.Executors

internal class BugsnagInitScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    companion object {
        private const val POOL_SIZE = 8
    }

    override fun startScenario() {
        val threadPool = Executors.newFixedThreadPool(POOL_SIZE)
        val callables = mutableListOf<Callable<Client?>>()

        IntRange(1, 25).forEach {
            callables.add(
                Callable {
                    reportBugsnagStartupDuration { Bugsnag.start(context) }
                }
            )

            callables.add(
                Callable {
                    reportBugsnagStartupDuration { Bugsnag.start(context, config.apiKey) }
                }
            )

            callables.add(
                Callable {
                    reportBugsnagStartupDuration {
                        Bugsnag.start(context, Configuration(config.apiKey))
                    }
                }
            )
        }

        val futures = threadPool.invokeAll(callables)
        val uniqueClients = futures.map { it.get() }.distinct()

        val bugsnag = uniqueClients.first()!!
        bugsnag.addMetadata("client", "count", uniqueClients.size)
        bugsnag.notify(RuntimeException())
    }
}
