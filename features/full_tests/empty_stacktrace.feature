Feature: Empty Stacktrace reported

  Background:
    Given I clear all persistent data

  Scenario: Exceptions with empty stacktraces are sent
    When I run "EmptyStacktraceScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "notifier.name" equals "Android Bugsnag Notifier"
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "com.bugsnag.android.mazerunner.scenarios.EmptyStacktraceScenario$EmptyException"
    And the error payload field "events.0.exceptions.0.stacktrace" is an array with 0 elements
