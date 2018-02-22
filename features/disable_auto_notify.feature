Feature: Android support

Scenario: Disabled Exception Handler
    When I run "DisableAutoNotifyScenario" with the defaults
    Then I should receive no requests
