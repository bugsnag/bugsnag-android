Feature: Stopping and resuming sessions

Scenario: When a session is stopped the error has no session information
    When I run "StoppedSessionScenario"
    Then I wait to receive 3 requests
#    And the request is valid for the session tracking API
    And I discard the oldest request
#    And the request 1 is valid for the error reporting API
    And the payload field "events.0.session" is not null
    And I discard the oldest request
#    And the request 2 is valid for the error reporting API
    And the payload field "events.0.session" is null

Scenario: When a session is resumed the error uses the previous session information
    When I run "ResumedSessionScenario"
    Then I wait to receive 3 requests
#    And the request is valid for the session tracking API
    And the payload field "events.0.session.events.handled" equals 1
    And I discard the oldest request
#    And the request is valid for the error reporting API
    And the payload field "events.0.session.events.handled" equals 2
    And I discard the oldest request
#    And the request is valid for the error reporting API
# Not currently implemented |    And the payload field "events.0.session.id" of request 1 equals the payload field "events.0.session.id" of request 2
# Not currently implemented |   And the payload field "events.0.session.startedAt" of request 1 equals the payload field "events.0.session.startedAt" of request 2

Scenario: When a new session is started the error uses different session information
    When I run "NewSessionScenario"
    Then I wait to receive 4 requests
    And the request 0 is valid for the session tracking API
    And the request 1 is valid for the error reporting API
    And the request 2 is valid for the session tracking API
    And the request 3 is valid for the error reporting API
    And the payload field "events.0.session.events.handled" equals 1 for request 1
    And the payload field "events.0.session.events.handled" equals 1 for request 3
    And the payload field "events.0.session.id" of request 1 does not equal the payload field "events.0.session.id" of request 3
