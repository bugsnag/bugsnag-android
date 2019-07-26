Feature: Reporting Error Context

Scenario: Context automatically set as most recent Activity name
    When I run "AutoContextScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the exception "message" equals "AutoContextScenario"
    And the event "context" equals "SecondActivity"

Scenario: Context manually set
    When I run "ManualContextScenario"
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the exception "message" equals "ManualContextScenario"
    And the event "context" equals "FooContext"
