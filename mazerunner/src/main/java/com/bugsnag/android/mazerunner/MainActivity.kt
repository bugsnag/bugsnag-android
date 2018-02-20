package com.bugsnag.android.mazerunner

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class MainActivity : Activity() {

    private val factory = TestCaseFactory()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        initialiseBugsnag()
        enqueueTestCase()
    }

    /**
     * Enqueues the test case with a delay on the main thread. This avoids the Activity wrapping
     * unhandled Exceptions
     */
    private fun enqueueTestCase() {
        window.decorView.postDelayed({
            executeTestCase()
        }, 100)
    }

    private fun executeTestCase() {
        val eventType = intent.getStringExtra("EVENT_TYPE")
        Log.d("Bugsnag", "Received test case, executing " + eventType)
        val testCase = factory.testCaseForName(eventType)
        testCase.run()
    }

    private fun initialiseBugsnag() {
        val config = Configuration(intent.getStringExtra("BUGSNAG_API_KEY"))
        val port = intent.getStringExtra("BUGSNAG_PORT")
        config.endpoint = "${findHostname()}:$port"
        config.sessionEndpoint = "${findHostname()}:$port"

        Bugsnag.init(this, config)
        Bugsnag.setLoggingEnabled(true)
    }

    private fun findHostname(): String {
        val isEmulator = Build.FINGERPRINT.startsWith("unknown")
                || Build.FINGERPRINT.contains("generic")
        return when {
            isEmulator -> "http://10.0.2.2"
            else -> "http://localhost"
        }
    }

}
