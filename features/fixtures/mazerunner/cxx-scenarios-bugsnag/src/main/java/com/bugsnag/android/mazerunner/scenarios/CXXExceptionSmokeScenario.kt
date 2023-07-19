package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnBreadcrumbCallback
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.Severity
import com.bugsnag.android.mazerunner.disableSessionDelivery
import java.util.regex.Pattern

class CXXExceptionSmokeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    external fun crash(): Int

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        Bugsnag.leaveBreadcrumb("CXXExceptionSmokeScenario")
        Bugsnag.setContext("Everest")
        val main = Handler(Looper.getMainLooper())
        main.postDelayed({ crash() }, 500)
    }

    init {
        System.loadLibrary("cxx-scenarios-bugsnag")
        config.appType = "Overwritten"
        config.appVersion = "9.9.9"
        config.versionCode = 999
        config.releaseStage = "CXXExceptionSmokeScenario"
        config.enabledReleaseStages = setOf("CXXExceptionSmokeScenario")
        config.context = "CXXExceptionSmokeScenario"
        config.setUser("ABC", "ABC@CBA.CA", "CXXExceptionSmokeScenario")
        config.addMetadata("TestData", "Source", "CXXExceptionSmokeScenario")
        config.redactedKeys = setOf(Pattern.compile(".*redacted.*"))
        config.addOnBreadcrumb(
            OnBreadcrumbCallback { breadcrumb ->
                val metadata = breadcrumb.metadata
                metadata!!["Source"] = "CXXExceptionSmokeScenario"
                breadcrumb.metadata = metadata
                true
            }
        )
        config.addOnError(
            OnErrorCallback { event ->
                event.addMetadata("TestData", "Callback", true)
                event.addMetadata("TestData", "redacted", false)
                event.severity = Severity.INFO
                true
            }
        )
        disableSessionDelivery(config)
    }
}
