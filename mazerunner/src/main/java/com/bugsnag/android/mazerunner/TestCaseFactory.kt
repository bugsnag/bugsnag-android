package com.bugsnag.android.mazerunner

import com.bugsnag.android.mazerunner.scenarios.Scenario

internal class TestCaseFactory {

    fun testCaseForName(eventType: String?): Scenario {
        try {
            val clz = Class.forName("com.bugsnag.android.mazerunner.scenarios.$eventType")
            return clz.newInstance() as Scenario
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to find class for $eventType")
        }
    }

}
