package com.bugsnag.android.mazerunner.scenarios

import android.os.StrictMode
import java.net.HttpURLConnection
import java.net.URL

/**
 * Generates a strictmode exception caused by performing a network request on the main thread
 */
internal class StrictModeNetworkScenario : Scenario() {

    override fun run() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
            .detectNetwork()
            .penaltyDeath()
            .build())

        val urlConnection = URL("http://example.com").openConnection() as HttpURLConnection
        urlConnection.doOutput = true
        urlConnection.responseMessage
    }

}
