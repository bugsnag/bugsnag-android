package com.bugsnag.android.okhttp

import com.bugsnag.android.Client
import com.bugsnag.android.Plugin
import okhttp3.EventListener

class BugsnagOkHttpPlugin : Plugin, EventListener() {

    override fun load(client: Client) {
    }

    override fun unload() {
    }
}
