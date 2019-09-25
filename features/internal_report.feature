Feature: Sending internal error reports

Scenario: Send a report about an error triggered within the notifier
    When I run "InternalReportScenario"
    And I set environment variable "EVENT_TYPE" to "EmptyReportScenario"
    And I relaunch the app
    Then I should receive 0 requests
