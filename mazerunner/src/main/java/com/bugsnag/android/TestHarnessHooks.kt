package com.bugsnag.android

import android.content.Context
import android.net.ConnectivityManager
import java.io.File
import java.io.FileWriter

/**
 * Accesses the session tracker and flushes all stored sessions
 */
internal fun flushAllSessions(client: Client) {
    Async.run(client.sessionTracker::flushStoredSessions)
}

internal fun flushErrorStoreAsync(client: Client, apiClient: ErrorReportApiClient) {
    client.errorStore.flushAsync(apiClient)
}

internal fun flushErrorStoreOnLaunch(client: Client, apiClient: ErrorReportApiClient) {
    client.errorStore.flushOnLaunch(apiClient)
}

/**
 * Creates an error API client with a 500ms delay, emulating poor network connectivity
 */
internal fun createSlowErrorApiClient(context: Context): ErrorReportApiClient {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    val defaultHttpClient = DefaultHttpClient(cm)

    return ErrorReportApiClient({ url: String?,
                                  report: Report?,
                                  headers: MutableMap<String, String>? ->
        Thread.sleep(500)
        defaultHttpClient.postReport(url, report, headers)
    })
}

/**
 * Writes an error to the old directory format (i.e. bugsnag-errors)
 */
internal fun writeErrorToOldDir(client: Client) {
    val configuration = Configuration("api-key")
    val error = Error.Builder(configuration, RuntimeException(), null).build()
    val filename = client.errorStore.getFilename(error)

    val file = File(client.errorStore.oldDirectory, filename)
    val writer = JsonStream(FileWriter(file))
    error.toStream(writer)
}

internal fun writeErrorToStore(client: Client) {
    val error = Error.Builder(Configuration("api-key"), RuntimeException(), null).build()
    client.errorStore.write(error)
}

/**
 * Sets a NOP implementation for the Session Tracking API, preventing delivery
 */
internal fun disableSessionDelivery(client: Client) {
    client.setSessionTrackingApiClient({ _, _, _ ->
        throw NetworkException("Session Delivery NOP", RuntimeException("NOP"))
    })
}

/**
 * Sets a NOP implementation for the Error Tracking API, preventing delivery
 */
internal fun disableReportDelivery(client: Client) {
    client.setErrorReportApiClient({ _, _, _ ->
        throw NetworkException("Error Delivery NOP", RuntimeException("NOP"))
    })
}

/**
 * Sets a NOP implementation for the Error Tracking API and the Session Tracking API,
 * preventing delivery
 */
internal fun disableAllDelivery(client: Client) {
    disableSessionDelivery(client)
    disableReportDelivery(client)
}

