Feature: Pausing and resuming sessions

Scenario: When a session is paused the error has no session information
    When I run "PausedSessionScenario"
    Then I wait to receive 3 requests
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session" is not null
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session" is null

Scenario: When a session is resumed the error uses the previous session information
    When I run "ResumedSessionScenario"
    Then I wait to receive 3 requests
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "sessions.0.id" is stored as the value "resumed_session_id"
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session.events.handled" equals 1
    And the payload field "events.0.session.id" equals the stored value "resumed_session_id"
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session.events.handled" equals 2
    And the payload field "events.0.session.id" equals the stored value "resumed_session_id"

Scenario: When a new session is started the error uses different session information
    When I run "NewSessionScenario"
    Then I wait to receive 4 requests
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "sessions.0.id" is stored as the value "first_new_session_id"
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session.id" equals the stored value "first_new_session_id"
    And I discard the oldest request
    And the request is valid for the session reporting API version "1.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "sessions.0.id" is stored as the value "second_new_session_id"
    And I discard the oldest request
    And the request is valid for the error reporting API version "4.0" for the "Android Bugsnag Notifier" notifier
    And the payload field "events.0.session.id" equals the stored value "second_new_session_id"
    And the payload field "events.0.session.id" does not equal the stored value "first_new_session_id"
