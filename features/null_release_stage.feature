Feature: Android support

Scenario: Null release stage
    When I run "NullReleaseStageScenario" with the defaults
    Then I should receive no requests
