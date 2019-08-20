Feature: Minimal error information is reported for corrupted/empty files

Scenario: Minimal error report for a Handled Exception with an empty file
    When I run "MinimalHandledExceptionScenario" and relaunch the app
    And I configure Bugsnag for "EmptyReportScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.exceptions.0.stacktrace" is an array with 0 elements
    And the exception "errorClass" equals "java.lang.RuntimeException"
    And the event "severity" equals "warning"
    And the event "unhandled" is false
    And the event "incomplete" is true
    And the event "breadcrumbs" is null
    And the event "session" is null
    And the event "context" is null
    And the event "severityReason.type" equals "handledException"

Scenario: Minimal error report for an Unhandled Exception with a corrupted file
    When I run "MinimalUnhandledExceptionScenario" and relaunch the app
    And I configure Bugsnag for "CorruptedReportScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.exceptions.0.stacktrace" is an array with 0 elements
    And the exception "errorClass" equals "java.lang.IllegalStateException"
    And the event "severity" equals "error"
    And the event "unhandled" is true
    And the event "incomplete" is true
    And the event "breadcrumbs" is null
    And the event "session" is null
    And the event "context" is null
    And the event "severityReason.type" equals "unhandledException"

Scenario: Minimal error report with old filename
    When I run "MinimalUnhandledExceptionScenario" and relaunch the app
    And I run "CorruptedOldReportScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "unhandled" is false
    And the event "incomplete" is false