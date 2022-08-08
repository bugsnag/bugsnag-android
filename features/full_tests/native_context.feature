Feature: Native Context API

  Background:
    Given I clear all persistent data

  Scenario: Changing intents followed by notifying in C
    When I run "CXXAutoContextScenario"
    Then I wait to receive 2 errors

    Then the error payload contains a completed handled native report
    And the event "severity" equals "info"
    And the event "context" equals "SecondActivity"
    And the exception "errorClass" equals "Hello hello"
    And the exception "message" equals "This is a new world"
    And the event "unhandled" is false
    And I discard the oldest error

    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "CXXAutoContextScenario"
    And the event "context" equals "SecondActivity"
