package com.bugsnag.android.internal

import com.bugsnag.android.CallbackState
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.internal.remoteconfig.RemoteConfigState

internal class DeliveryPipeline(
    val onSendCallbackState: CallbackState,
    val remoteConfigState: RemoteConfigState,
    val config: ImmutableConfig,
) {
    fun deliverEventPayload(payload: EventPayload): DeliveryStatus? {
        // Remote config is only fetched in response to an error. This never blocks: it is skipped
        // when the cached config is fresh, a cooldown is active or a fetch is already in flight.
        scheduleRemoteConfigRefresh()

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
            // Delivery never waits for a network fetch: apply whatever valid config is cached and
            // let the background refresh (if any) benefit later events.
            val remoteConfig = remoteConfigState.peekRemoteConfig()
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

    private fun scheduleRemoteConfigRefresh() {
        try {
            remoteConfigState.scheduleDownloadIfRequired()
        } catch (_: Exception) {
            // remote config must never prevent delivery
        }
    }
}
