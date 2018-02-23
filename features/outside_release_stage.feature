Feature: Android support

Scenario: Outside release stage
    When I run "OutsideReleaseStageScenario" with the defaults
    Then I should receive no requests
