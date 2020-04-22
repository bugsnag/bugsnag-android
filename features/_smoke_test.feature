Feature: Performs basic smoke tests to check that critical functionality is working. This allows
         for a faster feedback loop to check that a simple mistake hasn't entirely broken the
         notifier's error reporting, without having to wait for the full suite to run on CI.

Scenario: Test handled Kotlin Exception
    When I run "HandledExceptionScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "HandledExceptionScenario"
    And the payload field "events.0.device.cpuAbi" is a non-empty array for request 0

Scenario: Test Unhandled Java Exception with Session
    When I run "UnhandledExceptionJavaScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "UnhandledExceptionJavaScenario"

Scenario: Notifying in C
    When I run "CXXNotifyScenario"
    And I wait a bit
    Then I should receive a request
    And the request payload contains a completed handled native report
    And the event "severity" equals "error"
    And the event "context" equals "MainActivity"
    And the exception "errorClass" equals "Vitamin C deficiency"
    And the exception "message" equals "9 out of 10 adults do not get their 5-a-day"
    And the event "unhandled" is false

Scenario: Raise SIGSEGV
    When I run "CXXSigsegvScenario"
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive a request
    And the request payload contains a completed unhandled native report
    And the exception "errorClass" equals "SIGSEGV"
    And the exception "message" equals "Segmentation violation (invalid memory reference)"
    And the exception "type" equals "c"
    And the event "severity" equals "error"
    And the event "unhandled" is true

Scenario: Manual Session sends
    When I run "ManualSessionScenario"
    Then I should receive a request
    And the request is a valid for the session tracking API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "sessions" is an array with 1 element
    And the session "user.id" equals "123"
    And the session "user.email" equals "user@example.com"
    And the session "user.name" equals "Joe Bloggs"
    And the session "id" is not null
    And the session "startedAt" is not null

@anr
Scenario: Sleeping the main thread with pending touch events when autoDetectAnrs = true
    When I run "AppNotRespondingScenario"
    And I tap the screen
    And I tap the screen
    And I tap the screen
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "errorClass" equals "ANR"
    And the exception "message" starts with " Input dispatching timed out"
