Feature: Android support

Scenario: User disabled
    When I run "UserDisabledScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the exception "message" equals "UserDisabledScenario"
    And the event "user.id" is null
    And the event "user.email" is null
    And the event "user.name" is null
