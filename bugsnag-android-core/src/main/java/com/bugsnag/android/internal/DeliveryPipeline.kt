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
        try {
            val retain = onSendCallbackState.runOnSendTasks(
                { payload.event!! },
                config.logger
            )

            if (!retain) {
                return null
            }
        } catch (_: Exception) {
            // most likely the payload could not be decoded, so we continue
        }

        try {
            val remoteConfig = getRemoteConfig()
            if (remoteConfig != null) {
                val event = payload.event
                val isTimeSensitive = payload.isLaunchCrash || event?.errors?.any {
                    it.errorClass == "ANR" || it.errorClass == "AppHang"
                } ?: false

                if (!isTimeSensitive) {
                    val discardRules = remoteConfig.discardRules
                    val applicableDiscardRule = discardRules.firstOrNull { it.shouldDiscard(payload) }
                    if (applicableDiscardRule != null) {
                        logger.d("Discarding event due to remote discardRule: $applicableDiscardRule")
                        // discarded events are treated as being delivered, as the server would have discarded them
                        return DeliveryStatus.DELIVERED
                    }
                }
            }
        } catch (_: Exception) {
            // swallow any RemoteConfig related errors, and favour delivering the payload
        }

        val deliveryParams = config.getErrorApiDeliveryParams(payload)
        val delivery = config.delivery
        return delivery.deliver(payload, deliveryParams)
    }

    private fun getRemoteConfig(): RemoteConfig? {
        // Delivery should not block on remote-config downloads, as these reports can be
        // time-sensitive (for example, app hangs and ANRs). Use the latest cached snapshot and
        // let the background scheduler refresh it independently.
        return remoteConfigState.peekRemoteConfig()
    }
}
