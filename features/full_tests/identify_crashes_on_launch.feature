Feature: Identifying crashes on launch

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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

    Scenario: Escaping from a crash loop by reading LastRunInfo in an NDK error
        When I run "CXXCrashLoopScenario" and relaunch the app
        When I run "CXXCrashLoopScenario" and relaunch the app
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
