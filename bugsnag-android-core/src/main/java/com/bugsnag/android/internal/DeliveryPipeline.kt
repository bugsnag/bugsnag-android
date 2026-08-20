package com.bugsnag.android.internal

import com.bugsnag.android.CallbackState
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Logger
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.remoteconfig.RemoteConfigState

internal class DeliveryPipeline(
    val onSendCallbackState: CallbackState,
    val remoteConfigState: RemoteConfigState,
    val config: ImmutableConfig,
) {
    private val logger: Logger get() = config.logger

    fun deliverEventPayload(payload: EventPayload): DeliveryStatus? {
        if (!retainPayload(payload)) {
            return null
        }

        getDiscardStatus(payload)?.let {
            return it
        }

        val deliveryParams = config.getErrorApiDeliveryParams(payload)
        val delivery = config.delivery
        return delivery.deliver(payload, deliveryParams)
    }

    private fun retainPayload(payload: EventPayload): Boolean {
        return try {
            onSendCallbackState.runOnSendTasks(
                { payload.event!! },
                config.logger
            )
        } catch (_: Exception) {
            // most likely the payload could not be decoded, so we continue
            true
        }
    }

    private fun getDiscardStatus(payload: EventPayload): DeliveryStatus? {
        return try {
            val remoteConfig = getRemoteConfig() ?: return null
            if (isTimeSensitive(payload)) {
                return null
            }

            val applicableDiscardRule = remoteConfig.discardRules.firstOrNull {
                it.shouldDiscard(payload)
            } ?: return null

            logger.d("Discarding event due to remote discardRule: $applicableDiscardRule")
            // discarded events are treated as being delivered, as the server would have discarded them
            DeliveryStatus.DELIVERED
        } catch (_: Exception) {
            // swallow any RemoteConfig related errors, and favour delivering the payload
            null
        }
    }

    private fun isTimeSensitive(payload: EventPayload): Boolean {
        val event = payload.event
        return payload.isLaunchCrash || payload.isUnhandled || event?.errors?.any {
            it.errorClass == "ANR" || it.errorClass == "AppHang"
        } ?: false
    }

    private fun getRemoteConfig(): RemoteConfig? {
        // Delivery should not block on remote-config downloads, as these reports can be
        // time-sensitive (for example, app hangs and ANRs). Use the latest cached snapshot and
        // let the background scheduler refresh it independently.
        return remoteConfigState.peekRemoteConfig()
    }
}
