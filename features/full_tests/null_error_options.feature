Feature: Error options notify scenarios

  Background:
    Given I clear all persistent data

  Scenario: Handled exceptions with null error options
    When I run "NullErrorOptionsScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "breadcrumbs" is not null
    And the event "featureFlags" is not null
    And the event "threads" is not null
    And the event "user" is not null
    And the event "metaData" is not null