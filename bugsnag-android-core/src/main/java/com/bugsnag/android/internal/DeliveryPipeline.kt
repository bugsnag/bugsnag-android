package com.bugsnag.android.internal

import com.bugsnag.android.CallbackState
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.ErrorType
import com.bugsnag.android.EventPayload
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.remoteconfig.RemoteConfigState
import java.util.concurrent.TimeUnit

internal class DeliveryPipeline(
    val onSendCallbackState: CallbackState,
    val remoteConfigState: RemoteConfigState,
    val config: ImmutableConfig,
) {
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
            val event = payload.event
            if (event == null) {
                true
            } else {
                onSendCallbackState.runOnSendTasks({ event }, config.logger)
            }
        } catch (_: Exception) {
            // most likely the payload could not be decoded, so we continue
            true
        }
    }

    private fun getDiscardStatus(payload: EventPayload): DeliveryStatus? {
        return try {
            val remoteConfig = getRemoteConfig(payload)
            if (remoteConfig == null) {
                return null
            }

            val applicableDiscardRule = remoteConfig.discardRules.firstOrNull {
                it.shouldDiscard(payload)
            }

            if (applicableDiscardRule != null) {
                // discarded events are treated as being delivered, as the server would have discarded them
                DeliveryStatus.DELIVERED
            } else {
                null
            }
        } catch (_: Exception) {
            // swallow any RemoteConfig related errors, and favour delivering the payload
            null
        }
    }

    private fun isTimeSensitive(payload: EventPayload): Boolean {
        // Fast paths that avoid full JSON parsing where possible.
        // C errors and launch crashes are always time-sensitive.
        return payload.isLaunchCrash || payload.getErrorTypes().contains(ErrorType.C)
    }

    private fun getRemoteConfig(payload: EventPayload): RemoteConfig? {
        // Delivery should not block on remote-config downloads, as these reports can be
        // time-sensitive (for example, app hangs and ANRs). Use the latest cached snapshot and
        // let the background scheduler refresh it independently.
        if (isTimeSensitive(payload)) {
            return remoteConfigState.peekRemoteConfig()
        }

        // For non-time-sensitive events, we can wait briefly for a fresh config
        // if the cached one is expired or missing.
        return remoteConfigState.getRemoteConfig(5, TimeUnit.SECONDS)
    }
}
