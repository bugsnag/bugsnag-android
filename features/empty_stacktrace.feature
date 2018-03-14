Feature: Empty Stacktrace reported

Scenario: Exceptions with empty stacktraces are sent
    When I run "EmptyStacktraceScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 element
    And the exception "errorClass" equals "com.bugsnag.android.mazerunner.scenarios.EmptyStacktraceScenario$EmptyException"
    And the payload field "events.0.exceptions.0.stacktrace" is an array with 0 element
