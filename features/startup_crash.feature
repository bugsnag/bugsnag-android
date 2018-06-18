Feature: All errors are flushed if a startup crash is persisted

Scenario: 1 startup crash and 1 regular crash persisted
    When I configure the app to run in the "CrashOfflineWithDelay" state
    And I run "StartupCrashFlushScenario" with the defaults
    And I wait for 10 seconds

    And I configure the app to run in the "CrashOfflineAtStartup" state
    And I relaunch the app

    When I force stop the "com.bugsnag.android.mazerunner" Android app
    And I set environment variable "EVENT_TYPE" to "Wait"
    And I start the "com.bugsnag.android.mazerunner" Android app using the "com.bugsnag.android.mazerunner.MainActivity" activity
    Then I should receive 2 requests
