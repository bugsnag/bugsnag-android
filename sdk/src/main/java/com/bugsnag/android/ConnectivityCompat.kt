package com.bugsnag.android

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.support.annotation.RequiresApi

@Suppress("DEPRECATION")
class ConnectivityCompat(
    private val context: Context,
    internal val networkChange: ((connected: Boolean) -> Unit)? = null
) {

    private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

    fun registerForNetworkChanges() {
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(ConnectivityChangeReceiver(), intentFilter)
    }

    fun hasNetworkConnection(): Boolean {
        return when (cm) {
            null -> true // attempt delivery anyway
            else -> cm.activeNetworkInfo.isConnectedOrConnecting
        }
    }

    fun retrieveNetworkAccessState(): String {
        try {
            val activeNetwork = cm?.activeNetworkInfo
            return if (activeNetwork != null && activeNetwork.isConnectedOrConnecting) {
                when {
                    activeNetwork.type == 1 -> "wifi"
                    activeNetwork.type == 9 -> "ethernet"
                    else -> // We default to cellular as the other enums are all cellular in some
                        // form or another
                        "cellular"
                }
            } else {
                "none"
            }
        } catch (exception: Exception) {
            Logger.warn(
                "Could not get network access information, we "
                    + "recommend granting the 'android.permission.ACCESS_NETWORK_STATE' permission"
            )
            return "unknown"
        }
    }

    internal inner class ConnectivityChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            networkChange?.invoke(hasNetworkConnection())
        }
    }
}
