Feature: Reporting large stacktrace

  Background:
    Given I clear all persistent data

  Scenario: A large stacktrace should have its frames trimmed to a reasonable number
    When I run "TrimmedStacktraceScenario"
    And I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events.0.exceptions.0.stacktrace" is an array with 200 elements
