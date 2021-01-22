Feature: Native User API

    Scenario: Adding user information in C followed by notifying in C
        When I run "CXXUserInfoScenario"
        And I wait to receive an error
        Then the error payload contains a completed handled native report
        And the exception "errorClass" equals "Connection lost"
        And the exception "message" equals "No antenna detected"
        And the event "severity" equals "info"
        And the event "user.name" equals "Jack Mill"
        And the event "user.id" equals "324523"
        And the event "user.email" is null
        And the event "unhandled" is false
        And the event "app.binaryArch" is not null
        And the error payload field "events.0.device.cpuAbi" is a non-empty array

    Scenario: Adding user information in Java followed by a C crash
        And I run "CXXJavaUserInfoNativeCrashScenario" and relaunch the app
        And I configure the app to run in the "non-crashy" state
        And I configure Bugsnag for "CXXJavaUserInfoNativeCrashScenario"
        And I wait to receive an error
        Then the error payload contains a completed handled native report
        And the exception "errorClass" equals one of:
            | SIGILL |
            | SIGTRAP |
        And the event "severity" equals "error"
        And the event "user.name" equals "Strulyegha  Ghaumon  Rabelban  Snefkal  Angengtai  Samperris  D"
        And the event "user.id" equals "9816734"
        And the event "user.email" equals "j@example.com"
        And the event "unhandled" is true

    Scenario: Set user in Native layer followed by a Java crash
        When I run "CXXNativeUserInfoJavaCrashScenario" and relaunch the app
        And I configure Bugsnag for "CXXNativeUserInfoJavaCrashScenario"
        And I wait to receive an error
        Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
        And the error payload field "events" is an array with 1 elements
        And the event "user.id" equals "24601"
        And the event "user.email" equals "test@test.test"
        And the event "user.name" equals "test user"
