Feature: Android support

Scenario: Automatic Context Tracking
    When I run "AutoContextScenario" with the defaults
    And I wait for 3 seconds
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the exception "message" equals "AutoContextScenario"
    And the event "context" equals "SecondActivity"
