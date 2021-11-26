package com.bugsnag.android.ndk

import androidx.test.platform.app.InstrumentationRegistry
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Session
import org.junit.Test

internal class StartBugsnagTest {
    @Test(timeout = 5_000)
    fun start() {
        Bugsnag.start(
            InstrumentationRegistry.getInstrumentation().targetContext,
            Configuration("5d1ec5bd39a74caa1267142706a7fb21").apply {
                autoTrackSessions = false
                delivery = object : Delivery {
                    override fun deliver(
                        payload: Session,
                        deliveryParams: DeliveryParams
                    ): DeliveryStatus = DeliveryStatus.DELIVERED

                    override fun deliver(
                        payload: EventPayload,
                        deliveryParams: DeliveryParams
                    ): DeliveryStatus = DeliveryStatus.DELIVERED
                }
            }
        )
    }
}
