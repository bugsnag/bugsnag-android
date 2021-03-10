Feature: Empty Stacktrace reported

Scenario: Exceptions with empty stacktraces are sent
    When I run "EmptyStacktraceScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "com.bugsnag.android.mazerunner.scenarios.EmptyStacktraceScenario$EmptyException"
    And the payload field "events.0.exceptions.0.stacktrace" is an array with 0 elements
