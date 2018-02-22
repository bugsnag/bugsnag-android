Feature: Android support

Scenario: User email
    When I run "UserEmailScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "message" equals "UserEmailScenario"
    And the event "user.id" is null
    And the event "user.email" equals "user@example.com"
    And the event "user.name" is null
