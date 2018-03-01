Feature: Android support

Scenario: Automatic Filter Tracking
    When I run "AutoFilterScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the exception "message" equals "AutoFilterScenario"
    And the event "metaData.custom.foo" equals "hunter2"
    And the event "metaData.custom.password" equals "[FILTERED]"
    And the event "metaData.user.password" equals "[FILTERED]"
