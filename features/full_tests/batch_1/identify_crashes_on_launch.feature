Feature: Identifying crashes on launch

    Scenario: A JVM error captured after the launch period is false for app.isLaunching
        When I run "JvmDelayedErrorScenario"
        Then I wait to receive 2 errors
        And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "first error"
        And the event "app.isLaunching" is true
        Then I discard the oldest error
        And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "JvmDelayedErrorScenario"
        And the event "app.isLaunching" is false

    Scenario: An NDK error captured after the launch period is false for app.isLaunching
        When I run "CXXDelayedErrorScenario" and relaunch the app
        And I configure Bugsnag for "CXXDelayedErrorScenario"
        Then I wait to receive 2 errors
        And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "first error"
        And the event "app.isLaunching" is true
        Then I discard the oldest error
        And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "Abort program"
        And the event "app.isLaunching" is false

    Scenario: A JVM error captured after markLaunchComplete() is false for app.isLaunching
        When I run "JvmMarkLaunchCompletedScenario"
        Then I wait to receive 2 errors
        And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "first error"
        And the event "app.isLaunching" is true
        Then I discard the oldest error
        And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "JvmMarkLaunchCompletedScenario"
        And the event "app.isLaunching" is false

    Scenario: An NDK error captured after markLaunchComplete() is false for app.isLaunching
        When I run "CXXMarkLaunchCompletedScenario" and relaunch the app
        And I configure Bugsnag for "CXXMarkLaunchCompletedScenario"
        Then I wait to receive 2 errors
        And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "first error"
        And the event "app.isLaunching" is true
        Then I discard the oldest error
        And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the exception "message" equals "Abort program"
        And the event "app.isLaunching" is false

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
