package com.bugsnag.android.internal

import com.bugsnag.android.CallbackState
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Logger
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.remoteconfig.RemoteConfigState
import java.util.concurrent.TimeUnit

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

        val remoteConfig = getRemoteConfig(payload.isLaunchCrash)
        if (shouldDiscardPayload(remoteConfig, payload)) {
            // discarded events are treated as delivered, the server would have discarded them
            return DeliveryStatus.DELIVERED
        }

        val payloadEncoding = remoteConfig?.deliveryConfig?.payloadEncoding
            ?: DeliveryParams.PayloadEncoding.NONE

        val deliveryParams = config.getErrorApiDeliveryParams(payload, payloadEncoding)
        val delivery = config.delivery
        return delivery.deliver(payload, deliveryParams)
    }

    private fun shouldDiscardPayload(
        remoteConfig: RemoteConfig?,
        payload: EventPayload
    ): Boolean {
        try {
            if (remoteConfig != null) {
                val discardRules = remoteConfig.discardRules
                val applicableDiscardRule = discardRules.firstOrNull { it.shouldDiscard(payload) }
                if (applicableDiscardRule != null) {
                    logger.d("Discarding event due to remote discardRule: $applicableDiscardRule")
                    return true
                }
            }
        } catch (_: Exception) {
            // swallow any RemoteConfig related errors, and favour delivering the payload
        }
        return false
    }

    private fun getRemoteConfig(isLaunchCrash: Boolean): RemoteConfig? {
        try {
            val timeout = if (isLaunchCrash) LAUNCH_CRASH_LOAD_TIMEOUT_MS else Long.MAX_VALUE
            return remoteConfigState.getRemoteConfig(timeout, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {
            return null
        }
    }

    internal companion object {
        const val LAUNCH_CRASH_LOAD_TIMEOUT_MS = 100L
    }
}
