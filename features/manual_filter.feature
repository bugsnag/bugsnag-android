Feature: Android support

Scenario: Manual Filter Tracking
    When I run "ManualFilterScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the exception "message" equals "ManualFilterScenario"
    And the event "metaData.custom.foo" equals "[FILTERED]"
    And the event "metaData.user.foo" equals "[FILTERED]"
    And the event "metaData.custom.bar" equals "hunter2"
