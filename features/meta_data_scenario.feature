Feature: Android support

Scenario: Test handled Android Exception
    When I run "MetaDataScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the event "metaData.Custom.foo" equals "Hello World!"
