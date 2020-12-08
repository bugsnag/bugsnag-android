Feature: Reporting errors in multi process apps

Scenario: Test handled Kotlin Exception
    When I run "MultiProcessHandledExceptionScenario"
    Then I wait to receive 2 requests
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "MultiProcessHandledExceptionScenario"
    And the event "unhandled" is false
    And the payload field "events.0.metaData.process.name" equals "com.bugsnag.android.mazerunner"
    
    Then I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the exception "message" equals "MultiProcessHandledExceptionScenario"
    And the event "unhandled" is false
    And the payload field "events.0.metaData.process.name" equals "com.example.bugsnag.android.mazerunner.multiprocess"
