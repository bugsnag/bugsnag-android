package com.bugsnag.android

import android.content.Context
import android.net.ConnectivityManager
import com.bugsnag.android.Bugsnag.client

/**
 * Accesses the session tracker and flushes all stored sessions
 */
internal fun flushAllSessions() {
    Bugsnag.getClient().sessionTracker.flushStoredSessions()
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

internal fun writeErrorToStore(client: Client) {
    val error = Error.Builder(Configuration("api-key"), RuntimeException(), null).build()
    client.errorStore.write(error)
}

