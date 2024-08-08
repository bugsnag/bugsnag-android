package com.bugsnag.android.mazerunner

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import java.net.URL

enum class NetworkStatus {
    NO_NETWORK,
    UNKNOWN_CAPABILITIES,
    NO_INTERNET,
    CONNECTED,
    DISCONNECTED
}

val Context.networkStatus: NetworkStatus
    get() {
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkStatus.NO_NETWORK
            val capabilities = connectivityManager.getNetworkCapabilities(network)
                ?: return NetworkStatus.UNKNOWN_CAPABILITIES

            return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                NetworkStatus.CONNECTED
            } else {
                NetworkStatus.NO_INTERNET
            }
        } else {
            val networkInfo =
                connectivityManager.activeNetworkInfo ?: return NetworkStatus.NO_NETWORK

            if (networkInfo.isAvailable && networkInfo.isConnected) {
                @Suppress("SwallowedException")
                return try {
                    URL("https://www.google.com").readText()
                    NetworkStatus.CONNECTED
                } catch (e: Exception) {
                    NetworkStatus.NO_INTERNET
                }
            }

            return NetworkStatus.DISCONNECTED
        }
    }
