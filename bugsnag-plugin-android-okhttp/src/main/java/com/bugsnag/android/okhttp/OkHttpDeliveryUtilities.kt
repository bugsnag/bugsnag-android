package com.bugsnag.android.okhttp

import android.annotation.SuppressLint
import com.bugsnag.android.Configuration
import okhttp3.OkHttpClient

@SuppressLint("NotConstructor")
@JvmSynthetic
fun Configuration.OkHttpDelivery(okHttpClient: OkHttpClient): OkHttpDelivery {
    return OkHttpDelivery(okHttpClient, apiKey, maxStringValueLength, logger!!)
}

fun Configuration.useOkHttpDelivery(okHttpClient: OkHttpClient) {
    delivery = OkHttpDelivery(okHttpClient)
}
