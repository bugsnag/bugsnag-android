Feature: NDK Session Tracking

Scenario: Paused session is not in payload of unhandled NDK error
    When I run "CXXPausedSessionScenario"
    And I wait a bit
    And I wait a bit
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 2 requests
    And the request 0 is a valid for the session tracking API
    And the request 1 is a valid for the error reporting API
    And the payload field "events.0.session" is null for request 1

Scenario: Started session is in payload of unhandled NDK error
    When I run "CXXStartSessionScenario"
    And I wait a bit
    And I wait a bit
    And I configure the app to run in the "non-crashy" state
    And I relaunch the app
    Then I should receive 2 requests
    And the request 0 is a valid for the session tracking API
    And the request 1 is a valid for the error reporting API
    And the payload field "events.0.session.events.unhandled" equals 1 for request 1
