Feature: Reporting handled Exceptions

  Background:
    Given I clear all persistent data

  Scenario: Report a handled exception without a message
    When I run "HandledExceptionWithoutMessageScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the error payload field "events" is an array with 1 elements
    And the exception "errorClass" equals "com.bugsnag.android.mazerunner.SomeException"
    And the event "exceptions.0.message" is null
    And the error payload field "events.0.device.cpuAbi" is a non-empty array
