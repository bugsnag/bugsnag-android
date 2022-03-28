Feature: Reporting metadata

  Background:
    Given I clear all persistent data

  Scenario: Sends a handled exception which includes custom metadata added in a notify callback
    When I run "MetadataScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "metaData.Custom.foo" equals "Hello World!"

  Scenario: Add nested null value to metadata tab
    When I run "MetadataNestedNullScenario"
    Then I wait to receive an error
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
