Feature: Flushing errors does not send duplicates

Scenario: Only 1 request sent if connectivity change occurs before launch
    When I run "AsyncErrorConnectivityScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API

Scenario: Only 1 request sent if multiple connectivity changes occur
    When I run "AsyncErrorDoubleFlushScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API

Scenario: Only 1 request sent if connectivity change occurs after launch
    When I run "AsyncErrorLaunchScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
