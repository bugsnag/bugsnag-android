Feature: Identifying crashes on launch

  Background:
    Given I clear all persistent data

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
    When I run "CXXDelayedErrorScenario" and relaunch the crashed app
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
    When I run "CXXMarkLaunchCompletedScenario" and relaunch the crashed app
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
    When I run "JvmCrashLoopScenario" and relaunch the crashed app
    When I run "JvmCrashLoopScenario" and relaunch the crashed app
    When I run "JvmCrashLoopScenario"
    And I wait to receive 3 errors

        # JVM crash
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "First JVM crash"
    And the event "metaData.LastRunInfo.crashed" is null
    And the event "metaData.LastRunInfo.crashedDuringLaunch" is null
    And the event "metaData.LastRunInfo.consecutiveLaunchCrashes" is null
    And I discard the oldest error

        # JVM crash
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Second JVM crash"
    And the event "metaData.LastRunInfo.crashed" is true
    And the event "metaData.LastRunInfo.crashedDuringLaunch" is true
    And the event "metaData.LastRunInfo.consecutiveLaunchCrashes" equals 1
    And I discard the oldest error

        # Safe mode
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Safe mode enabled"
    And the event "metaData.LastRunInfo.crashed" is true
    And the event "metaData.LastRunInfo.crashedDuringLaunch" is true
    And the event "metaData.LastRunInfo.consecutiveLaunchCrashes" equals 2

  Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
    When I run "CXXCrashLoopScenario" and relaunch the crashed app
    When I run "CXXCrashLoopScenario" and relaunch the crashed app
    When I run "CXXCrashLoopScenario"
    And I wait to receive 3 errors

        # NDK crash
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Abort program"
    And the event "metaData.LastRunInfo.crashed" is null
    And the event "metaData.LastRunInfo.crashedDuringLaunch" is null
    And the event "metaData.LastRunInfo.consecutiveLaunchCrashes" is null
    And I discard the oldest error

        # NDK crash
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Abort program"
    And the event "metaData.LastRunInfo.crashed" is true
    And the event "metaData.LastRunInfo.crashedDuringLaunch" is true
    And the event "metaData.LastRunInfo.consecutiveLaunchCrashes" equals 1
    And I discard the oldest error

        # Safe mode
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Safe mode enabled"
    And the event "metaData.LastRunInfo.crashed" is true
    And the event "metaData.LastRunInfo.crashedDuringLaunch" is true
    And the event "metaData.LastRunInfo.consecutiveLaunchCrashes" equals 2
