package com.bugsnag.android

import org.junit.runner.Description
import org.junit.runner.notification.RunListener

class TestRunListener : RunListener() {

    override fun testFinished(description: Description) {
        super.testFinished(description)
        Async.cancelTasks()
    }
}
