Feature: Reporting app version

Scenario: Test handled Android Exception
    When I run "BugsnagInitScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the event "metaData.client.count" equals 1
