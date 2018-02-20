package com.bugsnag.android.mazerunner

import com.bugsnag.android.mazerunner.scenarios.HandledExceptionScenario
import com.bugsnag.android.mazerunner.scenarios.Scenario

internal class TestCaseFactory {

    fun testCaseForName(eventType: String?): Scenario {
        return when (eventType) {
            // TODO add test cases
            "HandledExceptionScenario" -> HandledExceptionScenario()
            else -> throw UnsupportedOperationException("Failed to find test case for eventType $eventType")
        }
    }

}
