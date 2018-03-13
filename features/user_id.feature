Feature: Android support

Scenario: User ID
    When I run "UserIdScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "message" equals "UserIdScenario"
    And the event "user.id" equals "abc"
    And the event "user.email" is null
    And the event "user.name" is null
