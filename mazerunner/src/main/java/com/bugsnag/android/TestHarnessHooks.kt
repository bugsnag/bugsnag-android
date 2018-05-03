package com.bugsnag.android

import android.content.Context
import android.net.ConnectivityManager

/**
 * Accesses the session tracker and flushes all stored sessions
 */
internal fun flushAllSessions() {
    Bugsnag.getClient().sessionTracker.flushStoredSessions()
}

internal fun flushErrorStoreAsync(client: Client, apiClient: ErrorReportApiClient) {
    client.setErrorReportApiClient(apiClient)
    client.errorStore.flushAsync()
}

internal fun flushErrorStoreOnLaunch(client: Client, apiClient: ErrorReportApiClient) {
    client.setErrorReportApiClient(apiClient)
    client.errorStore.flushOnLaunch()
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

internal fun createDefaultErrorClient(context: Context): ErrorReportApiClient {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    return DefaultHttpClient(cm)
}

internal fun createDefaultSessionClient(context: Context): SessionTrackingApiClient {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    return DefaultHttpClient(cm)
}

internal fun writeErrorToStore(client: Client) {
    val error = Error.Builder(Configuration("api-key"), RuntimeException(), null).build()
    client.errorStore.write(error)
}
