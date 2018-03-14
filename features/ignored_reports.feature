Feature: Reports are ignored

Scenario: Exception classname ignored
    When I run "IgnoredExceptionScenario" with the defaults
    Then I should receive no requests

Scenario: Disabled Exception Handler
    When I run "DisableAutoNotifyScenario" with the defaults
    Then I should receive no requests
