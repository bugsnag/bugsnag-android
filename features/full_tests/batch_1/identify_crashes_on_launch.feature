Feature: Identifying crashes on launch

    Scenario: Escaping from a crash loop by reading LastRunInfo in a JVM error
        When I run "JvmCrashLoopScenario" and relaunch the app
        When I run "JvmCrashLoopScenario"
        And I wait to receive 2 errors
        Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "First crash"
        And the event "metaData.LastRunInfo.crashed" is null
        And the event "metaData.LastRunInfo.crashedDuringLaunch" is null
        And the event "metaData.LastRunInfo.consecutiveLaunchCrashes" is null
        And I discard the oldest error
        Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "Safe mode enabled"
        And the event "metaData.LastRunInfo.crashed" is true
        And the event "metaData.LastRunInfo.crashedDuringLaunch" is true
        And the event "metaData.LastRunInfo.consecutiveLaunchCrashes" equals 1
