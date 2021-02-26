Feature: Reporting large stacktrace

Scenario: A large stacktrace should have its frames trimmed to a reasonable number
    When I run "TrimmedStacktraceScenario"
    And I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.exceptions.0.stacktrace" is an array with 200 elements
