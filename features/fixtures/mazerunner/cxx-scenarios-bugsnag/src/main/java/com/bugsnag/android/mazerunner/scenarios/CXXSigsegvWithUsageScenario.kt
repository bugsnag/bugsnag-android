package com.bugsnag.android.mazerunner.scenarios;

import com.bugsnag.android.Configuration;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class CXXSigsegvWithUsageScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        init {
            System.loadLibrary("cxx-scenarios-bugsnag")
        }
    }

    init {
        config.maxBreadcrumbs = 10
        config.autoTrackSessions = false
    }

    external fun crash(value: Int): Int

    override fun startScenario() {
        super.startScenario()
        crash(1)
    }
}
