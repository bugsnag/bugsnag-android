Feature: Minimal error information is reported for corrupted/empty files

Scenario: Minimal error report for a Handled Exception with an empty file
    When I run "MinimalHandledExceptionScenario"
    And I set environment variable "EVENT_TYPE" to "EmptyReportScenario"
    And I relaunch the app
    Then I should receive 1 request
    And the request is valid for the error reporting API
    And the payload field "events.0.exceptions.0.stacktrace" is an array with 0 element
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "severity" equals "warning"
    And the event "unhandled" is false
    And the event "incomplete" is true
    And the event "severityReason.type" equals "handledException"

Scenario: Minimal error report for an Unhandled Exception with a corrupted file
    When I run "MinimalUnhandledExceptionScenario"
    And I set environment variable "EVENT_TYPE" to "CorruptedReportScenario"
    And I relaunch the app
    Then I should receive 1 request
    And the request is valid for the error reporting API
    And the payload field "events.0.exceptions.0.stacktrace" is an array with 0 element
    And the exception "errorClass" equals "java.lang.IllegalStateException"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the event "incomplete" is true
    And the event "severityReason.type" equals "unhandledException"

Scenario: Minimal error report with old filename
    When I run "MinimalUnhandledExceptionScenario"
    And I set environment variable "EVENT_TYPE" to "CorruptedOldReportScenario"
    And I relaunch the app
    Then I should receive 1 request
    And the request is valid for the error reporting API
    And the event "unhandled" is false
    And the event "incomplete" is false
