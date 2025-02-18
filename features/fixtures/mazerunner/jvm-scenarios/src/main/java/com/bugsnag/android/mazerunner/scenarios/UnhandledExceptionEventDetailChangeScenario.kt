package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.Severity

/**
 * Sends an unhandled exception to Bugsnag where the event details are changed in a callback
 */
internal class UnhandledExceptionEventDetailChangeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.addOnError(
            OnErrorCallback { event ->
                event.apiKey = "0000111122223333aaaabbbbcccc9999"
                event.severity = Severity.ERROR
                event.groupingHash = "groupingHash1"
                event.context = "new-context"
                event.setUser("abc", "joe@test.com", "Joe")

                event.clearMetadata("custom_data1")
                event.clearMetadata("custom_data2", "data")
                event.addMetadata("custom_data2", "test_data", "this is test")

                event.clearFeatureFlag("test1")
                event.addFeatureFlag("beta", "b")
                event.addFeatureFlag("gamma")

                event.isUnhandled = false
                event.app.binaryArch = "x86"
                event.app.id = "12345"
                event.app.releaseStage = "custom"
                event.app.version = "1.2.3"
                event.app.buildUuid = "12345678"
                event.app.type = "android_custom"
                event.app.versionCode = 123
                event.app.duration = 123456
                event.app.durationInForeground = 123456
                event.app.inForeground = false
                event.app.isLaunching = false

                event.device.id = "12345"
                event.device.jailbroken = true
                event.device.locale = "en-UK"
                event.device.totalMemory = 123456
                event.device.runtimeVersions = mutableMapOf("androidApiLevel" to "30")
                event.device.freeDisk = 123456
                event.device.freeMemory = 123456
                event.device.orientation = "portrait"

                event.breadcrumbs.removeAt(event.breadcrumbs.lastIndex)
                event.breadcrumbs.first().type = BreadcrumbType.ERROR
                event.breadcrumbs.first().message = "new breadcrumb message"
                event.breadcrumbs[1].type = BreadcrumbType.ERROR
                event.breadcrumbs[1].message = "Second breadcrumb message"
                event.breadcrumbs.first().metadata = mapOf("foo" to "data")
                true
            }
        )
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.leaveBreadcrumb("Hello1")
        Bugsnag.leaveBreadcrumb("Hello2")
        Bugsnag.leaveBreadcrumb("Hello3")

        Bugsnag.addMetadata("custom_data1", "data", "hello")
        Bugsnag.addMetadata("custom_data2", "data", "hello")
        Bugsnag.addMetadata(
            "custom_data3",
            "test data",
            "divert all available power to the crash reporter"
        )
        Bugsnag.addFeatureFlag("test1")
        Bugsnag.addFeatureFlag("test2")

        if (eventMetadata == "notify") {
            Bugsnag.notify(RuntimeException("UnhandledExceptionEventDetailChangeScenario"))
        } else {
            throw NullPointerException("something broke")
        }
    }
}
