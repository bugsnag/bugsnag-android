Feature: Sending internal error reports

Scenario: Send a report about an error triggered within the notifier
    When I run "InternalReportScenario"
    And I run "EmptyReportScenario"
    Then I should receive 0 requests
