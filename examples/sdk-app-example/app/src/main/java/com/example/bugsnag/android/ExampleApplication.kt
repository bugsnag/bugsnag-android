package com.example.bugsnag.android

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Session
import com.bugsnag.android.okhttp.BugsnagOkHttpPlugin
import okhttp3.OkHttpClient
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class ExampleApplication : Application() {

    private class ExampleDelivery : Delivery {
        override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
            return deliverBytes(payload.toByteArray(), deliveryParams)
        }

        override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
            return deliverBytes(payload.toByteArray(), deliveryParams)
        }

        private fun deliverBytes(bytes: ByteArray, deliveryParams: DeliveryParams): DeliveryStatus {
            var connection: HttpURLConnection? = null
            return try {
                connection = URL(deliveryParams.endpoint).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000

                deliveryParams.headers.forEach { (key, value) ->
                    if (value != null) {
                        connection.addRequestProperty(key, value)
                    }
                }

                connection.outputStream.use { it.write(bytes) }

                val responseCode = connection.responseCode
                DeliveryStatus.forHttpResponseCode(responseCode)
            } catch (exc: IOException) {
                DeliveryStatus.UNDELIVERED
            } finally {
                connection?.disconnect()
            }
        }
    }

    private val bugsnagOkHttpPlugin = BugsnagOkHttpPlugin()
    val httpClient = OkHttpClient.Builder()
        .eventListener(bugsnagOkHttpPlugin)
        .build()

    companion object {
        init {
            System.loadLibrary("entrypoint")
        }
    }

    private external fun performNativeBugsnagSetup()

    override fun onCreate() {
        super.onCreate()

        val config = Configuration.load(this)
        config.setDelivery(ExampleDelivery())
        config.setUser("123456", "joebloggs@example.com", "Joe Bloggs")
        config.addMetadata("user", "age", 31)
        config.addPlugin(bugsnagOkHttpPlugin)

        // Configure the persistence directory when running MultiProcessActivity in a separate
        // process to ensure the two Bugsnag clients are independent
//        val processName = findCurrentProcessName()
//        if (processName.endsWith("secondaryprocess")) {
//            config.persistenceDirectory = File(filesDir, processName)
//        }

//
        config.endpoints = EndpointConfiguration(
            notify = "https://notify.example.com/",
            sessions = "https://sessions.example.com",
            configuration = "https://config.example.com/"
        )

        Bugsnag.start(this, config)

        // Initialise native callbacks
        performNativeBugsnagSetup()
    }


}