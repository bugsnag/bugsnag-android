package com.bugsnag.android.mazerunner

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.scenarios.Scenario

internal class TestCaseFactory {

    fun testCaseForName(eventType: String?, config: Configuration, context: Context): Scenario {
        val clz = Class.forName("com.bugsnag.android.mazerunner.scenarios.$eventType")
        val constructor = clz.constructors[0]
        return constructor.newInstance(config, context) as Scenario
    }

}
