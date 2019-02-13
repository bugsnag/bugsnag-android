Feature: Stopping and resuming sessions

Scenario: When a session is stopped the error has no session information
    When I run "StoppedSessionScenario"
    Then I should receive 3 requests
    And the request 0 is valid for the session tracking API
    And the request 1 is valid for the error reporting API
    And the request 2 is valid for the error reporting API
    And the payload field "events.0.session" is not null for request 1
    And the payload field "events.0.session" is null for request 2

Scenario: When a session is resumed the error uses the previous session information
    When I run "ResumedSessionScenario"
    Then I should receive 3 requests
    And the request 0 is valid for the session tracking API
    And the request 1 is valid for the error reporting API
    And the request 2 is valid for the error reporting API
    And the payload field "events.0.session.events.handled" equals 1 for request 1
    And the payload field "events.0.session.events.handled" equals 2 for request 2
