package com.bugsnag.android.mazerunner

import com.bugsnag.android.mazerunner.scenarios.Scenario

internal class TestCaseFactory {

    fun testCaseForName(eventType: String?): Scenario {
        val clz = Class.forName("com.bugsnag.android.mazerunner.scenarios.$eventType")
        return clz.newInstance() as Scenario
    }

}
