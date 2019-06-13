Feature: Reports are ignored

Scenario: Exception classname ignored
    When I run "IgnoredExceptionScenario"
    And I wait for 2 seconds
    Then I should receive no requests


Scenario: Disabled Exception Handler
    When I run "DisableAutoNotifyScenario"
    And I wait for 2 seconds
    Then I should receive no requests

