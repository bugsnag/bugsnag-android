Feature: Reporting metadata

Scenario: Sends a handled exception which includes custom metadata added in a notify callback
    When I run "MetadataScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the event "metaData.Custom.foo" equals "Hello World!"

Scenario: Add nested null value to metadata tab
    When I run "MetadataNestedNullScenario"
    Then I wait to receive a request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
