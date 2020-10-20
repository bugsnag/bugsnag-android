package com.bugsnag.android

import com.bugsnag.android.StateEvent.AddMetadata
import com.bugsnag.android.StateEvent.ClearMetadataSection
import com.bugsnag.android.StateEvent.ClearMetadataValue
import com.bugsnag.android.StateEvent.UpdateContext
import com.bugsnag.android.StateEvent.UpdateUser
import java.util.Observable
import java.util.Observer

/**
 * Listens for changes in the user, context, and metadata, then informs the JS layer
 * of any state alterations.
 */
internal class BugsnagReactNativeBridge(
    private val client: Client,
    private val cb: (event: MessageEvent) -> Unit
) : Observer {

    override fun update(observable: Observable, arg: Any?) {
        if (arg is StateEvent) {
            val event: MessageEvent? = when (arg) {
                is UpdateContext -> {
                    MessageEvent("ContextUpdate", arg.context)
                }
                is AddMetadata, is ClearMetadataSection, is ClearMetadataValue -> {
                    MessageEvent("MetadataUpdate", client.metadata)
                }
                is UpdateUser -> {
                    MessageEvent(
                        "UserUpdate",
                        mapOf(
                            Pair("id", arg.user.id),
                            Pair("email", arg.user.email),
                            Pair("name", arg.user.name)
                        )
                    )
                }
                else -> null
            }

            if (event != null) {
                cb(event)
            }
        }
    }
}
