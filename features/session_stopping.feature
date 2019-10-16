Feature: Pausing and resuming sessions

Scenario: When a session is paused the error has no session information
    When I run "PausedSessionScenario"
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
    And the payload field "events.0.session.id" of request 1 equals the payload field "events.0.session.id" of request 2
    And the payload field "events.0.session.startedAt" of request 1 equals the payload field "events.0.session.startedAt" of request 2

Scenario: When a new session is started the error uses different session information
    When I run "NewSessionScenario"
    Then I should receive 4 requests
    And the request 0 is valid for the session tracking API
    And the request 1 is valid for the error reporting API
    And the request 2 is valid for the session tracking API
    And the request 3 is valid for the error reporting API
    And the payload field "events.0.session.events.handled" equals 1 for request 1
    And the payload field "events.0.session.events.handled" equals 1 for request 3
    And the payload field "events.0.session.id" of request 1 does not equal the payload field "events.0.session.id" of request 3
