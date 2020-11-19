Feature: Performs basic smoke tests to check that critical functionality is working. This allows
         for a faster feedback loop to check that a simple mistake hasn't entirely broken the
         notifier's error reporting, without having to wait for the full suite to run on CI.

Scenario: Test handled Kotlin Exception
    When I run "HandledExceptionScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledExceptionScenario"
    And the payload field "events.0.device.cpuAbi" is a non-empty array

Scenario: Test Unhandled Java Exception with Session
    When I run "UnhandledExceptionJavaScenario" and relaunch the app
    And I configure Bugsnag for "UnhandledExceptionJavaScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledExceptionJavaScenario"

Scenario: Notifying in C
    When I run "CXXNotifyScenario"
    And I wait to receive a request
    Then the request payload contains a completed handled native report
    And the event "severity" equals "error"
    And the exception "errorClass" equals "Vitamin C deficiency"
    And the exception "message" equals "9 out of 10 adults do not get their 5-a-day"
    And the event "unhandled" is false

Scenario: Raise SIGSEGV
    When I run "CXXSigsegvScenario" and relaunch the app
    And I configure Bugsnag for "CXXSigsegvScenario"
    And I wait to receive a request
    And the request payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGSEGV"
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true

Scenario: Manual Session sends
    When I run "ManualSessionScenario"
    And I wait to receive a request
    Then the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "sessions" is an array with 1 elements
    And the session "user.id" equals "123"
    And the session "user.email" equals "user@example.com"
    And the session "user.name" equals "Joe Bloggs"
    And the session "id" is not null
    And the session "startedAt" is not null

@skip_android_8_1
Scenario: Sleeping the main thread with pending touch events when autoDetectAnrs = true
    When I run "JvmAnrScenario"
    And I wait for 2 seconds
    And I tap the screen 3 times
    And I wait for 4 seconds
    And I clear any error dialogue
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
