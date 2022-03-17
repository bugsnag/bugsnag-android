Feature: Pausing and resuming sessions

  Background:
    Given I clear all persistent data

  Scenario: Stopping, resuming, and starting sessions are reflected in error and session payloads
    When I run "SessionStoppingScenario"

    # 2 sessions are received
    Then I wait to receive 2 sessions
    And the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session payload field "sessions.0.id" is stored as the value "first_session_id"
    And I discard the oldest session
    And the session is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the session payload field "sessions.0.id" is stored as the value "second_session_id"

    # 4 errors are received
    And I wait to receive 4 errors

    # First error with session
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "First error with session"
    And the error payload field "events.0.session.events.handled" equals 1
    And the error payload field "events.0.session.events.unhandled" equals 0
    And the error payload field "events.0.session.id" equals the stored value "first_session_id"
    And I discard the oldest error

    # Second error with paused session
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Second error with paused session"
    And the error payload field "events.0.session" is null
    And I discard the oldest error

    # Third error with resumed session
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Third error with resumed session"
    And the error payload field "events.0.session.events.handled" equals 2
    And the error payload field "events.0.session.events.unhandled" equals 0
    And the error payload field "events.0.session.id" equals the stored value "first_session_id"
    And I discard the oldest error

    # Fourth error with new session
    And the error is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the exception "message" equals "Fourth error with new session"
    And the error payload field "events.0.session.events.handled" equals 1
    And the error payload field "events.0.session.events.unhandled" equals 0
    And the error payload field "events.0.session.id" equals the stored value "second_session_id"
    And the error payload field "events.0.session.id" does not equal the stored value "first_session_id"
