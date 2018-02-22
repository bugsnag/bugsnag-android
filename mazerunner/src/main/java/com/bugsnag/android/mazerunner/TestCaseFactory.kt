package com.bugsnag.android.mazerunner

import android.content.Context
import com.bugsnag.android.mazerunner.scenarios.Scenario

internal class TestCaseFactory {

    fun testCaseForName(eventType: String?, context: Context?): Scenario {
        val clz = Class.forName("com.bugsnag.android.mazerunner.scenarios.$eventType")
        val scenario = clz.newInstance() as Scenario
        scenario.context = context
        return scenario
    }

}
