Feature: Verify the Bugsnag Initialization methods

  Background:
    Given I clear all persistent data

  Scenario: Test Bugsnag initializes correctly
    When I run "BugsnagInitScenario"
    And I wait to receive an error
    Then the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "metaData.client.count" equals 1
