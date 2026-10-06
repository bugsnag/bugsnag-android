package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

class OkHttpInstrumentationCallbackScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : OkHttpInstrumentationScenario(config, context, eventMetadata) {
    override val instrumentation
        get() = super.instrumentation
            .addRequestCallback { request ->
                request.reportedRequestBody = "testing request body"
                request.reportedUrl = "http://testingUrl.bugsnag.com"
            }
            .addResponseCallback { response ->
                response.reportedResponseBody = "testing response body"
            }
}
