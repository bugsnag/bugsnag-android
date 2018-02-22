Feature: Android support

Scenario: User callback override
    When I run "UserCallbackScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "message" equals "UserCallbackScenario"
    And the event "user.id" equals "Agent Pink"
    And the event "user.email" equals "bob@example.com"
    And the event "user.name" equals "Zebedee"
