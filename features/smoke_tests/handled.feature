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

Scenario: Notifying in C
    When I run "CXXNotifyScenario"
    And I wait to receive a request
    Then the request payload contains a completed handled native report
    And the event "severity" equals "error"
    And the exception "errorClass" equals "Vitamin C deficiency"
    And the exception "message" equals "9 out of 10 adults do not get their 5-a-day"
    And the event "unhandled" is false