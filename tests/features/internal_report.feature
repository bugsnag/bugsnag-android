Feature: Sending internal error reports

Scenario: Sending internal error reports
    When I run "InternalReportScenario" and relaunch the app
    And I configure Bugsnag for "EmptyReportScenario"
    Then I should receive no requests
