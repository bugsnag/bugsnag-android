package com.bugsnag.android

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
class BugsnagExitInfoPlugin(
    private val context: Context
) : Plugin, OnSendCallback {

    private var exitInfoCallback: ExitInfoCallback? = null

    override fun onSend(event: Event): Boolean {
        return exitInfoCallback?.onSend(event) ?: true
    }

    override fun load(client: Client) {
        client.addOnSession(
            OnSessionCallback { session: Session ->
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                am.setProcessStateSummary(session.id.toByteArray())
                return@OnSessionCallback true
            }
        )
//        exitInfoCallback = ExitInfoCallback(
        //        client.context,
        //        TombstoneEventEnhancer(),
        //        TraceEventEnhancer(client.logger, client.immutableConfig.projectPackages))
    }

    override fun unload() {
        TODO("Not yet implemented")
    }
}
