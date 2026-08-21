package com.bugsnag.android

import com.bugsnag.android.internal.JsonHelper
import com.bugsnag.android.internal.convertToImmutableConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class EventInternalTelemetryTest {

    @Test
    fun usageObjectIsOmittedWhenTelemetryIsDisabled() {
        val config = Configuration("12312312312312312312312312312312")
        config.setTelemetry(emptySet())
        val immutableConfig = convertToImmutableConfig(config)
        val event = Event(
            RuntimeException(),
            immutableConfig,
            SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
            NoopLogger
        ).apply {
            setApp(BugsnagTestUtils.generateAppWithState())
            setDevice(BugsnagTestUtils.generateDeviceWithState())
        }
        val payload = EventPayload(immutableConfig.apiKey, event, null, Notifier(), immutableConfig)

        val json = JsonHelper.deserialize(payload.toByteArray())
        assertFalse(json.containsKey("usage"))
        assertTrue(json.containsKey("events"))
    }
}
