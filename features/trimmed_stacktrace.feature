Feature: Reporting large stacktrace

Scenario: A large stacktrace should have its frames trimmed to a reasonable number
    When I run "TrimmedStacktraceScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the payload field "events.0.exceptions.0.stacktrace" is an array with 200 elements
