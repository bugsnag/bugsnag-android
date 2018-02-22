Feature: Android support

Scenario: Ignored Exception thrown
    When I run "IgnoredExceptionScenario" with the defaults
    Then I should receive no requests
