Feature: Reporting User Information

Scenario: Override user details in callback
    When I run "UserCallbackScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "message" equals "UserCallbackScenario"
    And the event "user.id" equals "Agent Pink"
    And the event "user.email" equals "bob@example.com"
    And the event "user.name" equals "Zebedee"

Scenario: User fields set as null
    When I run "UserDisabledScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the exception "message" equals "UserDisabledScenario"
    And the event "user.id" is null
    And the event "user.email" is null
    And the event "user.name" is null


Scenario: Only User email field set
    When I run "UserEmailScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "message" equals "UserEmailScenario"
    And the event "user.id" is null
    And the event "user.email" equals "user@example.com"
    And the event "user.name" is null

Scenario: All user fields set
    When I run "UserEnabledScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the "Bugsnag-API-Key" header equals "a35a2a72bd230ac0aa0f52715bbdc6aa"
    And the exception "message" equals "UserEnabledScenario"
    And the event "user.id" equals "123"
    And the event "user.email" equals "user@example.com"
    And the event "user.name" equals "Joe Bloggs"

Scenario: Only User ID field set
    When I run "UserIdScenario" with the defaults
    Then I should receive a request
    And the request is a valid for the error reporting API
    And the exception "message" equals "UserIdScenario"
    And the event "user.id" equals "abc"
    And the event "user.email" is null
    And the event "user.name" is null
