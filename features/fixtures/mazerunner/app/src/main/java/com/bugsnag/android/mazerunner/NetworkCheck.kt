package com.bugsnag.android.mazerunner

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

// 10 second timeout
private const val PING_TIMEOUT = 10_000

private const val HTTP_CONNECT_TIMEOUT = 15_000
private const val HTTP_READ_TIMEOUT = 15_000
private const val NETWORK_CHECK_TIMEOUT = 30_000L

// number of PING requests to do for each server
private const val PING_COUNT = 5

/**
 * Produces a network [report] the can be used to help with debugging CI by testing the connection
 * to the Mazerunner server against connections to well-known external servers.
 */
class NetworkCheck : Thread("NetworkCheck") {

    private val pingServers = listOf(
        "google.com",
        "amazon.com",
        "ibm.com"
    )

    // we actually gather the report data as strings to be logged, keeping the code simple
    private val serverPingTimes = ConcurrentHashMap<String, String>()
    private val httpLogs = ConcurrentLinkedQueue<Collection<String>>()

    override fun run() {
        runCatching {
            val pingLatch = reportPingTimes()
            reportProxySpeed("https://www.google.com/?q=${UUID.randomUUID()}")
            reportProxySpeed("http://bs-local.com:9339")

            pingLatch.await()
        }
    }

    fun report() {
        log("- NETWORK REPORT ---------------------------------------------------------------")
        runCatching { join(NETWORK_CHECK_TIMEOUT) }

        if (isAlive) {
            log("- NETWORK REPORT STILL IN PROGRESS")
            log("- Dumping available data")
        }

        pingServers.forEach { server ->
            log(serverPingTimes[server] ?: "no ping data for $server")
        }

        httpLogs.flatten().forEach { log(it) }
    }

    private fun reportProxySpeed(urlString: String) {
        val logs = ConcurrentLinkedQueue<String>()
        httpLogs.add(logs)

        try {
            val url = URL(urlString)
            logs.add("GET $url")
            val connection = url.openConnection() as HttpURLConnection

            val responseCodeTime = measureTimeMillis {
                connection.connectTimeout = HTTP_CONNECT_TIMEOUT
                connection.readTimeout = HTTP_READ_TIMEOUT
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    logs.add("HTTP Failure $url: ${connection.responseCode} ${connection.responseMessage}")
                    return
                }

                // read 1 byte of the response to ensure we're not just talking to the proxy
                connection.inputStream.read()
            }

            logs.add("HTTP Request to $url took ${responseCodeTime}ms")
        } catch (e: Exception) {
            logs.add("Connection to Maze Runner FAILED \n${e.stackTrace.joinToString("\n")}")
        }
    }

    private fun reportPingTimes(): CountDownLatch {
        val countDownLatch = CountDownLatch(pingServers.size)
        pingServers
            .map { server ->
                val address = InetAddress.getByName(server)
                // each server gets its own PING thread to avoid doing all the work sequentially
                thread(name = "ping<$server>") {
                    val pingTimes = (1..PING_COUNT).map {
                        val reachable: Boolean
                        val pingTime = measureTimeMillis {
                            reachable = address.isReachable(PING_TIMEOUT)
                        }

                        when {
                            reachable -> "${pingTime}ms"
                            else -> "timeout"
                        }
                    }

                    serverPingTimes[server] =
                        "Ping time to $server ($address): ${pingTimes.joinToString(" ")}"

                    countDownLatch.countDown()
                }
            }

        return countDownLatch
    }
}
